package com.exceptioncoder.llm.domain.model;

import java.util.List;

/**
 * 文档目录树节点
 */
public record DocTreeNode(
        String name,
        String path,
        NodeType type,
        String category,
        String description,
        List<DocTreeNode> children
) {
    public enum NodeType {
        FILE, DIRECTORY
    }
}
