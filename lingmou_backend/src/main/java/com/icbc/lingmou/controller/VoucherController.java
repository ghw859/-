package com.icbc.lingmou.controller;

import com.icbc.lingmou.common.BusinessException;
import com.icbc.lingmou.common.Result;
import com.icbc.lingmou.common.ResultCode;
import com.icbc.lingmou.entity.Voucher;
import com.icbc.lingmou.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 凭证号查询控制器
 *
 * 支持两类凭证号（Day9 起双格式互认）：
 * 1. 业务直通码 T：21 位（T+网点4+日期8+序号4+CRC4），vouchers 表实体记录
 * 2. 预约凭证号 V：18 位（V+13位毫秒+4位随机，Day3 历史为14位），
 *    vouchers 表无行时回查 appointments 表，保证 V 号不再 validFormat:false
 */
@Tag(name = "凭证查询", description = "凭证号查询与校验（直通码 T / 预约凭证 V）")
@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @Operation(summary = "查询凭证详情", description = "根据凭证号查询凭证记录；T 号做 CRC 校验，V 号做格式校验并回查预约")
    @GetMapping("/{voucherNum}")
    public Result<Map<String, Object>> getVoucherByNum(
            @Parameter(description = "凭证号：T 开头 21 位直通码，或 V 开头 14/18 位预约凭证")
            @PathVariable String voucherNum) {

        // 先做格式合法性校验（T 走 CRC16，V 走格式）
        boolean valid = voucherService.isValidVoucherNum(voucherNum);

        Map<String, Object> result = new HashMap<>();
        result.put("voucherNum", voucherNum);
        result.put("validFormat", valid);

        if (!valid) {
            result.put("exists", false);
            return Result.success("凭证号格式不合法", result);
        }

        Voucher voucher = resolveVoucher(voucherNum);
        if (voucher == null) {
            throw new BusinessException(ResultCode.VOUCHER_NUM_NOT_FOUND);
        }

        result.put("exists", true);
        // kind：DIRECT_CODE=业务直通码（T），APPOINTMENT=预约凭证（V）
        result.put("kind", voucherNum.startsWith("T") ? "DIRECT_CODE" : "APPOINTMENT");
        result.put("id", voucher.getId());
        result.put("appointmentId", voucher.getAppointmentId());
        result.put("sn", voucher.getSn());
        result.put("qrcodeContent", voucher.getQrcodeContent());
        result.put("createdAt", voucher.getCreatedAt());

        return Result.success(result);
    }

    @Operation(summary = "校验凭证号格式", description = "只做格式/CRC校验，不查数据库（T 与 V 均认可）")
    @GetMapping("/{voucherNum}/validate")
    public Result<Map<String, Object>> validateVoucher(
            @PathVariable String voucherNum) {
        boolean valid = voucherService.isValidVoucherNum(voucherNum);
        Map<String, Object> result = new HashMap<>();
        result.put("voucherNum", voucherNum);
        result.put("valid", valid);
        result.put("kind", voucherNum.startsWith("T") ? "DIRECT_CODE"
                : voucherNum.startsWith("V") ? "APPOINTMENT" : "UNKNOWN");
        return Result.success(result);
    }

    @Operation(summary = "凭证号规则说明", description = "返回凭证号生成规则")
    @GetMapping("/rule")
    public Result<Map<String, Object>> getVoucherRule() {
        Map<String, Object> rule = new HashMap<>();
        rule.put("directCode", Map.of(
                "prefix", "T",
                "format", "T + 网点编码(4位) + YYYYMMDD(8位) + 当日序号(4位) + CRC16校验(4位)",
                "totalLength", 21,
                "example", "TBK00202609130001ABCD",
                "crcAlgorithm", "CRC16-Modbus (0xA001)"
        ));
        rule.put("appointmentVoucher", Map.of(
                "prefix", "V",
                "format", "V + 13位毫秒时间戳 + 4位随机数",
                "totalLength", 18,
                "example", "V17212345678901234"
        ));
        rule.put("format", "T + 网点编码(4位) + YYYYMMDD(8位) + 当日序号(4位) + CRC16校验(4位)");
        rule.put("totalLength", 21);
        rule.put("example", "TBJ01202609080001ABCD");
        rule.put("crcAlgorithm", "CRC16-Modbus (0xA001)");
        rule.put("branchCode", "如 b1~b6=北京6网点，缺省 BK00");
        return Result.success(rule);
    }

    /**
     * 解析凭证：先查 vouchers 表；V 号查不到时回查 appointments 表（历史数据兼容）
     */
    private Voucher resolveVoucher(String voucherNum) {
        Voucher voucher = voucherService.findByVoucherNum(voucherNum);
        if (voucher == null && voucherNum.startsWith("V")) {
            voucher = voucherService.resolveAppointmentVoucher(voucherNum);
        }
        return voucher;
    }
}
