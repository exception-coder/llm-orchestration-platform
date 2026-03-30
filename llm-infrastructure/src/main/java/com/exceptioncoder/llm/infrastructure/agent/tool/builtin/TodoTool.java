package com.exceptioncoder.llm.infrastructure.agent.tool.builtin;

import com.exceptioncoder.llm.domain.model.SecretaryTodo;
import com.exceptioncoder.llm.domain.repository.SecretaryTodoRepository;
import com.exceptioncoder.llm.infrastructure.agent.tool.Tool;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolParam;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;

/**
 * 待办管理工具
 */
@Slf4j
public class TodoTool {

    private static final String DEFAULT_USER = "default";

    private final SecretaryTodoRepository todoRepository;

    public TodoTool(SecretaryTodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    @Tool(name = "todo_add", description = "添加待办事项")
    public String addTodo(
            @ToolParam(value = "title", description = "待办标题") String title,
            @ToolParam(value = "priority", description = "优先级：LOW/MEDIUM/HIGH/URGENT，默认 MEDIUM", required = false) String priority,
            @ToolParam(value = "dueDate", description = "截止日期，格式：yyyy-MM-dd", required = false) String dueDate
    ) {
        try {
            SecretaryTodo.Priority p = parsePriority(priority);
            LocalDate due = dueDate != null ? LocalDate.parse(dueDate) : null;
            SecretaryTodo todo = new SecretaryTodo(null, DEFAULT_USER, title, p, due, false, null);
            SecretaryTodo saved = todoRepository.save(todo);
            return "待办已添加，ID: " + saved.id() + "，优先级: " + p.name();
        } catch (Exception e) {
            log.error("添加待办失败", e);
            return "添加失败: " + e.getMessage();
        }
    }

    @Tool(name = "todo_list", description = "查看待办列表，默认只显示未完成的")
    public String listTodos(
            @ToolParam(value = "showAll", description = "是否显示全部（含已完成），默认 false", required = false) Boolean showAll
    ) {
        try {
            List<SecretaryTodo> todos = (showAll != null && showAll)
                    ? todoRepository.findByUserId(DEFAULT_USER)
                    : todoRepository.findPending(DEFAULT_USER);
            if (todos.isEmpty()) {
                return "没有待办事项";
            }
            StringBuilder sb = new StringBuilder("待办列表（" + todos.size() + " 条）:\n\n");
            for (SecretaryTodo t : todos) {
                sb.append("[").append(t.id()).append("] ");
                sb.append(t.done() ? "[完成] " : "[待办] ");
                sb.append(t.title()).append(" [").append(t.priority().name()).append("]\n");
                if (t.dueDate() != null) sb.append("  截止: ").append(t.dueDate()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("查询待办失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    @Tool(name = "todo_done", description = "标记待办为已完成")
    public String markTodoDone(
            @ToolParam(value = "id", description = "待办 ID") Long id
    ) {
        try {
            todoRepository.markDone(id);
            return "待办 " + id + " 已标记为完成";
        } catch (Exception e) {
            log.error("标记待办失败: id={}", id, e);
            return "操作失败: " + e.getMessage();
        }
    }

    private SecretaryTodo.Priority parsePriority(String priority) {
        if (priority == null) return SecretaryTodo.Priority.MEDIUM;
        try {
            return SecretaryTodo.Priority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SecretaryTodo.Priority.MEDIUM;
        }
    }
}
