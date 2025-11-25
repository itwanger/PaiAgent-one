package com.paiagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 工作流实体类
 */
@Data
@TableName("workflow")
public class Workflow {
    
    /**
     * 工作流主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 工作流名称
     */
    private String name;
    
    /**
     * 工作流描述
     */
    private String description;
    
    /**
     * 工作流配置数据(节点和连线) - JSON 格式
     */
    private String flowData;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 逻辑删除标识
     */
    @TableLogic
    private Integer deleted;
}
