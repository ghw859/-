package com.icbc.lingmou.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icbc.lingmou.common.BusinessException;
import com.icbc.lingmou.common.Result;
import com.icbc.lingmou.common.ResultCode;
import com.icbc.lingmou.dto.request.QRCodeGenerateRequest;
import com.icbc.lingmou.entity.Branch;
import com.icbc.lingmou.entity.User;
import com.icbc.lingmou.entity.Voucher;
import com.icbc.lingmou.mapper.BranchMapper;
import com.icbc.lingmou.mapper.UserMapper;
import com.icbc.lingmou.service.AiPrecheckService;
import com.icbc.lingmou.service.AuditLogService;
import com.icbc.lingmou.service.QRCodeService;
import com.icbc.lingmou.service.VoucherService;
import com.icbc.lingmou.service.support.PrecheckBizSupport;
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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务直通码（QRCode）控制器
 *
 * 核心流程（T3 全链路，Day9 由 Java-B 接通前端 QRCodeView）：
 *   1. POST /api/qrcode/generate → 业务类型/材料归一化 → 调 Python /api/ai/precheck 材料预检
 *   2. 预检不通过 → HTTP 200 / code=0 / passed=false，data.missingLabels 供前端弹窗
 *      （预检拦截是正常业务分支，不是系统错误；未知业务 60004 仍走错误码）
 *   3. 预检通过 → 生成 T 凭证号 + 业务直通码内容 → 写 vouchers 表（可挂 appointment_id）
 *   4. 同步写 audit_logs 哈希链（VOUCHER_GENERATE），区块链审计页 GET /api/audit/my 可查
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
    private final UserMapper userMapper;
    private final QRCodeService qrCodeService;
    private final AuditLogService auditLogService;
    private final PrecheckBizSupport precheckBizSupport;
    private final ObjectMapper objectMapper;

    @Operation(summary = "生成业务直通码", description = "先调 Python AI 预检材料是否齐全；通过后生成 T 凭证号 + 直通码并落审计链")
    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(
            @Valid @RequestBody QRCodeGenerateRequest request,
            HttpServletRequest httpRequest) {

        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // ====== Step 1: 业务类型/材料归一化后调 AI 预检 ======
        String aiBizType = precheckBizSupport.normalizeBizType(request.getBusinessType());
        Map<String, Object> aiMaterials = precheckBizSupport.normalizeMaterials(
                request.getBusinessType(), request.getMaterials());

        AiPrecheckService.PrecheckResult precheck = aiPrecheckService.precheck(aiBizType, aiMaterials);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("passed", precheck.passed());
        result.put("businessType", request.getBusinessType());
        result.put("bizTypeName", precheckBizSupport.bizTypeName(request.getBusinessType()));
        result.put("missing", precheck.missing());
        result.put("missingLabels", precheckBizSupport.labels(precheck.missing()));
        result.put("required", precheck.required());

        if (!precheck.passed()) {
            // 预检未通过：正常业务分支返回（code=0），前端弹窗展示缺失材料、不跳转
            log.warn("[QRCode] AI预检未通过, businessType={}, aiBizType={}, missing={}",
                    request.getBusinessType(), aiBizType, precheck.missing());
            return Result.success("材料预检未通过，请按提示补齐材料", result);
        }

        // ====== Step 2: 找网点（取 branchId 对应的 branchCode）======
        Long branchId = request.getBranchId();
        String branchCode = "BK00"; // 默认编码（预填单链路未指定网点时兜底）
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

        // ====== Step 4: 构建业务直通码内容 ======
        String qrcodeContent = "LINGMOU://" + voucherNum; // 扫码协议格式

        // ====== Step 5: 保存 Voucher 记录（appointmentId 打通预约↔直通码数据通路）======
        Voucher voucher = new Voucher();
        voucher.setVoucherNum(voucherNum);
        voucher.setAppointmentId(request.getAppointmentId());
        voucher.setSn(sn);
        voucher.setQrcodeContent(qrcodeContent);
        voucherService.save(voucher);

        // ====== Step 6: 写区块链审计哈希链（best-effort，失败不回滚已签发的直通码）======
        User user = userMapper.selectById(userId);
        String loginName = (String) httpRequest.getAttribute("username");
        Map<String, String> identity = resolveIdentity(request.getMaterials(), user);
        String bizTypeName = precheckBizSupport.bizTypeName(request.getBusinessType());

        Map<String, Object> auditContent = new LinkedHashMap<>();
        auditContent.put("preFormId", request.getPreFormId());
        auditContent.put("appointmentId", request.getAppointmentId());
        auditContent.put("voucherNum", voucherNum);
        auditContent.put("sn", sn);
        auditContent.put("branchCode", branchCode);
        auditContent.put("businessType", request.getBusinessType());
        auditContent.put("bizTypeName", bizTypeName);
        auditContent.put("userName", identity.get("userName"));
        auditContent.put("idCard", identity.get("idCard"));
        auditContent.put("phone", identity.get("phone"));
        auditContent.put("extraData", extractExtraData(request.getMaterials()));
        auditContent.put("status", "已提交");

        try {
            auditLogService.writeLog(userId, loginName,
                    AuditLogService.ACTION_VOUCHER_GENERATE, objectMapper.writeValueAsString(auditContent));
        } catch (Exception e) {
            log.error("[QRCode] 审计链写入失败（直通码已签发）: voucherNum={}, err={}", voucherNum, e.getMessage());
        }

        // ====== Step 7: 组装返回 ======
        result.put("voucherNum", voucherNum);
        result.put("sn", sn);
        result.put("branchCode", branchCode);
        result.put("qrcodeContent", qrcodeContent);
        result.put("appointmentId", request.getAppointmentId());
        result.put("preFormId", request.getPreFormId());
        result.put("qrcodeImageUrl", "/api/qrcode/" + voucherNum + "/image");
        result.put("userId", userId);
        result.put("generatedAt", LocalDateTime.now().toString());

        log.info("[QRCode] 直通码生成成功, voucherNum={}, userId={}, preFormId={}",
                voucherNum, userId, request.getPreFormId());
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

    /**
     * 客户身份要素：优先取本次表单材料，缺省回退用户档案
     */
    private Map<String, String> resolveIdentity(Map<String, Object> materials, User user) {
        Map<String, Object> m = materials != null ? materials : Map.of();
        String userName = strOr(m.get("userName"),
                user != null ? firstNonBlank(user.getRealName(), user.getUsername()) : "");
        String idCard = strOr(m.get("idCard"), user != null ? user.getIdCard() : "");
        String phone = strOr(m.get("phone"), user != null ? user.getPhone() : "");
        return Map.of("userName", userName, "idCard", idCard, "phone", phone);
    }

    /**
     * 业务专项要素：剔除身份三要素，其余表单字段整体作为 extraData 落链/展示
     */
    private Map<String, Object> extractExtraData(Map<String, Object> materials) {
        Map<String, Object> extra = new LinkedHashMap<>();
        if (materials == null) {
            return extra;
        }
        for (Map.Entry<String, Object> e : materials.entrySet()) {
            if ("userName".equals(e.getKey()) || "idCard".equals(e.getKey()) || "phone".equals(e.getKey())) {
                continue;
            }
            if (e.getValue() != null) {
                extra.put(e.getKey(), e.getValue());
            }
        }
        return extra;
    }

    private static String strOr(Object value, String fallback) {
        if (value != null && !value.toString().isBlank()) {
            return value.toString();
        }
        return fallback == null ? "" : fallback;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }

    // ======================================================================
    // 二维码图片接口（ZXing 生成真正的 PNG，公开访问，<img> 可直接引用）
    // ======================================================================

    @Operation(summary = "返回业务直通码二维码图片",
            description = "根据凭证号查询直通码内容，并用 ZXing 生成 PNG 二维码图片返回。T/V 凭证号均支持。\n"
                    + "前端可直接 <img src=\"/api/qrcode/Txxxx/image\"> 展示。")
    @GetMapping(value = "/{voucherNum}/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrImage(
            @Parameter(description = "凭证号：T 开头 21 位直通码，或 V 开头预约凭证")
            @PathVariable String voucherNum,
            @Parameter(description = "图片像素边长（默认 300）")
            @RequestParam(value = "size", defaultValue = "300") int size) {

        if (!voucherService.isValidVoucherNum(voucherNum)) {
            throw new BusinessException(ResultCode.VOUCHER_NUM_NOT_FOUND, "凭证号格式不合法");
        }

        Voucher voucher = voucherService.findByVoucherNum(voucherNum);
        // V 预约凭证在 vouchers 表缺行时回查预约（历史数据兼容），不再抛 20007
        if (voucher == null && voucherNum.startsWith("V")) {
            voucher = voucherService.resolveAppointmentVoucher(voucherNum);
        }
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
