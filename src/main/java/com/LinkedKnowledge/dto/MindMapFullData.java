package com.LinkedKnowledge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 完整导图数据 DTO — 对应 mind-elixir 的 MindElixirData
 * 包含: nodeData(节点树) + arrows(关系连线) + summaries(汇总) + direction(布局方向)
 *
 * 关键:后端不解析 arrows/summaries 内部结构,直接透传 JSON
 *
 * 字段说明:
 *   - nodeData: 树形结构,根节点 + 嵌套 children
 *   - arrows: 关系连线列表(用户用 🔗 工具创建)
 *   - summaries: 汇总节点列表(mind-elixir 支持,前端暂未用)
 *   - direction: 布局方向(0=LEFT, 1=RIGHT, 2=SIDE),前端 Konva 渲染时忽略
 *
 * 用 Object 而非具体类型(原因):
 *   Jackson 泛型擦除:Result<MindMapFullData> 序列化失败
 *   Controller 改用 Result<Object>,手动构建 LinkedHashMap
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MindMapFullData {
    private MindMapNode nodeData;
    private Object arrows;      // 关系连线列表(透传 JSON)
    private Object summaries;   // 汇总节点列表(透传 JSON)
    private Integer direction;  // 布局方向: 0=LEFT, 1=RIGHT, 2=SIDE

    // 从 JSON 反序列化时,nodeData 被解析为 MindMapNode,
    // arrows/summaries 被 Jackson 解析为 List<Map>
}
