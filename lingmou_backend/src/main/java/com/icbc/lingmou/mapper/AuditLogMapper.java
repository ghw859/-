package com.icbc.lingmou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.icbc.lingmou.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志 Mapper 接口
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
