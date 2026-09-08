package com.icbc.lingmou.controller;

import com.icbc.lingmou.common.BusinessException;
import com.icbc.lingmou.common.Result;
import com.icbc.lingmou.common.ResultCode;
import com.icbc.lingmou.dto.request.QRCodeGenerateRequest;
import com.icbc.lingmou.entity.Branch;
import com.icbc.lingmou.entity.Voucher;
import com.icbc.lingmou.mapper.BranchMapper;
import com.icbc.lingmou.service.AiPrecheckService;
import com.icbc.lingmou.service.QRCodeService;
import com.icbc.lingmou.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务直通码（QRCode）控制器
 *
 * 核心流程：
 *   1. POST /api/qrcode/generate → 先调 Python /api/ai/precheck 做材料预检
 *   2. 预检通过 → 生成凭证号 + 业务直通码内容 → 写入 vouchers 表
 *   3. 预检不通过 → 返回缺失材料（业务直通码拦截）
 */
@Slf4j
@Tag(name = "业务直通码", description = "生成办理业务的业务直通码（二维码）")
@RestController
@RequestMapping("/api/qrcode")
@RequiredArgsConstructor
public class QRCodeController {

    private final AiPrecheckService aiPrecheckService;
    private final VoucherService voucherService;
    private final BranchMapper branchMapper;
    private final QRCodeService qrCodeService;

    @Operation(summary = "生成业务直通码", description = "先调 Python AI 预检材料是否齐全，通过后生成凭证号 + 直通码")
    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(
            @Valid @RequestBody QRCodeGenerateRequest request,
            HttpServletRequest httpRequest) {

        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // ====== Step 1: AI 预检 ======
        AiPrecheckService.PrecheckResult precheck = aiPrecheckService.precheck(
                request.getBusinessType(), request.getMaterials());

        Map<String, Object> result = new HashMap<>();
        result.put("passed", precheck.passed());
        result.put("missing", precheck.missing());
        result.put("required", precheck.required());

        if (!precheck.passed()) {
            // 预检未通过，业务直通码拦截
            log.warn("[QRCode] AI预检未通过, businessType={}, missing={}",
                    request.getBusinessType(), precheck.missing());
            throw new BusinessException(ResultCode.AI_PRECHECK_REJECTED,
                    "材料不完整，缺少：" + String.join("、", precheck.missing()));
        }

        // ====== Step 2: 找网点（取 branchId 对应的 branchCode）======
        Long branchId = request.getBranchId();
        String branchCode = "BK00"; // 默认编码（未指定网点时用兜底）
        if (branchId != null) {
            Branch branch = branchMapper.selectById(branchId);
            if (branch == null) {
                throw new BusinessException(ResultCode.BRANCH_NOT_FOUND);
            }
            if (branch.getBranchCode() != null && !branch.getBranchCode().isBlank()) {
                branchCode = branch.getBranchCode();
            } else {
                branchCode = String.format("BR%02d", branchId);
            }
        }

        // ====== Step 3: 生成凭证号 ======
        String voucherNum = voucherService.generateVoucherNum(branchCode);
        String sn = generateSn();

        // ====== Step 4: 构建业务直通码内容（前端扫码后跳转的 URL 或 JSON）======
        Map<String, Object> qrcodePayload = new HashMap<>();
        qrcodePayload.put("voucherNum", voucherNum);
        qrcodePayload.put("businessType", request.getBusinessType());
        qrcodePayload.put("branchCode", branchCode);
        qrcodePayload.put("userId", userId);
        qrcodePayload.put("timestamp", System.currentTimeMillis());
        String qrcodeContent = "LINGMOU://" + voucherNum; // 扫码协议格式

        // ====== Step 5: 保存 Voucher 记录 ======
        Voucher voucher = new Voucher();
        voucher.setVoucherNum(voucherNum);
        voucher.setSn(sn);
        voucher.setQrcodeContent(qrcodeContent);
        voucherService.save(voucher);

        // ====== Step 6: 组装返回 ======
        result.put("voucherNum", voucherNum);
        result.put("sn", sn);
        result.put("branchCode", branchCode);
        result.put("qrcodeContent", qrcodeContent);
        result.put("userId", userId);
        result.put("generatedAt", LocalDateTime.now().toString());

        log.info("[QRCode] 直通码生成成功, voucherNum={}, userId={}", voucherNum, userId);
        return Result.success("业务直通码生成成功", result);
    }

    /**
     * 生成流水号 SN：业务直通码 + 毫秒时间戳 + 4位随机
     */
    private String generateSn() {
        long ts = System.currentTimeMillis();
        int rand = (int) (Math.random() * 10000);
        return "SN" + ts + String.format("%04d", rand);
    }

    // ======================================================================
    // 二维码图片接口（ZXing 生成真正的 PNG）
    // ======================================================================

    @Operation(summary = "返回业务直通码二维码图片",
            description = "根据凭证号查询业务直通码内容，并用 ZXing 生成 PNG 二维码图片返回。\n"
                    + "前端可直接 <img src=\"/api/qrcode/Txxxx/image\"> 展示。")
    @GetMapping(value = "/{voucherNum}/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrImage(
            @Parameter(description = "凭证号，21位")
            @PathVariable String voucherNum,
            @Parameter(description = "图片像素边长（默认 300）")
            @RequestParam(value = "size", defaultValue = "300") int size) {

        if (!voucherService.isValidVoucherNum(voucherNum)) {
            throw new BusinessException(ResultCode.VOUCHER_NUM_NOT_FOUND, "凭证号格式不合法");
        }

        Voucher voucher = voucherService.findByVoucherNum(voucherNum);
        if (voucher == null) {
            throw new BusinessException(ResultCode.VOUCHER_NUM_NOT_FOUND);
        }

        // 用业务直通码内容生成二维码
        String content = voucher.getQrcodeContent();
        if (content == null || content.isBlank()) {
            content = "LINGMOU://" + voucherNum;
        }

        byte[] png;
        try {
            png = qrCodeService.generatePngBytes(content, size);
        } catch (Exception e) {
            log.error("[QRCode] 生成二维码图片失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "二维码图片生成失败: " + e.getMessage());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentDispositionFormData("inline", voucherNum + ".png");
        return new ResponseEntity<>(png, headers, 200);
    }
}
