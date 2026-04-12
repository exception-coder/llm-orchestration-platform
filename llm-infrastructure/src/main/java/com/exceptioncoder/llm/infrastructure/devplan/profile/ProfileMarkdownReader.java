package com.exceptioncoder.llm.infrastructure.devplan.profile;

import com.exceptioncoder.llm.domain.devplan.model.ProfileDimension;
import com.exceptioncoder.llm.domain.devplan.service.ProfileReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 项目画像 Markdown 读取器 —— 按 {@code ## N. 维度名} 分片，返回维度→内容映射。
 *
 * @author zhangkai
 * @since 2026-04-11
 */
@Slf4j
@Component
public class ProfileMarkdownReader implements ProfileReader {

    private static final Pattern SECTION_HEADER = Pattern.compile(
            "^## (\\d+)\\.\\s+(.+)$", Pattern.MULTILINE);

    /**
     * 读取 Markdown 文件全文。
     */
    public String readFull(Path profilePath) throws IOException {
        return Files.readString(profilePath);
    }

    /**
     * 将 Markdown 按 {@code ## } 二级标题分片，匹配到 {@link ProfileDimension} 枚举。
     *
     * @param markdown project-profile.md 的完整内容
     * @return key=ProfileDimension, value=该维度的完整 Markdown 片段（含标题行）
     */
    public Map<ProfileDimension, String> parse(String markdown) {
        Map<ProfileDimension, String> result = new LinkedHashMap<>();
        String[] sections = markdown.split("(?=^## \\d+\\.)", Pattern.MULTILINE);

        for (String section : sections) {
            Matcher m = SECTION_HEADER.matcher(section);
            if (m.find()) {
                int index = Integer.parseInt(m.group(1));
                ProfileDimension dim = ProfileDimension.fromIndex(index);
                if (dim != null) {
                    result.put(dim, section.trim());
                } else {
                    log.debug("Unknown dimension index: {}", index);
                }
            }
        }

        log.info("Parsed {} dimensions from Markdown", result.size());
        return result;
    }
}
