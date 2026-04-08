package com.exceptioncoder.llm.infrastructure.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 大文本块提取工具类。
 *
 * <p>按行扫描文本（文件或字符串），检测到起始标识所在行时开始收集，
 * 检测到结束标识所在行时结束并输出一个完整文本块。
 * 适用于从大日志文件中流式过滤目标内容片段，内存占用与单个块大小相关。
 *
 * <p>示例：
 * <pre>{@code
 * // 从大文件中流式提取，逐块回调（推荐用于 100MB+ 文件）
 * long count = TextBlockExtractor.extractFromFile(
 *     Path.of("/var/log/app.log"),
 *     "response: {",
 *     "\"code\":10000}",
 *     block -> System.out.println(block)
 * );
 *
 * // 从字符串中提取，返回所有匹配块列表
 * List<String> blocks = TextBlockExtractor.extractFromText(text, "START", "END");
 * }</pre>
 *
 * @author 张凯
 * @date 2026-04-02
 */
public class TextBlockExtractor {

    private static final Logger log = LoggerFactory.getLogger(TextBlockExtractor.class);

    private TextBlockExtractor() {
    }

    /**
     * 从文件中按起止标识流式提取文本块，每提取到一个完整块即触发回调。
     *
     * <p>逐行读取，内存占用与单个块大小相关，适合处理超大文件。
     * 文件末尾仍未出现结束标识的未关闭块将被丢弃，不触发回调。
     *
     * @param filePath    文件路径，不能为 null
     * @param startMarker 起始行标识（行内包含该字符串则视为块起始），不能为 null
     * @param endMarker   结束行标识（行内包含该字符串则视为块结束），不能为 null
     * @param handler     块处理回调，每匹配到一个完整块调用一次，参数为块全文（每行末尾含 '\n'），不能为 null
     * @return 共提取到的块数量
     * @throws IOException 读取文件时发生 I/O 异常
     */
    public static long extractFromFile(Path filePath, String startMarker, String endMarker,
                                       Consumer<String> handler) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            return doExtract(reader, startMarker, endMarker, handler);
        }
    }

    /**
     * 从字符串中按起止标识提取所有匹配文本块，返回块列表。
     *
     * <p>适合内容已在内存中的文本。超大文本请改用
     * {@link #extractFromFile(Path, String, String, Consumer)} 的流式回调版本。
     *
     * @param text        待提取的文本内容，不能为 null
     * @param startMarker 起始行标识，不能为 null
     * @param endMarker   结束行标识，不能为 null
     * @return 所有匹配块的列表，无匹配时返回空列表
     */
    public static List<String> extractFromText(String text, String startMarker, String endMarker) {
        List<String> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
            doExtract(reader, startMarker, endMarker, result::add);
        } catch (IOException e) {
            // StringReader 不会抛出实际 I/O 异常，此处理论上不可达
            log.error("从字符串提取文本块时发生意外异常: {}", e.getMessage(), e);
        }
        return result;
    }

    /**
     * 核心提取逻辑，逐行扫描并按起止标识收集文本块。
     *
     * @param reader      行读取器
     * @param startMarker 起始标识
     * @param endMarker   结束标识
     * @param handler     块处理回调
     * @return 提取到的块数量
     * @throws IOException 读取时发生 I/O 异常
     */
    private static long doExtract(BufferedReader reader, String startMarker, String endMarker,
                                   Consumer<String> handler) throws IOException {
        long matchCount = 0;
        StringBuilder block = null;
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.contains(startMarker)) {
                block = new StringBuilder(512);
            }

            if (block != null) {
                block.append(line).append('\n');

                if (line.contains(endMarker)) {
                    matchCount++;
                    handler.accept(block.toString());
                    block = null;
                }
            }
        }

        if (block != null) {
            log.debug("文件末尾存在未关闭的文本块，已丢弃，末尾内容片段: {}",
                    block.substring(0, Math.min(block.length(), 100)));
        }

        log.debug("文本块提取完成，共匹配 {} 个块", matchCount);
        return matchCount;
    }
}
