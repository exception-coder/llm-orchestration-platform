package com.exceptioncoder.llm.domain.devplan.analysis;

import java.util.Map;

/**
 * 代码向量化的嵌入文本单元。
 *
 * @param text     用于 embedding 的摘要文本（Javadoc + 类声明 + 方法签名）
 * @param metadata 元数据（filePath、className、packageName 等）
 */
public record EmbeddingText(String text, Map<String, Object> metadata) {}
