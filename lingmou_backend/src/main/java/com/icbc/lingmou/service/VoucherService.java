package com.icbc.lingmou.service;

import com.icbc.lingmou.entity.Voucher;

/**
 * 凭证 Service 接口
 *
 * 系统内存在两类凭证号（Day9 起双格式互认）：
 * 1. 业务直通码 T：T + 网点编码(4) + YYYYMMDD(8) + 当日序号(4) + CRC校验(4)，共 21 位
 *    示例：TBK00202609130001ABCD，由 POST /api/qrcode/generate 生成，落 vouchers 表
 * 2. 预约凭证号 V：V + 13位毫秒时间戳 + 4位随机数（共 18 位；Day3 历史数据为 14 位）
 *    示例：V17212345678901234，由预约流程生成，存 appointments.voucher_num，
 *    预约创建时同步在 vouchers 表落关联行（appointment_id 指向预约）。
 */
public interface VoucherService {

    /**
     * 生成业务直通码凭证号（自动带网点编码、日期、序号、CRC校验）
     *
     * @param branchCode 网点编码
     * @return 完整凭证号（T 开头，21 位）
     */
    String generateVoucherNum(String branchCode);

    /**
     * 根据凭证号查询 vouchers 表记录（T/V 同表）
     */
    Voucher findByVoucherNum(String voucherNum);

    /**
     * 校验凭证号是否合法（T 走 CRC16 校验；V 走格式校验）
     */
    boolean isValidVoucherNum(String voucherNum);

    /**
     * 保存凭证记录
     */
    Voucher save(Voucher voucher);

    /**
     * 预约创建后，把预约凭证号 V 同步落一行 vouchers 记录并关联 appointment_id。
     * 若该凭证号已有记录则不重复插入（幂等）。
     */
    void saveAppointmentVoucher(Long appointmentId, String voucherNum);

    /**
     * 按预约凭证号 V 回查预约，组装为临时 Voucher 对象（不落库），
     * 用于兼容 vouchers 表缺失关联行的历史预约数据。
     *
     * @return 预约存在时返回带 appointmentId/qrcodeContent 的临时凭证；不存在返回 null
     */
    Voucher resolveAppointmentVoucher(String voucherNum);
}
