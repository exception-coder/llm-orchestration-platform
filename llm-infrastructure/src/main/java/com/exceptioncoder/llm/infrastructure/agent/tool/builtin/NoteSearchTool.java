package com.exceptioncoder.llm.infrastructure.agent.tool.builtin;

import com.exceptioncoder.llm.domain.repository.NoteRepository;
import com.exceptioncoder.llm.domain.model.Note;
import com.exceptioncoder.llm.infrastructure.agent.tool.Tool;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 碎片记录搜索工具 -- 检索用户已有的碎片笔记。
 *
 * <p><b>归属智能体：</b>个人秘书智能体（secretary）
 * <br><b>归属 Agent：</b>个人秘书（secretary-default）
 * <br><b>调用阶段：</b>对话过程中按需调用
 * <br><b>业务场景：</b>用户询问历史记录（如"我之前记过哪些关于XX的笔记"）时，
 * 秘书 Agent 调用 note_search 按关键词和类目检索；需要查看具体内容时调用 note_get。
 * 底层查询 NoteRepository，支持模糊搜索和类目过滤。
 */
@Slf4j
@Component
public class NoteSearchTool {

    private final NoteRepository noteRepository;

    public NoteSearchTool(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    @Tool(name = "note_search", description = "搜索已有的碎片记录，支持关键词搜索和类目过滤")
    public String searchNotes(
            @ToolParam(value = "keyword", description = "搜索关键词（匹配标题、内容、标签）", required = false) String keyword,
            @ToolParam(value = "category", description = "类目名称过滤", required = false) String category,
            @ToolParam(value = "limit", description = "返回数量限制，默认10", required = false) Integer limit
    ) {
        try {
            List<Note> notes;
            int maxResults = (limit != null && limit > 0) ? limit : 10;

            if (keyword != null && !keyword.isBlank()) {
                notes = noteRepository.search(keyword);
            } else if (category != null && !category.isBlank()) {
                notes = noteRepository.findAllOrderByCreatedAtDesc().stream()
                        .filter(n -> category.equals(n.getCategoryName()))
                        .collect(Collectors.toList());
            } else {
                notes = noteRepository.findAllOrderByCreatedAtDesc().stream()
                        .limit(20)
                        .collect(Collectors.toList());
            }

            notes = notes.stream()
                    .filter(n -> !n.getIsEncrypted())
                    .limit(maxResults)
                    .collect(Collectors.toList());

            if (notes.isEmpty()) {
                return "未找到匹配的记录";
            }

            StringBuilder sb = new StringBuilder("找到 " + notes.size() + " 条记录:\n\n");
            for (int i = 0; i < notes.size(); i++) {
                Note note = notes.get(i);
                sb.append(i + 1).append(". [").append(note.getCategoryName()).append("] ");
                sb.append(note.getTitle()).append("\n");
                if (note.getSummary() != null) {
                    sb.append("   摘要: ").append(note.getSummary()).append("\n");
                }
                if (note.getTags() != null && !note.getTags().isEmpty()) {
                    sb.append("   标签: ").append(String.join(", ", note.getTags())).append("\n");
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("搜索记录失败", e);
            return "搜索失败: " + e.getMessage();
        }
    }

    @Tool(name = "note_get", description = "获取单条碎片记录的详细内容")
    public String getNote(
            @ToolParam(value = "id", description = "记录 ID") Long id
    ) {
        try {
            var noteOpt = noteRepository.findById(id);
            if (noteOpt.isEmpty()) {
                return "记录不存在: " + id;
            }
            Note note = noteOpt.get();
            if (note.getIsEncrypted()) {
                return "该记录已加密，无法直接查看";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("标题: ").append(note.getTitle()).append("\n");
            sb.append("类目: ").append(note.getCategoryName()).append("\n");
            sb.append("原始输入: ").append(note.getRawInput()).append("\n");
            if (note.getContent() != null) {
                sb.append("内容: ").append(note.getContent()).append("\n");
            }
            if (note.getSummary() != null) {
                sb.append("摘要: ").append(note.getSummary()).append("\n");
            }
            if (note.getTags() != null) {
                sb.append("标签: ").append(String.join(", ", note.getTags()));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("获取记录失败: id={}", id, e);
            return "获取失败: " + e.getMessage();
        }
    }
}
