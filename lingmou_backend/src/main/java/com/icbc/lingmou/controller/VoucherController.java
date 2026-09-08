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
 * 凭证号规则：T + 网点编码(4) + YYYYMMDD(8) + 序号(4) + CRC16校验(4) = 21位
 */
@Tag(name = "凭证查询", description = "凭证号查询与校验")
@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @Operation(summary = "查询凭证详情", description = "根据凭证号查询凭证记录，含CRC合法性校验")
    @GetMapping("/{voucherNum}")
    public Result<Map<String, Object>> getVoucherByNum(
            @Parameter(description = "凭证号，21位，格式：T+网点4+日期8+序号4+CRC4")
            @PathVariable String voucherNum) {

        // 先做格式+CRC合法性校验
        boolean valid = voucherService.isValidVoucherNum(voucherNum);

        Map<String, Object> result = new HashMap<>();
        result.put("voucherNum", voucherNum);
        result.put("validFormat", valid);

        if (!valid) {
            result.put("exists", false);
            return Result.success("凭证号格式不合法", result);
        }

        Voucher voucher = voucherService.findByVoucherNum(voucherNum);
        if (voucher == null) {
            throw new BusinessException(ResultCode.VOUCHER_NUM_NOT_FOUND);
        }

        result.put("exists", true);
        result.put("id", voucher.getId());
        result.put("appointmentId", voucher.getAppointmentId());
        result.put("sn", voucher.getSn());
        result.put("qrcodeContent", voucher.getQrcodeContent());
        result.put("createdAt", voucher.getCreatedAt());

        return Result.success(result);
    }

    @Operation(summary = "校验凭证号格式", description = "只做格式+CRC校验，不查数据库")
    @GetMapping("/{voucherNum}/validate")
    public Result<Map<String, Object>> validateVoucher(
            @PathVariable String voucherNum) {
        boolean valid = voucherService.isValidVoucherNum(voucherNum);
        Map<String, Object> result = new HashMap<>();
        result.put("voucherNum", voucherNum);
        result.put("valid", valid);
        return Result.success(result);
    }

    @Operation(summary = "凭证号规则说明", description = "返回凭证号生成规则")
    @GetMapping("/rule")
    public Result<Map<String, Object>> getVoucherRule() {
        Map<String, Object> rule = new HashMap<>();
        rule.put("format", "T + 网点编码(4位) + YYYYMMDD(8位) + 当日序号(4位) + CRC16校验(4位)");
        rule.put("totalLength", 21);
        rule.put("example", "TBJ01202609080001ABCD");
        rule.put("crcAlgorithm", "CRC16-Modbus (0xA001)");
        rule.put("branchCode", "如 BJ01=北京西单 / SH01=上海浦东 / SZ01=深圳南山 / HZ01=杭州西湖");
        return Result.success(rule);
    }
}
