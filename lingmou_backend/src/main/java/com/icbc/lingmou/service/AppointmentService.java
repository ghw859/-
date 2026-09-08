package com.icbc.lingmou.service;

import com.icbc.lingmou.common.PageResult;
import com.icbc.lingmou.dto.request.AppointmentHistoryRequest;
import com.icbc.lingmou.dto.request.AppointmentRequest;
import com.icbc.lingmou.dto.response.AppointmentResponse;
import com.icbc.lingmou.entity.Appointment;

import java.util.List;

/**
 * 预约Service接口
 */
public interface AppointmentService {

    /**
     * 创建预约
     */
    AppointmentResponse createAppointment(Long userId, AppointmentRequest request);

    /**
     * 查询用户的预约列表
     */
    List<AppointmentResponse> getUserAppointments(Long userId);

    /**
     * 查询单个预约详情
     */
    AppointmentResponse getAppointmentById(Long id, Long userId);

    /**
     * 取消预约
     */
    void cancelAppointment(Long id, Long userId);

    /**
     * 生成排队号
     */
    String generateQueueNumber(Long branchId);

    /**
     * 检查时段是否已约满（<=10人）
     */
    boolean isSlotFull(Long branchId, String date, String timeSlot);

    /**
     * 检查用户是否在同一时段有其他预约
     */
    boolean hasConflictAppointment(Long userId, String date, String timeSlot);

    /**
     * 查询预约进度
     */
    AppointmentResponse getProgress(Long id, Long userId);

    /**
     * 推进预约进度（手动按钮）
     */
    AppointmentResponse advanceProgress(Long id, Long userId);

    /**
     * 历史预约查询（分页+多条件）
     */
    PageResult<AppointmentResponse> getHistory(Long userId, AppointmentHistoryRequest request);
}
