package com.icbc.lingmou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.lingmou.common.BusinessException;
import com.icbc.lingmou.common.ResultCode;
import com.icbc.lingmou.dto.request.PreFormRequest;
import com.icbc.lingmou.dto.response.PreFormResponse;
import com.icbc.lingmou.entity.PreForm;
import com.icbc.lingmou.mapper.PreFormMapper;
import com.icbc.lingmou.service.PreFormService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 预填单Service实现
 */
@Service
@RequiredArgsConstructor
public class PreFormServiceImpl implements PreFormService {

    private final PreFormMapper preFormMapper;

    @Override
    @Transactional
    public PreFormResponse createPreForm(Long userId, PreFormRequest request) {
        PreForm preForm = new PreForm();
        preForm.setUserId(userId);
        preForm.setBusinessType(request.getBusinessType());
        preForm.setRawText(request.getRawText());
        preForm.setParsedJson(request.getParsedJson());
        preForm.setStatus("DRAFT");

        preFormMapper.insert(preForm);
        return toResponse(preForm);
    }

    @Override
    public List<PreFormResponse> getUserPreForms(Long userId) {
        List<PreForm> preForms = preFormMapper.selectList(
            new LambdaQueryWrapper<PreForm>()
                .eq(PreForm::getUserId, userId)
                .orderByDesc(PreForm::getCreatedAt)
        );
        return preForms.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public PreFormResponse getPreFormById(Long id, Long userId) {
        PreForm preForm = preFormMapper.selectById(id);
        if (preForm == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!preForm.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return toResponse(preForm);
    }

    @Override
    @Transactional
    public PreFormResponse updatePreForm(Long id, Long userId, PreFormRequest request) {
        PreForm preForm = preFormMapper.selectById(id);
        if (preForm == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!preForm.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        preForm.setBusinessType(request.getBusinessType());
        preForm.setRawText(request.getRawText());
        preForm.setParsedJson(request.getParsedJson());

        preFormMapper.updateById(preForm);
        return toResponse(preForm);
    }

    @Override
    @Transactional
    public void deletePreForm(Long id, Long userId) {
        PreForm preForm = preFormMapper.selectById(id);
        if (preForm == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!preForm.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        preFormMapper.deleteById(id);
    }

    private PreFormResponse toResponse(PreForm preForm) {
        return PreFormResponse.builder()
            .id(preForm.getId())
            .userId(preForm.getUserId())
            .businessType(preForm.getBusinessType())
            .rawText(preForm.getRawText())
            .parsedJson(preForm.getParsedJson())
            .status(preForm.getStatus())
            .createdAt(preForm.getCreatedAt())
            .updatedAt(preForm.getUpdatedAt())
            .build();
    }
}
