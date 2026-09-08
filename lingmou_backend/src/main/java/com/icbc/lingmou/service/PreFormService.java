package com.icbc.lingmou.service;

import com.icbc.lingmou.dto.request.PreFormRequest;
import com.icbc.lingmou.dto.response.PreFormResponse;

import java.util.List;

/**
 * 预填单Service接口
 */
public interface PreFormService {

    /**
     * 创建预填单
     */
    PreFormResponse createPreForm(Long userId, PreFormRequest request);

    /**
     * 查询用户的预填单列表
     */
    List<PreFormResponse> getUserPreForms(Long userId);

    /**
     * 查询单个预填单详情
     */
    PreFormResponse getPreFormById(Long id, Long userId);

    /**
     * 更新预填单
     */
    PreFormResponse updatePreForm(Long id, Long userId, PreFormRequest request);

    /**
     * 删除预填单
     */
    void deletePreForm(Long id, Long userId);
}
