# Qdrant 向量数据库部署指南

## 快速启动

### 1. 配置 API Key

```bash
# 复制环境变量配置文件
cp env.example .env

# 生成强密钥
openssl rand -base64 32

# 编辑 .env 文件，设置 QDRANT_API_KEY
# QDRANT_API_KEY=生成的密钥
```

### 2. 启动 Qdrant

```bash
cd docker/qdrant
docker-compose up -d
```

### 3. 验证服务

```bash
# 检查容器状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 访问 Web UI（需要 API Key）
# 浏览器打开: http://localhost:6333/dashboard
# Header: api-key: your-api-key
```

### 4. 停止服务

```bash
docker-compose down

# 停止并删除数据卷
docker-compose down -v
```

## 配置说明

### API Key 认证

**预生产环境已启用 API Key 认证**，所有请求必须携带 API Key。

**配置方式**：
1. 设置环境变量 `QDRANT_API_KEY`
2. 或在 `.env` 文件中配置
3. 或使用默认值（不推荐）

**生成强密钥**：
```bash
# 方式 1: OpenSSL
openssl rand -base64 32

# 方式 2: UUID
uuidgen

# 方式 3: Python
python -c "import secrets; print(secrets.token_urlsafe(32))"
```

### 端口映射

- **6333**: HTTP API 端口（用于 REST API 和 Web UI）
- **6334**: gRPC API 端口（Spring AI 使用此端口）

### 数据持久化

数据存储在 Docker 卷 `qdrant_storage` 中，容器重启后数据不会丢失。

### 环境变量

- `QDRANT__LOG_LEVEL`: 日志级别（DEBUG、INFO、WARN、ERROR）
- `QDRANT__SERVICE__ENABLE_TLS`: 是否启用 TLS（开发环境设为 false）

## 应用配置

### 开发环境 (dev)

```yaml
spring:
  ai:
    vectorstore:
      qdrant:
        host: localhost
        port: 6334
        api-key: ${QDRANT_API_KEY}  # 从环境变量读取
        use-tls: false
        collection-name: job_postings_dev
```

**设置应用环境变量**：
```bash
# Linux/Mac
export QDRANT_API_KEY="your-api-key-here"

# Windows PowerShell
$env:QDRANT_API_KEY="your-api-key-here"

# 或在 application.yml 中直接配置（不推荐）
spring:
  ai:
    vectorstore:
      qdrant:
        api-key: your-api-key-here
```

### 生产环境 (prod)

```yaml
spring:
  ai:
    vectorstore:
      qdrant:
        host: ${QDRANT_HOST}
        port: 6334
        api-key: ${QDRANT_API_KEY}
        use-tls: true
        collection-name: job_postings
```

## 常用操作

### 查看集合列表

```bash
curl -H "api-key: your-api-key-here" http://localhost:6333/collections
```

### 创建集合

```bash
curl -X PUT http://localhost:6333/collections/test_collection \
  -H 'Content-Type: application/json' \
  -H 'api-key: your-api-key-here' \
  -d '{
    "vectors": {
      "size": 1536,
      "distance": "Cosine"
    }
  }'
```

### 查看集合信息

```bash
curl -H "api-key: your-api-key-here" \
  http://localhost:6333/collections/job_postings_dev
```

### 删除集合

```bash
curl -X DELETE \
  -H "api-key: your-api-key-here" \
  http://localhost:6333/collections/test_collection
```

## 性能优化

### 生产环境配置

创建 `docker-compose.prod.yml`:

```yaml
version: '3.8'

services:
  qdrant:
    image: qdrant/qdrant:latest
    container_name: llm-qdrant-prod
    ports:
      - "6334:6334"  # 只暴露 gRPC 端口
    volumes:
      - /data/qdrant:/qdrant/storage  # 使用宿主机路径
    environment:
      - QDRANT__LOG_LEVEL=WARN
      - QDRANT__SERVICE__ENABLE_TLS=true
      - QDRANT__SERVICE__API_KEY=${QDRANT_API_KEY}
    deploy:
      resources:
        limits:
          cpus: '4'
          memory: 8G
        reservations:
          cpus: '2'
          memory: 4G
    restart: always
    networks:
      - llm-network

networks:
  llm-network:
    driver: bridge
```

启动生产环境：

```bash
docker-compose -f docker-compose.prod.yml up -d
```

## 监控

### 查看资源使用

```bash
docker stats llm-qdrant
```

### 查看实时日志

```bash
docker-compose logs -f --tail=100
```

## 备份与恢复

### 备份数据

```bash
# 停止容器
docker-compose stop

# 备份数据卷
docker run --rm -v qdrant_storage:/data -v $(pwd):/backup \
  alpine tar czf /backup/qdrant-backup-$(date +%Y%m%d).tar.gz /data

# 启动容器
docker-compose start
```

### 恢复数据

```bash
# 停止并删除容器
docker-compose down

# 恢复数据
docker run --rm -v qdrant_storage:/data -v $(pwd):/backup \
  alpine tar xzf /backup/qdrant-backup-20260304.tar.gz -C /

# 启动容器
docker-compose up -d
```

## 故障排查

### 容器无法启动

```bash
# 查看详细日志
docker-compose logs qdrant

# 检查端口占用
netstat -ano | findstr "6333"
netstat -ano | findstr "6334"
```

### 连接失败

1. 检查防火墙设置
2. 确认端口映射正确
3. 验证网络配置

### 性能问题

1. 增加内存限制
2. 调整日志级别为 WARN
3. 使用 SSD 存储
4. 启用索引优化

## 参考资料

- [Qdrant 官方文档](https://qdrant.tech/documentation/)
- [Docker Hub - Qdrant](https://hub.docker.com/r/qdrant/qdrant)
- [Spring AI Qdrant 集成](https://docs.spring.io/spring-ai/reference/api/vectordbs/qdrant.html)

