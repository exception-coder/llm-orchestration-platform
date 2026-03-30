package com.exceptioncoder.llm.application.usecase;

import com.exceptioncoder.llm.domain.repository.DocStructureVersionRepository;
import com.exceptioncoder.llm.infrastructure.agent.DocStructureAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 文档目录刷新用例
 * 流程：读取 README → 计算 hash → hash 未变则跳过 → 启动 DocStructureAgent
 */
@Slf4j
@Service
public class DocRefreshUseCase {

    private static final String README_PATH = "classpath:docs/README.md";

    private final DocStructureVersionRepository versionRepository;
    private final DocStructureAgent docStructureAgent;
    private final ResourceLoader resourceLoader;

    public DocRefreshUseCase(
            DocStructureVersionRepository versionRepository,
            DocStructureAgent docStructureAgent,
            ResourceLoader resourceLoader
    ) {
        this.versionRepository = versionRepository;
        this.docStructureAgent = docStructureAgent;
        this.resourceLoader = resourceLoader;
    }

    /**
     * 触发目录刷新
     *
     * @param force 为 true 时跳过 hash 检测，强制重新解析
     * @return 刷新结果描述
     */
    public String refresh(boolean force) {
        String readmeContent = readReadme();
        String readmeHash = sha256(readmeContent);

        if (!force) {
            boolean unchanged = versionRepository.findActive()
                    .map(v -> readmeHash.equals(v.getReadmeHash()))
                    .orElse(false);
            if (unchanged) {
                log.info("README.md 内容未变化（hash={}），跳过解析", readmeHash);
                return "README 未变化，无需刷新";
            }
        }

        log.info("启动 DocStructureAgent，readmeHash={}", readmeHash);
        String result = docStructureAgent.execute(readmeContent, readmeHash);
        log.info("DocStructureAgent 完成: {}", result);
        return result;
    }

    private String readReadme() {
        try {
            var resource = resourceLoader.getResource(README_PATH);
            if (!resource.exists()) {
                throw new IllegalStateException("docs/README.md 不存在，请先创建文件");
            }
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("读取 docs/README.md 失败: " + e.getMessage(), e);
        }
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("计算 SHA-256 失败", e);
        }
    }
}
