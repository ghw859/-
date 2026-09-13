package com.icbc.lingmou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.lingmou.common.BusinessException;
import com.icbc.lingmou.common.ResultCode;
import com.icbc.lingmou.entity.Appointment;
import com.icbc.lingmou.entity.Voucher;
import com.icbc.lingmou.mapper.AppointmentMapper;
import com.icbc.lingmou.mapper.VoucherMapper;
import com.icbc.lingmou.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 凭证 Service 实现
 *
 * 支持两类凭证号（Day9 起双格式互认）：
 *
 * 业务直通码 T：
 *   格式：T + 网点编码(4) + YYYYMMDD(8) + 当日序号(4) + CRC16校验(4)
 *   总长：1 + 4 + 8 + 4 + 4 = 21 位
 *   示例：TBK00202609130001ABCD
 *   CRC16 使用 Modbus 标准算法（多项式 0xA001）
 *
 * 预约凭证号 V：
 *   格式：V + 13位毫秒时间戳 + 4位随机数 = 18 位
 *   Day3 契约示例 V1721234567890（14 位）同样视为合法
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherMapper voucherMapper;
    private final AppointmentMapper appointmentMapper;

    private static final String PREFIX_DIRECT = "T";
    private static final String PREFIX_APPOINTMENT = "V";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public String generateVoucherNum(String branchCode) {
        if (branchCode == null || branchCode.length() < 2) {
            throw new BusinessException(ResultCode.VOUCHER_NUM_GENERATE_FAIL, "网点编码无效");
        }

        String datePart = LocalDate.now().format(DATE_FMT);

        // 查询该网点今天已使用的最大序号
        String prefix = PREFIX_DIRECT + padBranchCode(branchCode) + datePart;
        Integer seq = voucherMapper.findMaxSeqByPrefix(prefix);
        int nextSeq = (seq == null ? 0 : seq) + 1;

        if (nextSeq > 9999) {
            throw new BusinessException(ResultCode.VOUCHER_NUM_GENERATE_FAIL, "当日凭证号已用完");
        }

        String raw = prefix + String.format("%04d", nextSeq);
        String crc = crc16Hex(raw);

        return raw + crc;
    }

    @Override
    public Voucher findByVoucherNum(String voucherNum) {
        return voucherMapper.selectOne(
                new LambdaQueryWrapper<Voucher>()
                        .eq(Voucher::getVoucherNum, voucherNum)
        );
    }

    @Override
    public boolean isValidVoucherNum(String voucherNum) {
        return isValidDirectCode(voucherNum) || isValidAppointmentVoucherNum(voucherNum);
    }

    @Override
    public void saveAppointmentVoucher(Long appointmentId, String voucherNum) {
        if (appointmentId == null || voucherNum == null || voucherNum.isBlank()) {
            return;
        }
        // 幂等：同一凭证号不重复落行
        if (findByVoucherNum(voucherNum) != null) {
            return;
        }
        Voucher voucher = new Voucher();
        voucher.setVoucherNum(voucherNum);
        voucher.setAppointmentId(appointmentId);
        voucher.setQrcodeContent("LINGMOU://" + voucherNum);
        voucher.setSn("SN" + System.currentTimeMillis()
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000)));
        voucherMapper.insert(voucher);
        log.info("[凭证] 预约凭证落 vouchers 关联行: appointmentId={}, voucherNum={}", appointmentId, voucherNum);
    }

    @Override
    public Voucher resolveAppointmentVoucher(String voucherNum) {
        if (!isValidAppointmentVoucherNum(voucherNum)) {
            return null;
        }
        Appointment appointment = appointmentMapper.selectOne(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getVoucherNum, voucherNum)
                        .last("LIMIT 1")
        );
        if (appointment == null) {
            return null;
        }
        // 组装临时凭证（不落库），把预约桥接成凭证查询/二维码接口可消费的形态
        Voucher voucher = new Voucher();
        voucher.setVoucherNum(voucherNum);
        voucher.setAppointmentId(appointment.getId());
        voucher.setQrcodeContent("LINGMOU://" + voucherNum);
        voucher.setCreatedAt(appointment.getCreatedAt());
        return voucher;
    }

    /**
     * 业务直通码 T：21 位、T 开头、CRC16 自洽
     */
    private boolean isValidDirectCode(String voucherNum) {
        if (voucherNum == null || voucherNum.length() != 21) {
            return false;
        }
        if (!voucherNum.startsWith(PREFIX_DIRECT)) {
            return false;
        }
        String raw = voucherNum.substring(0, 17); // 前17位（T+网点4+日期8+序号4）
        String crc = voucherNum.substring(17).toUpperCase();
        String expectedCrc = crc16Hex(raw);
        return expectedCrc.equals(crc);
    }

    /**
     * 预约凭证号 V：V 开头 + 13~17 位纯数字
     * （现网生成 13 位毫秒 + 4 位随机 = 17 位；Day3 历史数据为 13 位）
     */
    private boolean isValidAppointmentVoucherNum(String voucherNum) {
        if (voucherNum == null || !voucherNum.startsWith(PREFIX_APPOINTMENT)) {
            return false;
        }
        String tail = voucherNum.substring(1);
        int len = tail.length();
        if (len < 13 || len > 17) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (!Character.isDigit(tail.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Voucher save(Voucher voucher) {
        if (voucher.getId() == null) {
            voucherMapper.insert(voucher);
        } else {
            voucherMapper.updateById(voucher);
        }
        return voucher;
    }

    /**
     * 将网点编码补齐/截断为4位（小写转正）
     */
    private String padBranchCode(String code) {
        String upper = code.toUpperCase();
        if (upper.length() >= 4) {
            return upper.substring(0, 4);
        }
        return String.format("%-4s", upper).replace(' ', '0');
    }

    /**
     * CRC16-Modbus 算法，返回4位十六进制大写字符串
     * 多项式：0xA001（反转的 0x8005）
     */
    public static String crc16Hex(String data) {
        byte[] bytes = data.getBytes();
        int crc = 0xFFFF;
        for (byte b : bytes) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc = crc >> 1;
                }
            }
        }
        return String.format("%04X", crc & 0xFFFF);
    }
}
