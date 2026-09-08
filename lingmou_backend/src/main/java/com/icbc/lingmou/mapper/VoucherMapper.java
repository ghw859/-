package com.icbc.lingmou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.icbc.lingmou.entity.Voucher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 凭证 Mapper 接口
 */
@Mapper
public interface VoucherMapper extends BaseMapper<Voucher> {

    /**
     * 查询某网点+日期的最大序号
     * 凭证号结构：T + 网点4 + 日期8 + 序号4 + CRC4
     * 前17位 = 前缀 + 序号
     */
    @Select("SELECT MAX(CAST(SUBSTRING(voucher_num, 14, 4) AS UNSIGNED)) " +
            "FROM vouchers " +
            "WHERE voucher_num LIKE CONCAT(#{prefix}, '%') AND deleted = 0")
    Integer findMaxSeqByPrefix(@Param("prefix") String prefix);
}
