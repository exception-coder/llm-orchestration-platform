package com.exceptioncoder.llm.infrastructure.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TextBlockExtractor 手动测试入口，用于从大日志文件中提取指定响应块。
 *
 * @author 张凯
 * @date 2026-04-02
 */
public class TextBlockExtractorRunner {

    private static final Logger log = LoggerFactory.getLogger(TextBlockExtractorRunner.class);

    private static final String LOG_FILE = "D:\\data\\logs\\kpay-pos-order-manage-tmp.log";

    private static final String START_MARKER = "response: {";
    private static final String END_MARKER = "\"code\":10000}";

    private static final String OUTPUT_DIR = "D:\\data\\logs";
    private static final String OUTPUT_PREFIX = "kpay-pos-order-manage-extracted";

    public static void main(String[] args) throws IOException {
        long startTime = System.currentTimeMillis();
        AtomicLong blockIndex = new AtomicLong(0);

        long count = TextBlockExtractor.extractFromFile(
                Path.of(LOG_FILE),
                START_MARKER,
                END_MARKER,
                block -> {
                    long idx = blockIndex.incrementAndGet();
                    Path outPath = Path.of(OUTPUT_DIR, OUTPUT_PREFIX + "-" + idx + ".log");
                    try (BufferedWriter writer = Files.newBufferedWriter(outPath, StandardCharsets.UTF_8)) {
                        writer.write(block);
                    } catch (IOException e) {
                        throw new UncheckedIOException("写出文本块失败, block=" + idx, e);
                    }
                }
        );

        log.info("Total matched blocks : {}", count);
        log.info("Time cost            : {} ms", System.currentTimeMillis() - startTime);
        log.info("Output dir           : {}", OUTPUT_DIR);
    }
}
