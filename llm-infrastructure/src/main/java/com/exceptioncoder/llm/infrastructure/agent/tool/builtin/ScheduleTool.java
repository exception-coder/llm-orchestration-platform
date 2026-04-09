package com.exceptioncoder.llm.infrastructure.agent.tool.builtin;

import com.exceptioncoder.llm.domain.model.SecretarySchedule;
import com.exceptioncoder.llm.domain.repository.SecretaryScheduleRepository;
import com.exceptioncoder.llm.infrastructure.agent.tool.Tool;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolParam;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 日程管理工具 -- 提供日程的增、查、完成操作。
 *
 * <p><b>归属智能体：</b>个人秘书智能体（secretary）
 * <br><b>归属 Agent：</b>个人秘书（secretary-default）
 * <br><b>调用阶段：</b>对话过程中按需调用
 * <br><b>业务场景：</b>用户在与秘书对话时提到时间相关事项（如"明天下午3点开会"），
 * 秘书 Agent 调用 schedule_add 创建日程；用户询问近期安排时调用 schedule_list；
 * 事项完成后调用 schedule_done 标记。数据持久化到 secretary_schedule 表。
 */
@Slf4j
public class ScheduleTool {

    private static final String DEFAULT_USER = "default";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SecretaryScheduleRepository scheduleRepository;

    public ScheduleTool(SecretaryScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    @Tool(name = "schedule_add", description = "添加日程安排，支持设置标题、时间、是否提醒")
    public String addSchedule(
            @ToolParam(value = "title", description = "日程标题") String title,
            @ToolParam(value = "startTime", description = "开始时间，格式：yyyy-MM-dd HH:mm") String startTime,
            @ToolParam(value = "endTime", description = "结束时间，格式：yyyy-MM-dd HH:mm", required = false) String endTime,
            @ToolParam(value = "description", description = "日程描述", required = false) String description,
            @ToolParam(value = "reminder", description = "是否提醒，默认 false", required = false) Boolean reminder
    ) {
        try {
            LocalDateTime start = LocalDateTime.parse(startTime, FORMATTER);
            LocalDateTime end = endTime != null ? LocalDateTime.parse(endTime, FORMATTER) : start.plusHours(1);
            SecretarySchedule schedule = new SecretarySchedule(
                    null, DEFAULT_USER, title, description, start, end,
                    reminder != null && reminder, false, null);
            SecretarySchedule saved = scheduleRepository.save(schedule);
            return "日程已添加，ID: " + saved.id() + "，时间: " + startTime;
        } catch (Exception e) {
            log.error("添加日程失败", e);
            return "添加失败: " + e.getMessage();
        }
    }

    @Tool(name = "schedule_list", description = "查看日程列表，可查看未来N天内的日程")
    public String listSchedules(
            @ToolParam(value = "days", description = "查看未来几天的日程，默认7天", required = false) Integer days
    ) {
        try {
            int d = (days != null && days > 0) ? days : 7;
            LocalDateTime from = LocalDateTime.now();
            LocalDateTime to = from.plusDays(d);
            List<SecretarySchedule> schedules = scheduleRepository.findUpcoming(DEFAULT_USER, from, to);
            if (schedules.isEmpty()) {
                return "未来 " + d + " 天内没有日程";
            }
            StringBuilder sb = new StringBuilder("未来 " + d + " 天的日程（" + schedules.size() + " 条）:\n\n");
            for (SecretarySchedule s : schedules) {
                sb.append("[").append(s.id()).append("] ").append(s.title()).append("\n");
                sb.append("  时间: ").append(s.startTime().format(FORMATTER));
                if (s.endTime() != null) sb.append(" ~ ").append(s.endTime().format(FORMATTER));
                sb.append("\n");
                if (s.description() != null) sb.append("  描述: ").append(s.description()).append("\n");
                sb.append(s.done() ? "  [已完成]" : "  [待进行]").append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("查询日程失败", e);
            return "查询失败: " + e.getMessage();
        }
    }

    @Tool(name = "schedule_done", description = "标记日程为已完成")
    public String markScheduleDone(
            @ToolParam(value = "id", description = "日程 ID") Long id
    ) {
        try {
            scheduleRepository.markDone(id);
            return "日程 " + id + " 已标记为完成";
        } catch (Exception e) {
            log.error("标记日程失败: id={}", id, e);
            return "操作失败: " + e.getMessage();
        }
    }
}
