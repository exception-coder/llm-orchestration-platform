package com.exceptioncoder.llm.api.controller.note;

import com.exceptioncoder.llm.api.dto.NoteEncryptedInputDTO;
import com.exceptioncoder.llm.api.dto.NoteInputDTO;
import com.exceptioncoder.llm.api.dto.NoteResponseDTO;
import com.exceptioncoder.llm.application.usecase.NoteCategoryUseCase;
import com.exceptioncoder.llm.application.usecase.NoteUseCase;
import com.exceptioncoder.llm.domain.model.Note;
import com.exceptioncoder.llm.domain.model.NoteCategory;
import com.exceptioncoder.llm.domain.repository.NoteCategoryRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 记录 API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {

    private final NoteUseCase noteUseCase;
    private final NoteCategoryUseCase categoryUseCase;
    private final NoteCategoryRepository categoryRepository;

    public NoteController(
            NoteUseCase noteUseCase,
            NoteCategoryUseCase categoryUseCase,
            NoteCategoryRepository categoryRepository) {
        this.noteUseCase = noteUseCase;
        this.categoryUseCase = categoryUseCase;
        this.categoryRepository = categoryRepository;
    }

    /**
     * 捕获记录（AI 分类 + 存储）
     */
    @PostMapping
    public NoteResponseDTO capture(@Valid @RequestBody NoteInputDTO dto) {
        log.info("收到记录捕获请求，长度: {}", dto.getContent().length());

        Note note = noteUseCase.capture(
                dto.getContent(),
                Boolean.TRUE.equals(dto.getIsVoice()),
                dto.getModel()
        );

        String icon = categoryRepository.findById(note.getCategoryId())
                .map(NoteCategory::getIcon)
                .orElse("📝");

        return NoteResponseDTO.fromDomain(note, icon);
    }

    /**
     * 存储加密记录（前端已加密，跳过 AI 分类）
     */
    @PostMapping("/encrypted")
    public NoteResponseDTO saveEncrypted(@Valid @RequestBody NoteEncryptedInputDTO dto) {
        log.info("收到加密记录存储请求");

        Note note = noteUseCase.saveEncrypted(
                dto.getCategoryId(),
                dto.getTitle(),
                dto.getRawInput(),
                dto.getEncryptedContent(),
                Boolean.TRUE.equals(dto.getIsVoice()),
                dto.getTags()
        );

        String icon = categoryRepository.findById(note.getCategoryId())
                .map(NoteCategory::getIcon)
                .orElse("🔒");

        return NoteResponseDTO.fromDomain(note, icon);
    }

    /**
     * 获取所有记录
     */
    @GetMapping
    public List<NoteResponseDTO> getAllNotes(
            @RequestParam(required = false) Long categoryId) {

        List<Note> notes = (categoryId != null)
                ? noteUseCase.getNotesByCategory(categoryId)
                : noteUseCase.getAllNotes();

        return enrichWithIcons(notes);
    }

    /**
     * 获取单个记录
     */
    @GetMapping("/{id}")
    public NoteResponseDTO getNote(@PathVariable Long id) {
        Note note = noteUseCase.getNote(id);

        String icon = categoryRepository.findById(note.getCategoryId())
                .map(NoteCategory::getIcon)
                .orElse("📝");

        return NoteResponseDTO.fromDomain(note, icon);
    }

    /**
     * 搜索记录
     */
    @GetMapping("/search")
    public List<NoteResponseDTO> search(@RequestParam String keyword) {
        return enrichWithIcons(noteUseCase.search(keyword));
    }

    /**
     * 删除记录
     */
    @DeleteMapping("/{id}")
    public Map<String, Boolean> deleteNote(@PathVariable Long id) {
        noteUseCase.deleteNote(id);
        return Map.of("deleted", true);
    }

    private List<NoteResponseDTO> enrichWithIcons(List<Note> notes) {
        return notes.stream()
                .map(note -> {
                    String icon = categoryRepository.findById(note.getCategoryId())
                            .map(NoteCategory::getIcon)
                            .orElse("📝");
                    return NoteResponseDTO.fromDomain(note, icon);
                })
                .collect(Collectors.toList());
    }
}
