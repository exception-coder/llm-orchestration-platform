package com.exceptioncoder.llm.domain.devplan.service;

import com.exceptioncoder.llm.domain.devplan.model.ProfileDimension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * 项目画像 Markdown 读取接口 —— 供 Application 层读取和解析画像文件。
 *
 * @author zhangkai
 * @since 2026-04-11
 */
public interface ProfileReader {

    /**
     * 读取画像文件全文。
     */
    String readFull(Path profilePath) throws IOException;

    /**
     * 将 Markdown 按 {@code ## N.} 二级标题分片，返回维度→内容映射。
     */
    Map<ProfileDimension, String> parse(String markdown);
}
