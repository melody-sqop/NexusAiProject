# 🚀 NexusAI Platform — 高并发内容生态平台（后端核心版·AI赋能版）

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Cloud-2023.0.0-blue" alt="Spring Cloud">
  <img src="https://img.shields.io/badge/Java-17-orange" alt="Java 17">
  <img src="https://img.shields.io/badge/Redis-7.0-red" alt="Redis">
  <img src="https://img.shields.io/badge/RocketMQ-5.0-green" alt="RocketMQ">
  <img src="https://img.shields.io/badge/MySQL-8.0-blue" alt="MySQL">
  <img src="https://img.shields.io/badge/ElasticSearch-8.0-yellow" alt="ElasticSearch">
  <img src="https://img.shields.io/badge/ClickHouse-24.0-lightgrey" alt="ClickHouse">
  <img src="https://img.shields.io/badge/License-MIT-green" alt="License">
</p>

<p align="center">
  <b>以高并发点赞系统为核心交互枢纽，构建内容创作-互动传播-审核管控-商业变现-全域运营的后端生态体系</b><br>
  <b>目标 QPS 1万-2万，适配千万级用户规模</b>
</p>

---

## 📋 目录

- [🌟 项目亮点](#-项目亮点)
- [🏗️ 系统架构](#️-系统架构)
- [📦 核心模块](#-核心模块)
- [🛠️ 技术栈](#️-技术栈)
- [🚀 快速开始](#-快速开始)
- [📊 性能指标](#-性能指标)
- [🔒 安全设计](#-安全设计)
- [🗺️ 项目路线图](#️-项目路线图)
- [📁 项目结构](#-项目结构)
- [🤝 贡献指南](#-贡献指南)
- [📄 许可证](#-许可证)
- [👤 作者](#-作者)

---

## 🌟 项目亮点

| 亮点 | 说明 | 价值 |
|------|------|------|
| **🔥 高并发点赞系统** | Redis + Lua 原子操作 + 异步写库 + 本地缓存三级防护 | 单机支撑 1万+ QPS，P99 < 50ms |
| **🤖 AI 智能审核** | GPT-4 / 文心一言 / 通义千问 多模型适配层 + 提示词工程 | 语义级内容理解，误杀率降低 40% |
| **🛡️ 五层防御架构** | L1 规则引擎 → L1.5 兜底策略 → L2 语义缓存 → L2 AI 审核 → L3 样本回流 | 从毫秒级拦截到语义级理解，层层递进 |
| **💰 商业变现体系** | 虚拟礼物、创作者分成、广告系统 | 完整的内容商业闭环 |
| **📈 实时数据运营** | ClickHouse + 实时看板 + 运营配置秒级热更新 | 运营策略零停机生效 |
| **🔐 五级风险决策** | P0 Critical → P4 Info，差异化处置策略 | 精准管控，避免一刀切 |

---

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              🌐 接入层 (Gateway)                              │
│                    Spring Cloud Gateway + Sentinel 限流熔断                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                              📦 业务服务层                                     │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  用户服务    │  │  内容服务    │  │  点赞服务    │  │  审核服务    │        │
│  │  User Svc   │  │ Content Svc │  │  Like Svc   │  │ Review Svc  │        │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  通知服务    │  │  变现服务    │  │  运营服务    │  │  搜索服务    │        │
│  │ Notify Svc  │  │ Monetize Svc│  │   Ops Svc   │  │ Search Svc  │        │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘        │
├─────────────────────────────────────────────────────────────────────────────┤
│                              🧠 AI 适配层                                    │
│              GPT-4 / 文心一言 / 通义千问 统一调用抽象层                      │
│                    提示词模板引擎 + 多模型降级策略                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                              💾 数据层                                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   MySQL     │  │   Redis     │  │  RocketMQ   │  │ ElasticSearch│        │
│  │  分库分表    │  │   集群      │  │   消息队列   │  │   搜索引擎    │        │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘        │
│  ┌─────────────┐  ┌─────────────┐                                           │
│  │ ClickHouse  │  │  MinIO/OSS  │                                           │
│  │  实时分析    │  │   对象存储   │                                           │
│  └─────────────┘  └─────────────┘                                           │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 核心交互链路

```
用户发布内容
    │
    ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  L1 规则引擎 │ → │ L1.5 兜底   │ → │  L2 语义缓存 │ → │  L2 AI审核  │
│ DFA Trie树   │    │ 新用户/采样  │    │ SimHash+Redis│    │ 多模型适配   │
│ 毫秒级拦截    │    │ 强制送审     │    │ 相似复用降本 │    │ 语义级理解   │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
    │                    │                    │                    │
    ▼                    ▼                    ▼                    ▼
  通过/拦截           送AI复核            命中缓存复用          人工复核
                                                            │
                                                            ▼
                                                      ┌─────────────┐
                                                      │ L3 样本回流  │
                                                      │ 结果回流训练  │
                                                      │ 每日增量优化  │
                                                      └─────────────┘
```

---

## 📦 核心模块

### 模块 1：用户管理模块 ✅
- 用户注册/登录/权限体系
- JWT Token + Refresh Token 双令牌机制
- 用户画像与行为标签体系

### 模块 2：高并发点赞系统 ✅
- **核心突破点**：Redis + Lua 原子计数 + 异步批量写库
- 本地缓存（Caffeine）→ Redis 集群 → MySQL 三级降级
- 防刷策略：用户频率限制 + IP 维度风控
- 目标性能：**单机 1万+ QPS，P99 < 50ms**

### 模块 3：内容创作与分发模块 ✅
- 内容发布/编辑/删除全生命周期
- 个性化推荐分发引擎（基于用户画像）
- 内容热度计算与排行榜

### 模块 4：内容审核与质量管控模块 🚧 **进行中**
> **AI 核心赋能模块，项目最大技术亮点**

| 层级 | 组件 | 状态 | 技术要点 |
|------|------|------|----------|
| **L1** | 规则引擎 | ✅ 已完成 | DFA Trie 树敏感词过滤 + 双轨热更新 + 五级风险决策 |
| **L1.5** | 兜底策略 | ✅ 已完成 | 新用户识别（7天内全走AI）+ 随机采样 5% + 用户举报阈值触发 |
| **L2a** | 语义缓存层 | 🚧 进行中 | SimHash 局部敏感哈希 + Redis 分桶存储，相似文本复用降本 60%+ |
| **L2b** | AI 审核层 | ⏳ 待实现 | GPT-4/文心一言/通义千问 多模型适配层 + 提示词工程 |
| **L3** | 样本回流闭环 | ⏳ 待实现 | 人工复核结果自动回流训练模型，每日增量训练 |

**为什么采用五层防御架构？**
- **L1 规则引擎**：毫秒级拦截明确违规（涉政、暴恐），成本最低
- **L1.5 兜底策略**：覆盖规则引擎的漏网之鱼，用"确定性规则"补充"模糊规则"
- **L2 语义缓存**：相似内容复用历史审核结果，降低大模型 API 调用成本 60%+
- **L2 AI 审核**：处理语义级复杂场景（讽刺、隐喻、变体），是系统的"大脑"
- **L3 样本回流**：形成数据闭环，系统越用越聪明，避免模型退化

**不这样做的后果**：单层拦截要么误杀率极高（规则引擎），要么成本爆炸（全量走AI），要么无法持续进化（无回流闭环）。

### 模块 5：互动延伸模块 ⏳
- 评论系统（嵌套回复、点赞、举报）
- 转发/收藏/关注关系链
- @提及与消息推送

### 模块 6：通知中心模块 ⏳
- 站内信/邮件/短信/推送多渠道
- 通知优先级与合并策略
- 用户订阅偏好管理

### 模块 7：商业变现模块 ⏳
- 虚拟礼物系统（打赏、送礼）
- 创作者收益分成结算
- 广告位管理与投放

### 模块 8：运营管理模块 ⏳
- 运营后台（内容管理、用户管理、数据统计）
- 配置中心（审核规则、兜底策略、AI 参数热更新）
- 实时数据看板（ClickHouse 驱动）

---

## 🛠️ 技术栈

### 后端框架
| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.x | 基础框架 |
| Spring Cloud | 2023.0.0 | 微服务治理 |
| Spring Cloud Gateway | 2023.0.0 | API 网关 |
| Spring Cloud Alibaba Nacos | 2023.0.0 | 服务注册/配置中心 |
| Sentinel | 1.8.8 | 限流熔断 |

### 数据存储
| 技术 | 版本 | 用途 |
|------|------|------|
| MySQL | 8.0 | 主数据库，分库分表 |
| Redis | 7.0 | 缓存、计数、分布式锁 |
| ElasticSearch | 8.0 | 全文搜索 |
| ClickHouse | 24.x | 实时数据分析 |
| RocketMQ | 5.0 | 异步消息、削峰填谷 |

### AI 与算法
| 技术 | 用途 |
|------|------|
| OpenAI GPT-4 API | 语义审核主模型 |
| 百度文心一言 API | 国产模型备选/合规 |
| 阿里通义千问 API | 多模型降级策略 |
| SimHash | 文本相似度计算 |
| DFA (Deterministic Finite Automaton) | 敏感词匹配 |

### 工具与运维
| 技术 | 用途 |
|------|------|
| Docker | 容器化部署 |
| Maven | 构建工具 |
| Swagger/OpenAPI | API 文档 |
| Micrometer + Prometheus | 监控埋点 |

---

## 🚀 快速开始

### 环境要求

- **JDK**: 17+
- **Maven**: 3.9+
- **MySQL**: 8.0+
- **Redis**: 7.0+ (Cluster 模式)
- **RocketMQ**: 5.0+
- **ElasticSearch**: 8.0+
- **ClickHouse**: 24.x+

### 1. 克隆项目

```bash
git clone https://github.com/melody-sqop/NexusAiProject.git
cd NexusAiProject
```

### 2. 初始化数据库

```bash
# 执行 SQL 初始化脚本
mysql -u root -p < sql/init.sql
```

### 3. 配置环境

```bash
# 复制配置文件模板
cp application.yml.example application.yml

# 编辑 application.yml，填写你的：
# - 数据库连接信息
# - Redis 集群地址
# - RocketMQ NameServer 地址
# - AI API Keys（GPT-4 / 文心一言 / 通义千问）
# - 其他中间件配置
```

> ⚠️ **安全提示**：`application.yml` 已加入 `.gitignore`，请勿将真实配置（尤其是 API Key）提交到仓库！

### 4. 编译运行

```bash
# 编译
mvn clean package -DskipTests

# 启动网关服务
cd nexus-gateway
mvn spring-boot:run

# 启动核心服务（以审核服务为例）
cd nexus-review-service
mvn spring-boot:run
```

### 5. 验证服务

```bash
# 查看网关路由
curl http://localhost:8080/actuator/gateway/routes

# 测试点赞接口
curl -X POST http://localhost:8080/api/v1/like   -H "Content-Type: application/json"   -d '{"userId": 1, "contentId": 1001, "type": "POST"}'

# 查看 API 文档
open http://localhost:8080/swagger-ui.html
```

---

## 📊 性能指标

### 目标 vs 当前

| 指标 | 目标 | 当前状态 | 测试方法 |
|------|------|----------|----------|
| 点赞 QPS | 10,000+ | 🚧 压测中 | JMeter 1000 并发 |
| 点赞 P99 延迟 | < 50ms | 🚧 压测中 | Micrometer 埋点 |
| 审核吞吐量 | 500 TPS | 🚧 压测中 | 模拟内容发布 |
| AI 调用成本降低 | 60%+ | 🚧 验证中 | SimHash 缓存命中率 |
| 系统可用性 | 99.9% | ⏳ 待压测 | 混沌工程 |

### 压测报告
> 📁 详见 `docs/benchmark/` 目录（持续更新）

---

## 🔒 安全设计

### 敏感信息保护
- ✅ `.gitignore` 已配置：排除 `application.yml`、IDE 配置、编译产物
- ✅ API Keys 通过环境变量注入，不硬编码
- ✅ 数据库密码使用加密配置（Jasypt）

### 内容安全
- ✅ 五级风险决策体系（P0 Critical → P4 Info）
- ✅ P0 涉政内容永不缓存，强制重审
- ✅ 用户举报触发二次审核（3次阈值自动隐藏）

### 系统安全
- ✅ Sentinel 限流熔断，防止服务雪崩
- ✅ JWT Token + 接口签名校验
- ✅ SQL 注入防护（MyBatis 参数化查询）

---

## 🗺️ 项目路线图

### 第一阶段：基础后端 + 简单点赞 ✅
- [x] 项目脚手架搭建（Spring Cloud 微服务）
- [x] 用户管理模块
- [x] 基础点赞功能（同步写库）

### 第二阶段：高并发点赞 + AI 审核基础 ✅
- [x] Redis + Lua 原子计数
- [x] 异步批量写库（RocketMQ）
- [x] L1 规则引擎（DFA Trie 树）
- [x] L1.5 兜底策略（新用户 + 随机采样）

### 第三阶段：AI 提示词优化 + 审核闭环 🚧 **当前**
- [x] L1 规则引擎双轨热更新
- [x] L1.5 用户举报兜底架构
- [x] L2a SimHash 语义缓存层（进行中）
- [x] L2b AI 审核多模型适配层
- [x] 提示词工程优化

### 第四阶段：生态完善 + 工程化落地 ⏳
- [ ] L3 样本回流闭环（人工复核 → 模型训练）
- [ ] 内容创作与分发模块
- [ ] 评论与互动系统
- [ ] 通知中心
- [ ] 商业变现体系
- [ ] 运营后台 + 实时看板
- [ ] Docker 容器化部署
- [ ] CI/CD 流水线
- [ ] 混沌工程测试

---

## 📁 项目结构

```
NexusAiProject/
├── 📄 README.md                    # 项目说明（本文件）
├── 📄 LICENSE                      # MIT 许可证
├── 📄 .gitignore                   # Git 忽略配置
├── 📄 pom.xml                      # 父工程 Maven 配置
│
├── 🗂️ nexus-gateway/               # API 网关服务
│   ├── src/
│   └── pom.xml
│
├── 🗂️ nexus-user-service/          # 用户管理服务
│   ├── src/
│   └── pom.xml
│
├── 🗂️ nexus-content-service/       # 内容创作与分发服务
│   ├── src/
│   └── pom.xml
│
├── 🗂️ nexus-like-service/          # 高并发点赞服务 ⭐ 核心
│   ├── src/
│   └── pom.xml
│
├── 🗂️ nexus-review-service/       # 内容审核与质量管控服务 ⭐ AI核心
│   ├── src/
│   │   └── main/
│   │       └── java/com/nexus/review/
│   │           ├── controller/     # API 接口层
│   │           ├── service/      # 业务逻辑层
│   │           │   ├── impl/
│   │           │   │   └── CommentServiceImpl.java  # 评论审核三分支
│   │           ├── infrastructure/ # 基础设施层
│   │           │   ├── aho_corasick/   # AC 自动机（L1 规则引擎）
│   │           │   ├── simhash/        # SimHash 语义缓存（L2a）
│   │           │   ├── ai_adapter/     # AI 模型适配层（L2b）
│   │           │   └── cache/          # Redis 缓存封装
│   │           ├── domain/         # 领域模型
│   │           └── config/         # 配置类
│   └── pom.xml
│
├── 🗂️ nexus-notify-service/        # 通知中心服务
│   ├── src/
│   └── pom.xml
│
├── 🗂️ nexus-monetize-service/      # 商业变现服务
│   ├── src/
│   └── pom.xml
│
├── 🗂️ nexus-ops-service/           # 运营管理服务
│   ├── src/
│   └── pom.xml
│
├── 🗂️ nexus-common/               # 公共模块（工具类、常量、异常）
│   ├── src/
│   └── pom.xml
│
├── 🗂️ sql/                         # 数据库初始化脚本
│   └── init.sql
│
├── 🗂️ docs/                        # 项目文档
│   ├── architecture/               # 架构图
│   ├── benchmark/                  # 压测报告
│   └── interview/                  # 面试话术与知识点
│
└── 🗂️ scripts/                     # 部署脚本
    ├── docker/
    └── k8s/
```

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. **Fork** 本仓库
2. 创建你的 Feature 分支：`git checkout -b feature/AmazingFeature`
3. 提交变更：`git commit -m 'Add some AmazingFeature'`
4. 推送到分支：`git push origin feature/AmazingFeature`
5. 创建 **Pull Request**

### 代码规范
- 遵循阿里巴巴 Java 开发手册
- 所有新功能必须包含单元测试
- API 变更需同步更新 Swagger 注解

---

## 👤 作者

**melody-sqop**

- GitHub: [@melody-sqop](https://github.com/melody-sqop)
- 项目地址: [https://github.com/melody-sqop/NexusAiProject](https://github.com/melody-sqop/NexusAiProject)

---

<p align="center">
  ⭐ 如果这个项目对你有帮助，请点个 Star 支持一下！
</p>
