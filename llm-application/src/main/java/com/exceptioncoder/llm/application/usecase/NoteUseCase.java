package com.exceptioncoder.llm.application.usecase;

import com.exceptioncoder.llm.domain.model.Note;
import com.exceptioncoder.llm.domain.model.NoteClassificationResult;
import com.exceptioncoder.llm.domain.repository.NoteRepository;
import com.exceptioncoder.llm.domain.service.NoteClassifier;
import com.exceptioncoder.llm.infrastructure.note.NoteClassifierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 记录用例
 */
@Slf4j
@Service
public class NoteUseCase {

    private final NoteRepository noteRepository;
    private final NoteClassifier classifier;
    private final NoteCategoryUseCase categoryUseCase;

    public NoteUseCase(
            NoteRepository noteRepository,
            NoteClassifier classifier,
            NoteCategoryUseCase categoryUseCase) {
        this.noteRepository = noteRepository;
        this.classifier = classifier;
        this.categoryUseCase = categoryUseCase;
    }

    /**
     * 捕获记录：AI 分类 + 存储
     *
     * @param rawText  用户原始输入
     * @param isVoice  是否来自语音
     * @param model    使用的模型（可为 null，使用默认路由）
     * @return 存储后的记录
     */
    @Transactional
    public Note capture(String rawText, boolean isVoice, String model) {
        log.info("开始捕获记录，长度: {} 字，语音: {}", rawText.length(), isVoice);

        // 调用 LLM 分类
        NoteClassificationResult result = classifier.classify(rawText);
        log.info("AI 分类结果: 类目={}, 标题={}, 敏感={}",
                result.getCategory(), result.getTitle(), result.getIsSensitive());

        // 获取或创建类目
        var category = categoryUseCase.getOrCreateCategory(
                result.getCategory(),
                result.getCategoryDescription(),
                result.getCategoryIcon()
        );

        // 构建记录
        Note note = Note.builder()
                .categoryId(category.getId())
                .categoryName(category.getName())
                .title(result.getTitle())
                .rawInput(rawText)
                .content(result.getStructuredContent())
                .summary(result.getSummary())
                .isEncrypted(false)
                .isVoice(isVoice)
                .tags(result.getTags())
                .build();

        return noteRepository.save(note);
    }

    /**
     * 存储加密记录（前端加密后直接存储，跳过 AI 分类）
     */
    @Transactional
    public Note saveEncrypted(
            Long categoryId,
            String title,
            String rawText,
            String encryptedContent,
            boolean isVoice,
            List<String> tags) {

        Note note = Note.builder()
                .categoryId(categoryId)
                .title(title)
                .rawInput(rawText)
                .content(encryptedContent)
                .isEncrypted(true)
                .isVoice(isVoice)
                .tags(tags)
                .build();

        return noteRepository.save(note);
    }

    /**
     * 获取所有记录
     */
    public List<Note> getAllNotes() {
        return noteRepository.findAllOrderByCreatedAtDesc();
    }

    /**
     * 按类目获取记录
     */
    public List<Note> getNotesByCategory(Long categoryId) {
        return noteRepository.findByCategoryIdOrderByCreatedAtDesc(categoryId);
    }

    /**
     * 获取单个记录
     */
    public Note getNote(Long id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("记录不存在: " + id));
    }

    /**
     * 搜索记录
     */
    public List<Note> search(String keyword) {
        return noteRepository.search(keyword);
    }

    /**
     * 删除记录
     */
    @Transactional
    public void deleteNote(Long id) {
        noteRepository.deleteById(id);
    }
}
