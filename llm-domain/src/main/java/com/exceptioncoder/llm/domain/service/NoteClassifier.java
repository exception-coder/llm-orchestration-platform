package com.exceptioncoder.llm.domain.service;

import com.exceptioncoder.llm.domain.model.NoteClassificationResult;

/**
 * 记录分类服务接口
 */
public interface NoteClassifier {

    /**
     * 对用户输入进行分类和结构化
     *
     * @param rawText 用户原始输入
     * @return 分类结果
     */
    NoteClassificationResult classify(String rawText);
}
