package com.icbc.lingmou.controller;

import com.icbc.lingmou.common.BusinessException;
import com.icbc.lingmou.common.Result;
import com.icbc.lingmou.common.ResultCode;
import com.icbc.lingmou.entity.Appointment;
import com.icbc.lingmou.entity.Voucher;
import com.icbc.lingmou.mapper.AppointmentMapper;
import com.icbc.lingmou.service.VoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * bindAppointment 绑定守卫单元测试（纯 Mockito，不依赖 MySQL/Redis）
 * 覆盖：未登录 / 预约不存在 / 他人预约 / 重绑拒绝(20009) / 幂等放行 / 正常绑定 / V号历史补落
 */
@ExtendWith(MockitoExtension.class)
class QRCodeControllerBindTest {

    private static final Long USER_ID = 1L;
    private static final String DIRECT_CODE = "TBK00202609130001ABCD";
    private static final String APPT_VOUCHER = "V20260910093001234";

    @Mock
    private VoucherService voucherService;

    @Mock
    private AppointmentMapper appointmentMapper;

    private QRCodeController controller;

    @BeforeEach
    void setUp() {
        // bindAppointment 路径只用到 voucherService 与 appointmentMapper，其余依赖传 null
        controller = new QRCodeController(
                null, voucherService, null, null, appointmentMapper, null, null, null, null);
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", USER_ID);
        return request;
    }

    private Appointment appointmentOf(Long id, Long userId) {
        Appointment appt = new Appointment();
        appt.setId(id);
        appt.setUserId(userId);
        return appt;
    }

    @Test
    void withoutLogin_isRejected() {
        assertThatThrownBy(() -> controller.bindAppointment(
                DIRECT_CODE, 9L, new MockHttpServletRequest()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void missingAppointment_isRejected() {
        when(voucherService.isValidVoucherNum(DIRECT_CODE)).thenReturn(true);
        when(appointmentMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> controller.bindAppointment(DIRECT_CODE, 99L, request()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ResultCode.APPOINTMENT_NOT_FOUND.getCode()));
    }

    @Test
    void otherUsersAppointment_isForbidden() {
        when(voucherService.isValidVoucherNum(DIRECT_CODE)).thenReturn(true);
        when(appointmentMapper.selectById(9L)).thenReturn(appointmentOf(9L, 2L));

        assertThatThrownBy(() -> controller.bindAppointment(DIRECT_CODE, 9L, request()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ResultCode.FORBIDDEN.getCode()));
        verify(voucherService, never()).findByVoucherNum(any());
    }

    @Test
    void voucherBoundToOtherAppointment_isRejectedWith20009() {
        when(voucherService.isValidVoucherNum(DIRECT_CODE)).thenReturn(true);
        when(appointmentMapper.selectById(9L)).thenReturn(appointmentOf(9L, USER_ID));
        Voucher bound = new Voucher();
        bound.setVoucherNum(DIRECT_CODE);
        bound.setAppointmentId(5L);
        when(voucherService.findByVoucherNum(DIRECT_CODE)).thenReturn(bound);

        assertThatThrownBy(() -> controller.bindAppointment(DIRECT_CODE, 9L, request()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getCode()).isEqualTo(ResultCode.VOUCHER_ALREADY_BOUND.getCode()));
        verify(voucherService, never()).save(any(Voucher.class));
    }

    @Test
    void sameAppointment_rebind_isIdempotentSuccess() {
        when(voucherService.isValidVoucherNum(DIRECT_CODE)).thenReturn(true);
        when(appointmentMapper.selectById(9L)).thenReturn(appointmentOf(9L, USER_ID));
        Voucher bound = new Voucher();
        bound.setVoucherNum(DIRECT_CODE);
        bound.setAppointmentId(9L);
        when(voucherService.findByVoucherNum(DIRECT_CODE)).thenReturn(bound);

        Result<Map<String, Object>> result =
                controller.bindAppointment(DIRECT_CODE, 9L, request());

        assertThat(result.getCode()).isEqualTo(ResultCode.SUCCESS.getCode());
        assertThat(result.getData().get("bound")).isEqualTo(true);
        verify(voucherService).save(bound);
    }

    @Test
    void freshDirectCode_bindsSuccessfully() {
        when(voucherService.isValidVoucherNum(DIRECT_CODE)).thenReturn(true);
        when(appointmentMapper.selectById(9L)).thenReturn(appointmentOf(9L, USER_ID));
        Voucher fresh = new Voucher();
        fresh.setVoucherNum(DIRECT_CODE);
        when(voucherService.findByVoucherNum(DIRECT_CODE)).thenReturn(fresh);

        Result<Map<String, Object>> result =
                controller.bindAppointment(DIRECT_CODE, 9L, request());

        assertThat(result.getData().get("bound")).isEqualTo(true);
        assertThat(fresh.getAppointmentId()).isEqualTo(9L);
        verify(voucherService).save(fresh);
    }

    @Test
    void historicalAppointmentVoucher_fallsBackToSaveRow() {
        when(voucherService.isValidVoucherNum(APPT_VOUCHER)).thenReturn(true);
        when(appointmentMapper.selectById(9L)).thenReturn(appointmentOf(9L, USER_ID));
        when(voucherService.findByVoucherNum(APPT_VOUCHER)).thenReturn(null);

        Result<Map<String, Object>> result =
                controller.bindAppointment(APPT_VOUCHER, 9L, request());

        assertThat(result.getData().get("bound")).isEqualTo(true);
        verify(voucherService).saveAppointmentVoucher(9L, APPT_VOUCHER);
    }
}
