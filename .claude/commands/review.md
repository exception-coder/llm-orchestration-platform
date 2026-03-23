请对以下代码或文件进行代码审查：$ARGUMENTS

审查要点（按项目规范）：
1. 是否遵循 DDD-lite 分层：API → Application → Domain ← Infrastructure
2. 是否使用构造函数注入（禁止 @Autowired 字段注入）
3. 是否使用 jakarta.* 命名空间（禁止 javax.*）
4. Controller 是否包含业务逻辑（不应包含）
5. 方法长度是否超过 50 行
6. 是否存在示例类（禁止 XxxExample/Demo/Sample）
7. 异常处理是否正确（不吞异常）
8. 日志是否使用 SLF4J（禁止 System.out.println）
