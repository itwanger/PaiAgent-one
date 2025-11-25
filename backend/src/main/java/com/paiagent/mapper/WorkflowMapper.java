package com.paiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.paiagent.entity.Workflow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流 Mapper 接口
 */
@Mapper
public interface WorkflowMapper extends BaseMapper<Workflow> {
}
