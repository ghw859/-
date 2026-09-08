package com.icbc.lingmou.service;

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
}
