# MasterArt · 美术馆在线展览平台

> 一个基于 **Spring Boot 3 + Vue 3** 的艺术品展览平台，内置**受控 AI 智能策展 Agent**（Spring AI + DeepSeek）：
> 支持艺术品/展览/艺术家浏览、用户体系与社区，以及「查询 → 预约草稿 → 人工确认 → 真实预约」的完整智能服务闭环。

---

## 目录

- [项目简介](#项目简介)
- [功能特性](#功能特性)
- [AI 智能策展（核心模块）](#ai-智能策展核心模块)
- [技术栈](#技术栈)
- [安全设计（面试重点）](#安全设计面试重点)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [接口速览](#接口速览)
- [测试](#测试)
- [数据库表](#数据库表)
- [常见问题](#常见问题)
- [相关文档](#相关文档)

---

## 项目简介

MasterArt 是一个面向美术馆场景的在线展览平台。除了常规的展览/艺术品/艺术家浏览与社区功能外，
本项目实现了一个**受控的 Agent 编排层**：大模型（DeepSeek）通过 5 个白名单工具访问后端能力，
但**没有权限直接完成预约、锁座或扣款**——它只能生成预约草稿，真实预约必须由用户在网页上人工确认后，
由前端直接调用真实预约接口完成。

```
用户提问 ──► AI 策展助手（DeepSeek / Spring AI 原生 ToolCalling）
               │  只读工具：检索作品 / 检索知识库(RAG) / 查询排期余票
               │  受限写工具：生成预约草稿（幂等）/ 新展订阅（幂等）
               ▼
        预约草稿（PENDING，30 分钟有效，不占库存）
               ▼
        用户在网页上点击「确认预约」
               ▼
        POST /api/reservations/confirm（真实写入 + 库存原子扣减，Agent 不可达）
```

---

## 功能特性

### 业务功能
- **展览**：列表/详情、状态（未开始/进行中/已结束）、排期与余票、新展订阅
- **艺术品**：列表/搜索/筛选、详情（多图、3D 模型、音频讲解）、收藏、浏览历史
- **艺术家**：列表/详情、按流派/国籍/时期筛选、代表作
- **用户体系**：注册登录、个人中心、计划参观、我的预约
- **社区与后台**：艺术社区发帖、管理员后台（展品/展览/用户管理）、艺术家中心

### AI 智能策展（`/ai-art-guide`）
- 多轮对话 + **SSE 流式打字机**输出，按用户隔离对话记忆，支持「新对话」清空
- **5 个受控工具**：`searchArtworkKnowledge`（RAG 检索）、`searchArtworks`（多条件检索）、
  `queryExhibition`（排期/余票）、`createReservationDraft`（预约草稿）、`createSubscription`（新展订阅）
- **人工确认闸门**：草稿卡片内一键确认，真实预约由前端直调预约接口完成
- **幂等控制**：哈希幂等键 + 数据库唯一约束双层防护，模型重试不会重复占座
- **步数限制**：单轮最多 6 次工具调用，超限自动收敛
- **降级策略**：RAG 超时/失败、工具异常、模型异常均返回友好提示，不编造数据
- **首页数据看板**：在展艺术品 / 展览总数 / 进行中展览 / 入驻艺术家（滚动计数动画）
- **AI 入口桥接**：作品详情「让 AI 讲讲这幅画」、艺术家详情「让 AI 介绍这位艺术家」、
  展览场次「让 AI 生成预约草稿」均自动跳转并提问

---

## AI 智能策展（核心模块）

### Agent 编排层（`com.pan.master_art.agent`）

| 组件 | 说明 |
|---|---|
| `AgentPrompts` | 系统提示词：安全边界、工具说明、幂等规则、步数约束的唯一权威声明 |
| `CuratorAgentService` | 编排入口：ChatClient 原生 ToolCalling 循环、对话记忆、SSE 流式、安全兜底 |
| `AgentConfig` | 装配 ChatClient、工具白名单（统一包裹步数守卫）、聊天记忆 |
| `StepLimitedToolCallback` | 步数守卫装饰器：单轮 >6 步返回「停止调用」指令，工具本体不再执行 |
| `AgentTurnState` | 单轮状态：会话用户ID、草稿ID、工具轨迹、步数计数（经 ToolContext 传递） |
| `rag/DatabaseKnowledgeRetriever` | 轻量 RAG：分词召回 + 加权打分 + 超时控制，可替换为向量库实现 |
| `tool/*` | 5 个 `@Tool` 工具（只读 ×3 + 受限写 ×2） |

### 多步调用示例

```
用户：帮我预约《印象派大师展》明天上午10点场次 2 张票

AI：[工具1] queryExhibition(exhibitionId=1)      → 排期、余票
    [工具2] createReservationDraft(sessionId=1, ticketCount=2, idempotencyKey=auto)
    → 预约草稿 #1（PENDING，¥176.00，30 分钟有效）
AI：草稿已生成，请到网页上确认（我无法直接出票或扣款）
用户：点击卡片「确认预约」
    → POST /api/reservations/confirm → 真实预约 + 库存 38 → 40
```

---

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot **3.4.5**、Java **17**、Spring Data JPA（Hibernate 6）、MySQL 8 |
| AI | **Spring AI 1.0.0**（`spring-ai-starter-model-openai`）+ DeepSeek（OpenAI 兼容协议） |
| 前端 | Vue **3**、Vite 5、Element Plus、Pinia、Vue Router、Axios、ECharts |
| 测试 | JUnit 5、Mockito、H2（内存库）、真实 `OpenAiChatModel` + 脚本化 OpenAI 端点 |

---

## 安全设计（面试重点）

| 防线 | 实现 |
|---|---|
| **人工确认闸门** | Agent 只能写 `reservation_drafts`；真实预约只能由用户点击后经 `POST /api/reservations/confirm` 触发 |
| **工具白名单** | 只有 5 个 `@Tool`；确认/扣库存等真实写入方法**永不注册为工具**，`AgentToolWhitelistTest` 反射守护 |
| **幂等双层** | 应用层：`SHA-256(userId\|exhibitionId\|sessionId\|ticketCount)` 先查后建；数据库层：`uk_draft_idempotency` 唯一约束兜底 |
| **步数限制** | `StepLimitedToolCallback` 装饰全部工具，单轮上限 6 步 |
| **身份注入** | 写工具的 `userId` **仅由服务端从会话注入**，模型无法伪造/指定他人身份 |
| **库存原子扣减** | `UPDATE ... WHERE total_tickets - sold_tickets >= n` 条件更新，天然防超卖；仅确认接口可触发 |
| **安全兜底** | 模型调用白名单外工具 / 服务异常 → 收敛为有界拒绝话术，零真实写入 |
| **RAG 降级** | 检索超时（默认 2500ms）返回友好提示，禁止模型编造；分词召回避免整句 LIKE 召回失败 |

> 详细架构、时序与设计取舍见 [AGENT_MODULE.md](AGENT_MODULE.md)。

---

## 项目结构

```
Master_Art
├── src/main/java/com/pan/master_art
│   ├── agent/                    # ★ AI 策展 Agent 编排层
│   │   ├── AgentPrompts.java
│   │   ├── config/AgentConfig.java
│   │   ├── controller/AgentController.java
│   │   ├── orchestration/        # CuratorAgentService / StepLimitedToolCallback / AgentTurnState
│   │   ├── rag/                  # 知识检索（分词召回 + 超时降级）
│   │   └── tool/                 # 5 个受控工具
│   ├── controller/               # 业务接口（展览/作品/艺术家/预约/订阅/统计…）
│   ├── entity/                   # 实体（含草稿、预约、票务场次、订阅）
│   ├── repository/               # 数据访问
│   ├── service/                  # 业务服务（草稿/预约/订阅/票务/知识检索调用）
│   ├── dto/ · exception/ · config/ · utils/
│   └── MasterArtApplication.java
├── src/main/resources/application.properties
├── src/test/java                 # 23 项测试（编排/白名单/幂等/真库诊断/live）
├── art-gallery-frontend/         # ★ Vue 3 前端
│   └── src/
│       ├── api/                  # agent.js / reservation.js 等
│       ├── views/                # 页面（AIArtGuide、MyReservations、Home…）
│       ├── stores/ · router/ · locales/
│       └── style.css             # 美术馆主题基座（奶油底/墨绿/金色）
├── trial-data.sql                # 试运行种子数据（幂等可重复执行）
├── AGENT_MODULE.md               # Agent 模块详细设计文档
└── pom.xml
```

---

## 快速开始

### 前置要求
- **JDK 17**（Spring Boot 3 要求）
- **Maven 3.8+**
- **MySQL 8**（本地 3306，默认账号 `root/123456`，可在配置中修改）
- **Node 18+**（前端）
- 可用的 **DeepSeek API Key**（通过环境变量 `AI_API_KEY` 注入，仓库中不含任何真实 Key）

### 1. 创建数据库
```sql
CREATE DATABASE IF NOT EXISTS master_art DEFAULT CHARACTER SET utf8mb4;
```
> 表结构由 `spring.jpa.hibernate.ddl-auto=update` 在启动时自动创建/更新，无需手工建表。

### 2. 导入试运行数据（强烈建议）
```bash
mysql -uroot -p123456 --default-character-set=utf8mb4 < trial-data.sql
```
包含：5 位启用艺术家、5 件上架作品（含 RAG 故事/描述）、5 个展览（进行中/即将开始）、
7 个票务场次，以及试运行账号 **`demo / 123456`**。脚本幂等，可重复执行。

### 3. 启动后端
```bash
mvn spring-boot:run
```
启动后访问 `http://localhost:8080/api/stats/overview` 应返回统计数据。

### 4. 启动前端
```bash
cd art-gallery-frontend
npm install
npm run dev
```

### 5. 体验
浏览器打开 **http://localhost:5173**，使用 **`demo / 123456`** 登录：

1. 首页 → 数据看板、精选展览/作品/艺术家
2. 展览详情 → 排期与余票 → 「让 AI 生成预约草稿」
3. **AI 智能策展**（`/ai-art-guide`）→ 试问：
   - 「最近有哪些展览？余票怎么样？」
   - 「给我讲讲《星空》的创作故事」
   - 「帮我预约《印象派大师展》明天上午10点场次 2 张票」
4. 在对话草稿卡片或「我的预约」页点击「确认预约」→ 完成真实预约
5. 「我的预约」页可查看/取消预约、退订新展提醒

---

## 配置说明

`src/main/resources/application.properties`：

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/master_art...` | 数据库连接（可用 `DB_URL` 覆盖） |
| `spring.datasource.username/password` | `${DB_USERNAME:root} / ${DB_PASSWORD:123456}` | 数据库账号（环境变量覆盖） |
| `spring.jpa.hibernate.ddl-auto` | `update` | 自动建表/更新 |
| `spring.ai.openai.api-key` | `${AI_API_KEY:not-configured}` | **必须通过环境变量 `AI_API_KEY` 提供**，仓库不含真实 Key |
| `spring.ai.openai.base-url` | `https://api.deepseek.com` | OpenAI 兼容端点 |
| `spring.ai.openai.chat.options.model` | `deepseek-chat` | 模型（可用 `AI_MODEL` 覆盖） |
| `agent.max-tool-steps` | `6` | 单轮最大工具调用步数 |
| `agent.draft-ttl-minutes` | `30` | 预约草稿有效期 |
| `agent.rag.timeout-ms` | `2500` | RAG 检索超时（毫秒） |
| `agent.rag.top-k` | `5` | RAG 返回条数 |
| `agent.chat-memory.max-messages` | `20` | 对话记忆窗口 |

### 🔑 API Key 安全（不要提交到仓库）

仓库中**不含任何真实 Key**。请通过环境变量注入（任选其一）：

```bash
# Windows PowerShell（当前会话）
$env:AI_API_KEY="sk-xxxx"; mvn spring-boot:run

# Windows PowerShell（永久写入用户环境变量，重开终端生效）
setx AI_API_KEY "sk-xxxx"

# Linux / macOS
AI_API_KEY=sk-xxxx mvn spring-boot:run
```

**IntelliJ IDEA**：`Run → Edit Configurations → Environment variables` 增加 `AI_API_KEY=sk-xxxx`。

未配置 `AI_API_KEY` 时应用仍可正常启动（浏览/管理功能可用），AI 接口会返回友好降级提示。

> ⚠️ 如果您曾在旧提交中提交过真实 Key，请立即到 DeepSeek 控制台**吊销并重新生成** Key。
> 上传 GitHub 前可执行以下自检：
> ```bash
> git grep -n "sk-" -- . ':!*.md'        # 工作区检查
> git log --all -S "sk-" --oneline       # 历史检查
> ```

---

## 接口速览

### AI 策展 Agent
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/agent/chat` | 非流式对话（返回内容 + 草稿卡片 + 工具轨迹） |
| POST | `/api/agent/chat/stream` | SSE 流式（`token` / `done` / `error` 事件） |
| POST | `/api/agent/chat/clear` | 清空当前用户对话记忆（新对话） |

### 预约 / 订阅 / 票务
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/reservations/confirm` | **人工确认草稿**（真实预约 + 库存扣减，Agent 不可达） |
| GET | `/api/reservations/drafts/my?userId=` | 我的草稿列表 |
| GET | `/api/reservations/my?userId=` | 我的预约列表 |
| POST | `/api/reservations/{id}/cancel` | 取消预约（票额回补） |
| GET | `/api/ticket-sessions/exhibition/{id}` | 展览排期与余票（只读） |
| POST | `/api/subscriptions` | 订阅新展提醒 |
| POST | `/api/subscriptions/cancel` | 取消订阅 |
| GET | `/api/subscriptions/my?userId=` | 我的订阅 |

### 基础业务
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/stats/overview` | 首页数据看板 |
| GET | `/api/exhibitions/get-list` | 展览列表 |
| GET | `/api/exhibitions/{id}` | 展览详情 |
| GET | `/api/artworks/getAllArtworks` | 作品列表（分页/筛选） |
| GET | `/api/artworks/{id}` | 作品详情 |
| GET | `/api/artists/getAllArtists` | 艺术家列表 |
| POST | `/api/users/login` · `/api/users/register` | 登录 / 注册 |

---

## 测试

```bash
# 全量单元/集成测试（H2 内存库，无需 MySQL / 外网）
mvn test
```

覆盖 23 项，核心用例：

| 测试类 | 覆盖场景 |
|---|---|
| `AgentOrchestrationTest` | 多步编排、越权拦截、RAG 超时降级、步数限制、幂等键/重放/非法键/未登录/余票不足/草稿过期 |
| `AgentToolWhitelistTest` | 工具白名单精确匹配、写语义工具禁止、真实写入路径无 `@Tool`、步数守卫全覆盖 |
| `ReservationDraftIdempotencyTest` | H2 真库验证唯一约束与服务层幂等重放 |
| `SubscriptionViewTest` | 订阅视图事务内组装（懒加载安全） |
| `LiveDeepSeekSmokeTest` | 真实 DeepSeek 聊天 + 原生函数调用（可选） |
| `RagMysqlDebugTest` | 真库 RAG 召回诊断（可选） |

可选：真实模型 / 真库验证
```bash
# 真实 DeepSeek 连通与函数调用
mvn test -Dtest=LiveDeepSeekSmokeTest "-Dagent.live=true" "-Dai.api.key=sk-xxx"

# 真实 MySQL 上的 RAG 召回诊断
mvn test -Dtest=RagMysqlDebugTest "-Ddb.live=true" \
  "-Dspring.datasource.url=jdbc:mysql://localhost:3306/master_art?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai" \
  "-Dspring.datasource.username=root" "-Dspring.datasource.password=123456" \
  "-Dspring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver" "-Dspring.jpa.hibernate.ddl-auto=none"
```

前端构建：
```bash
cd art-gallery-frontend && npm run build
```

---

## 数据库表

| 表 | 说明 | 关键约束 |
|---|---|---|
| `artists` / `artworks` / `exhibitions` / `exhibition_artworks` | 艺术家、艺术品、展览及关联 | 作品关联艺术家；展览带状态与时间 |
| `ticket_sessions` | 展览场次排期与票池 | `total_tickets` / `sold_tickets` / `price` |
| `reservation_drafts` | AI 生成的预约草稿 | `uk_draft_idempotency(idempotency_key)` 唯一 |
| `reservations` | 人工确认后的真实预约 | `draft_id` 唯一（草稿↔预约一对一） |
| `exhibition_subscriptions` | 新展订阅 | `(user_id, exhibition_id)` 唯一 |
| `users` / `post` / `comments` / `user_favorites` / `user_uploads` 等 | 用户与社区 | — |

库存扣减唯一入口（防超卖）：
```sql
UPDATE ticket_sessions SET sold_tickets = sold_tickets + :n
WHERE id = :sessionId AND status = 1 AND total_tickets - sold_tickets >= :n
```

---

## 常见问题

**Q：启动报 8080 端口被占用？**
A：多为上一次启动的 Java 进程残留。`netstat -ano | findstr :8080` 找到 PID 后结束进程，再启动。

**Q：启动报数据库连接失败 / 表不存在？**
A：确认 MySQL 已启动、`master_art` 库已创建、账号密码与 `application.properties` 一致；
`ddl-auto=update` 会在首次启动时自动建表。中文数据请确保库/表字符集为 `utf8mb4`。

**Q：AI 回复"未登录，无法生成草稿"？**
A：预约/订阅工具要求登录态。请用 `demo / 123456` 登录后再对话；未登录可正常浏览与只读问答。

**Q：AI 调用返回 401 / 连接失败？**
A：检查 `spring.ai.openai.api-key` 是否有效（或设置 `AI_API_KEY` 环境变量），并确认网络可访问
`https://api.deepseek.com`。

**Q：流式对话在浏览器中没有逐字输出？**
A：需使用支持 SSE 的现代浏览器；接口为 `text/event-stream`，若被反向代理缓存请关闭缓冲。

**Q：想换模型（如 `deepseek-reasoner`）？**
A：设置环境变量 `AI_MODEL=deepseek-reasoner`，或修改 `spring.ai.openai.chat.options.model`。

---

## 相关文档

- [AGENT_MODULE.md](AGENT_MODULE.md) —— AI 策展 Agent 架构、安全设计、接口与测试详解
- [trial-data.sql](trial-data.sql) —— 试运行种子数据
- `面试准备/` —— 项目面试讲解手册与速答卡

---

> 本项目用于学习与毕业设计/面试展示。API Key 请勿提交到公开仓库，建议使用环境变量注入。
