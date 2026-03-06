# Docker 部署文件

本目录包含项目所需的各种服务的 Docker Compose 配置文件。

## 目录结构

```
docker/
├── qdrant/              # Qdrant 向量数据库
│   ├── docker-compose.yml
│   └── README.md
├── mysql/               # MySQL 数据库（待添加）
├── redis/               # Redis 缓存（待添加）
└── README.md            # 本文件
```

## 快速启动

### 启动所有服务

```bash
# 从项目根目录执行
cd docker/qdrant && docker-compose up -d
```

### 停止所有服务

```bash
cd docker/qdrant && docker-compose down
```

## 服务列表

### Qdrant 向量数据库

- **目录**: `docker/qdrant/`
- **端口**: 6333 (HTTP), 6334 (gRPC)
- **用途**: 向量存储和相似度检索
- **文档**: [docker/qdrant/README.md](qdrant/README.md)

## 开发环境配置

所有服务默认配置为开发环境，特点：
- 不启用 TLS
- 使用默认密码
- 暴露所有端口
- 启用详细日志

## 生产环境配置

生产环境需要：
1. 启用 TLS
2. 使用强密码
3. 限制端口暴露
4. 配置资源限制
5. 启用监控和日志

详见各服务目录下的 `docker-compose.prod.yml` 文件。

## 注意事项

1. **数据持久化**: 所有服务使用 Docker 卷持久化数据
2. **网络隔离**: 服务运行在独立的 Docker 网络中
3. **资源限制**: 生产环境需配置 CPU 和内存限制
4. **安全性**: 生产环境必须修改默认密码和启用认证

## 常用命令

```bash
# 查看所有容器状态
docker ps

# 查看容器日志
docker logs -f <container_name>

# 进入容器
docker exec -it <container_name> /bin/bash

# 查看资源使用
docker stats

# 清理未使用的资源
docker system prune -a
```

