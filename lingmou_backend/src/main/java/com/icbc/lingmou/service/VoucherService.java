package com.icbc.lingmou.service;

import com.icbc.lingmou.entity.Voucher;

/**
 * 凭证 Service 接口
 * 凭证号规则：T + 网点编码(4) + YYYYMMDD(8) + 当日序号(4) + CRC校验(4)
 * 示例：TBJ01202609080001ABCD
 */
public interface VoucherService {

    /**
     * 生成凭证号（自动带网点编码、日期、序号、CRC校验）
     *
     * @param branchCode 网点编码
     * @return 完整凭证号
     */
    String generateVoucherNum(String branchCode);

    /**
     * 根据凭证号查询凭证记录
     */
    Voucher findByVoucherNum(String voucherNum);

    /**
     * 校验凭证号是否合法（CRC校验 + 格式校验）
     */
    boolean isValidVoucherNum(String voucherNum);

    /**
     * 保存凭证记录
     */
    Voucher save(Voucher voucher);
}
