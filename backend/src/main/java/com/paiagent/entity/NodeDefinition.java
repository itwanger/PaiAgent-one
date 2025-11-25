package com.paiagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 节点定义实体类
 */
@Data
@TableName("node_definition")
public class NodeDefinition {
    
    /**
     * 节点定义主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 节点类型标识
     */
    private String nodeType;
    
    /**
     * 显示名称
     */
    private String displayName;
    
    /**
     * 节点分类(LLM/TOOL)
     */
    private String category;
    
    /**
     * 节点图标
     */
    private String icon;
    
    /**
     * 输入参数 JSON Schema
     */
    private String inputSchema;
    
    /**
     * 输出参数 JSON Schema
     */
    private String outputSchema;
    
    /**
     * 配置参数 JSON Schema
     */
    private String configSchema;
    
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
