package com.icbc.lingmou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.lingmou.entity.Voucher;
import com.icbc.lingmou.mapper.AppointmentMapper;
import com.icbc.lingmou.mapper.VoucherMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 凭证号规则与落库幂等性单元测试（纯 Mockito，不依赖 MySQL/Redis）
 */
@ExtendWith(MockitoExtension.class)
class VoucherServiceImplTest {

    @Mock
    private VoucherMapper voucherMapper;

    @Mock
    private AppointmentMapper appointmentMapper;

    private VoucherServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VoucherServiceImpl(voucherMapper, appointmentMapper);
    }

    /** 用私有 crc16Hex 造一个 CRC 自洽的合法 T 直通码 */
    private String buildValidDirectCode() {
        String raw = "TBK00202609130001"; // T + 网点4 + 日期8 + 序号4 = 17位
        String crc = ReflectionTestUtils.invokeMethod(service, "crc16Hex", raw);
        return raw + crc;
    }

    @Test
    void directCode_withSelfConsistentCrc_isValid() {
        assertThat(service.isValidVoucherNum(buildValidDirectCode())).isTrue();
    }

    @Test
    void directCode_withTamperedCrc_isInvalid() {
        assertThat(service.isValidVoucherNum(buildValidDirectCode() + "X")).isFalse(); // 22位
        assertThat(service.isValidVoucherNum("TBK00202609130001ZZZZ")).isFalse(); // 21位但CRC不自洽（Z非hex）
    }

    @Test
    void directCode_withWrongLength_isInvalid() {
        assertThat(service.isValidVoucherNum("TBK0020260913")).isFalse();
    }

    @Test
    void appointmentVoucher_13to17Digits_isValid() {
        assertThat(service.isValidVoucherNum("V1721234567890")).isTrue(); // 13位（Day3历史格式）
        assertThat(service.isValidVoucherNum("V20260910093001234")).isTrue(); // 17位（现网格式）
    }

    @Test
    void appointmentVoucher_badFormat_isInvalid() {
        assertThat(service.isValidVoucherNum("V123")).isFalse(); // 太短
        assertThat(service.isValidVoucherNum("V202609100930012345")).isFalse(); // 18位超长
        assertThat(service.isValidVoucherNum("V2026AB100930012")).isFalse(); // 含非数字
        assertThat(service.isValidVoucherNum("X202609100930012")).isFalse(); // 前缀不对
        assertThat(service.isValidVoucherNum(null)).isFalse();
    }

    @Test
    void saveAppointmentVoucher_existingNum_isIdempotent() {
        when(voucherMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(new Voucher());

        service.saveAppointmentVoucher(9L, "V20260910093001234");

        verify(voucherMapper, never()).insert(any(Voucher.class));
    }

    @Test
    void saveAppointmentVoucher_newNum_insertsLinkedRow() {
        when(voucherMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service.saveAppointmentVoucher(9L, "V20260910093001234");

        ArgumentCaptor<Voucher> captor = ArgumentCaptor.forClass(Voucher.class);
        verify(voucherMapper).insert(captor.capture());
        Voucher saved = captor.getValue();
        assertThat(saved.getVoucherNum()).isEqualTo("V20260910093001234");
        assertThat(saved.getAppointmentId()).isEqualTo(9L);
        assertThat(saved.getQrcodeContent()).isEqualTo("LINGMOU://V20260910093001234");
        assertThat(saved.getSn()).isNotBlank();
    }
}
