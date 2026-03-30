# 文档索引

> 所有项目文档统一存放于此，按用途分类管理。

---

## design/ — 设计文档

| 文件 | 说明 |
|---|---|
| `architecture.md` | 系统架构设计文档 |
| `secretary-design.md` | 个人秘书智能体功能设计文档 |
| `llm-orchestration-platform.drawio` | 架构图（drawio 格式，可用 VS Code drawio 插件打开） |

**新建设计文档**：`docs/design/{feature}-design.md`，参考 `secretary-design.md` 模板。

---

## guides/ — 使用指南

| 文件 | 说明 |
|---|---|
| `prompt-test-guide.md` | Prompt 测试功能使用指南 |
| `prompt-template-management.md` | Prompt 模板管理指南 |
| `content-optimization-guide.md` | 内容优化功能使用指南 |
| `configuration-guide.md` | 配置文件说明 |
| `qdrant-integration-guide.md` | Qdrant 向量存储集成说明 |

**新建指南**：`docs/guides/{feature}-guide.md`

---

## dev/ — 开发记录

| 文件 | 说明 |
|---|---|
| `dev-log.md` | 开发日志（持续追加，每次变更后记录） |
| `dev-log-append.md` | 历史开发日志（旧格式，已归档） |

**记录规范**：
- 每次代码变更后追加 `dev-log.md`
- 格式：`## {日期}` → 任务描述 / 创建的文件 / 修改的文件 / 关键设计决策

---

## sql/ — 数据库脚本

| 文件 | 说明 |
|---|---|
| `init-database.sql` | 数据库初始化脚本（含所有表结构） |
| `init-model-config.sql` | 模型配置初始化数据 |

---

## 文档维护规范

- **新建文档**：按上述分类目录存放，文件名全小写中划线分隔
- **更新文档**：同步更新本索引
- **删除文档**：从目录移除后同步更新本索引
- **设计文档模板**：`~/.claude/skills/new-feature/FEATURE-DESIGN-TEMPLATE.md`
