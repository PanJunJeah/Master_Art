# MasterArt 智能策展 Agent 编排层（Agent Orchestration Module）

> 基于 Spring Boot 3.4 + Spring AI 1.0 + DeepSeek（OpenAI 兼容协议）实现的**受控 Agent 编排层**。
> 本模块在原「艺术品展览平台」之上新增：智能策展对话、展览排期余票查询、预约草稿（人工确认闸门）、新展订阅。

---

## 1. 技术栈与工程升级

| 项目 | 变更 |
|---|---|
| Java | 8 → **17**（IDE 与 Maven 已使用 JDK 17） |
| Spring Boot | 2.6.13 → **3.4.5**（javax → jakarta 迁移） |
| Spring AI | **1.0.0**（`spring-ai-bom` + `spring-ai-starter-model-openai`） |
| 数据库 | MySQL 8（新增 4 张表），测试用 H2 内存库 |
| 前端 | Vue3 + Element Plus：AI 智能策展页重写（SSE 流式 + 草稿确认卡片） |

DeepSeek 提供 OpenAI 兼容协议，直接复用 Spring AI 的 OpenAI 客户端：
```properties
spring.ai.openai.base-url=https://api.deepseek.com
spring.ai.openai.chat.options.model=deepseek-chat
```

---

## 2. 模块结构

```
src/main/java/com/pan/master_art/
├── agent/                                # ===== Agent 编排层 =====
│   ├── AgentPrompts.java                 # 系统提示词（安全边界唯一权威声明）
│   ├── config/AgentConfig.java           # ChatClient / 工具白名单 / 步数守卫装配
│   ├── controller/AgentController.java   # /api/agent/chat（JSON）与 /chat/stream（SSE）
│   ├── dto/AgentChatRequest/Response     # 请求与结构化响应（含草稿卡片、工具轨迹）
│   ├── orchestration/
│   │   ├── CuratorAgentService.java      # 编排入口（原生 ToolCalling 循环 + 安全兜底）
│   │   ├── AgentTurnState.java           # 单轮状态（userId/草稿ID/工具轨迹/步数）
│   │   └── StepLimitedToolCallback.java  # 步数限制守卫（ToolCallback 装饰器）
│   ├── rag/                              # 轻量 RAG（可替换为向量库实现）
│   └── tool/                             # 5 个 @Tool 工具（唯一后端能力出口）
├── entity/                               # 新增 TicketSession / ReservationDraft /
│                                         #       Reservation / ExhibitionSubscription
├── repository/                           # 对应仓库（含 tryOccupyTickets 原子扣减）
├── service/
│   ├── ReservationDraftService.java      # 幂等草稿（先查后建 + 唯一约束兜底）
│   ├── ReservationService.java           # 真实预约（人工确认后，Agent 永不可达）
│   ├── TicketSessionService.java         # 排期余票（只读）
│   └── SubscriptionService.java          # 订阅（幂等）
└── controller/ReservationController 等   # 前端直调的真实写入接口
```

---

## 3. 工具层（Spring AI @Tool）

模型可见的工具**恰好 5 个**（白名单精确匹配，见 `AgentToolWhitelistTest`）：

| 工具 | 类型 | 说明 |
|---|---|---|
| `searchArtworkKnowledge` | 只读 | RAG 检索艺术品背景知识（超时降级为友好提示） |
| `searchArtworks` | 只读 | 名称/流派/艺术家/类型/年代多条件检索 |
| `queryExhibition` | 只读 | 展览排期、场次与余票 |
| `createReservationDraft` | 受限写 | **仅生成预约草稿**，强制幂等键 |
| `createSubscription` | 受限写 | 新展订阅提醒（幂等合并） |

**严禁暴露**：`ReservationService.confirm`、`TicketSessionRepository.tryOccupyTickets` 等
真实"扣减库存/锁座/真实下单"接口永不注册为工具——模型没有任何路径触达，
`AgentToolWhitelistTest` 用反射 + 名称扫描自动守护这条边界。

---

## 4. 核心安全与边界控制（面试重点）

### 4.1 人工确认闸门（Human-in-the-loop）
```
用户提问 → Agent 调用 createReservationDraft（仅写 reservation_drafts）
        → 前端渲染草稿卡片（状态 PENDING，有效期 30 分钟）
        → 用户点击「确认预约」
        → Vue3 直接调用 POST /api/reservations/confirm（真实预约接口）
        → ReservationService.confirm：状态守卫 + 原子扣减库存 + 生成真实预约
```
- 系统提示词显式声明：**“你没有权限直接完成预约、锁座或扣款，只能生成预约草稿”**；
- 用户要求“直接帮我订/马上扣款”时，模型必须拒绝并引导生成草稿（测试场景 b 验证）；
- 草稿不占库存，库存只在人工确认时原子扣减（条件 UPDATE 防超卖）。

### 4.2 幂等控制（双层防护）
- **应用层**：`idempotencyKey = SHA-256(userId|exhibitionId|sessionId|ticketCount)`；
  工具强制要求传入幂等键（可传 `auto` 由服务端生成规范键并回传，模型重试原样复用）；
  同一意图先查后建，命中即重放，绝不重复占座。
- **数据库层**：`reservation_drafts.idempotency_key` 唯一约束 `uk_draft_idempotency` 兜底，
  并发重试触发 `DataIntegrityViolationException` 时降级为读取已有草稿。
- 非法幂等键被拒绝；草稿→真实预约一对一（`draft_id` 唯一 + 状态守卫），确认接口本身幂等。

### 4.3 步数限制（防死循环）
- `StepLimitedToolCallback` 装饰器包裹全部工具，单轮工具执行步数上限默认 **6**（`agent.max-tool-steps`）；
- 循环本身由 Spring AI 原生 ToolCalling 机制驱动（`ChatClient` → `OpenAiChatModel` 内部循环 →
  `DefaultToolCallingManager` 原生工具执行），**不手写 JSON 解析循环**；
- 超限后守卫直接返回“停止调用”指令，工具本体不再执行（测试场景 d 验证：8 次尝试仅 6 次真实执行）。

### 4.4 其他防线
- **身份由服务端注入**：`createReservationDraft` / `createSubscription` 的**工具签名不接受 userId 参数**，
  userId 只从会话上下文（ToolContext）注入——模型无法伪造他人身份（试运行实测发现模型无法看到 ToolContext，
  由模型传 userId 会被误拦截，因此改为注入式设计，安全性与可用性同时提升）；
- **安全兜底**：模型尝试调用白名单外工具时，编排层将异常收敛为有界的拒绝话术（零真实写入）；
- **RAG 超时降级 + 分词召回**：整句提问不是任何字段的连续子串，检索器按词元（全词 + 中文二元组）逐个召回合并去重；
  检索超时（`agent.rag.timeout-ms`）返回友好提示，模型不得编造数据；空结果兜底走 fetch-join 查询（避免懒加载异常）；
- **对话记忆按 userId 隔离**（`MessageWindowChatMemory` + `MessageChatMemoryAdvisor`），支持「新对话」清空记忆。

---

## 5. 数据模型（ddl-auto=update 自动建表）

| 表 | 说明 | 关键约束 |
|---|---|---|
| `ticket_sessions` | 展览场次排期 | `total_tickets` / `sold_tickets` / `price` |
| `reservation_drafts` | Agent 草稿 | `uk_draft_idempotency(idempotency_key)` |
| `reservations` | 人工确认后的真实预约 | `draft_id` 唯一（一对一） |
| `exhibition_subscriptions` | 新展订阅 | `uk_subscription_user_exhibition(user_id, exhibition_id)` |

库存扣减唯一入口（条件 UPDATE，防超卖）：
```sql
UPDATE ticket_sessions SET sold_tickets = sold_tickets + :n
WHERE id = :sessionId AND status = 1 AND total_tickets - sold_tickets >= :n
```

---

## 6. 测试（`mvn test`，无需 MySQL / 外网）

| 测试 | 场景 |
|---|---|
| `AgentOrchestrationTest` | a) 多步编排（searchArtworks→createReservationDraft）<br>b) 越权拦截（模型调用白名单外“直接确认”工具 → 拒绝并引导草稿）<br>c) RAG 超时降级<br>d) 步数限制（8 次尝试仅 6 次执行）<br>e) 幂等键规范化/重放/非法键/未登录拦截/余票不足/草稿过期 |
| `AgentToolWhitelistTest` | 白名单精确匹配、写语义工具名禁止、真实写入路径无 @Tool、步数守卫全覆盖 |
| `ReservationDraftIdempotencyTest` | H2 真库验证唯一约束与服务层重放 |
| `LiveDeepSeekSmokeTest` | 真实 DeepSeek 聊天 + 原生函数调用（可选：`-Dagent.live=true -Dai.api.key=sk-xxx`） |
| `RagMysqlDebugTest` | 真库诊断 RAG 召回（可选：`-Ddb.live=true` + datasource 覆盖） |

测试与生产同构：真实 `OpenAiChatModel` + 脚本化 OpenAI 兼容端点（`ScriptedOpenAiServer`），
原生工具循环完全由 Spring AI 驱动。

---

## 7. 运行与试运行

```bash
# 1) 初始化试运行数据（幂等，可重复执行；启用艺术家/上架作品含故事/修正展览/插入票务场次/创建 demo 账号）
mysql -uroot -p123456 --default-character-set=utf8mb4 < trial-data.sql

# 2) 后端（需本地 MySQL：master_art 库；DeepSeek Key 通过环境变量 AI_API_KEY 注入，仓库不含真实 Key）
mvn spring-boot:run

# 3) 前端
cd art-gallery-frontend && npm install && npm run dev
```

**试运行账号**：`demo / 123456`（普通用户，可生成预约草稿/订阅）。
试运行路径：登录 → 首页（数据看板）→ 展览详情（排期余票）→ AI 智能策展（查作品/查排期/生成草稿）
→ 对话内草稿卡片或「我的预约」页点击「确认预约」→ 真实预约 + 库存原子扣减。

### 二次迭代新增功能（v1.1 / v1.2）
- **全站美术馆主题统一**：奶油底 / 墨绿炭黑 / 金色（Element Plus 主题变量 + 全站色板统一 + Manger 外壳与首页 Hero 重设计）；
- **首页数据看板**：`GET /api/stats/overview` 统计在展艺术品/展览总数/进行中展览/艺术家，前端数字滚动动画；
- **我的预约页**（`/my-reservations`，导航栏用户菜单入口）：预约草稿（可续确认/取消）、已确认预约（可取消并回补票额）、新展订阅（可退订）；
  - 配套接口：`GET /api/reservations/drafts/my`；`GET /api/subscriptions/my` 改为返回事务内组装的 `SubscriptionView`（修复懒加载序列化）；
- **AI 入口桥接**：作品详情「让 AI 讲讲这幅画」、艺术家详情「让 AI 介绍这位艺术家」、展览场次「让 AI 生成预约草稿」，
  均跳转 `/ai-art-guide?q=...` 自动发起提问；
- **新对话**：`POST /api/agent/chat/clear` 清空当前用户对话记忆，AI 页右上角「新对话」按钮；
- **RAG 分词召回修复**（试运行实测修复）：整句提问分词召回 + fetch-join 兜底，知识检索对真实提问稳定命中；
- **身份注入式防越权**（试运行实测修复）：写工具 userId 仅由服务端从会话注入，模型无法伪造他人身份。

### 接口速览
- `POST /api/agent/chat` — 非流式对话（含草稿卡片等结构化产物）
- `POST /api/agent/chat/stream` — SSE 流式（`token` / `done` / `error` 事件）
- `POST /api/agent/chat/clear` — 清空对话记忆（新对话）
- `POST /api/reservations/confirm` — 人工确认草稿（真实预约，Agent 不可达）
- `GET /api/reservations/drafts/my` / `GET /api/reservations/my` — 我的草稿/预约
- `GET /api/ticket-sessions/exhibition/{id}` — 排期余票（只读）
- `GET /api/stats/overview` — 首页数据看板
- `POST /api/subscriptions` / `GET /api/subscriptions/my` — 新展订阅

### 配置
```properties
agent.max-tool-steps=6          # 单轮最大工具步数
agent.draft-ttl-minutes=30      # 草稿有效期
agent.rag.timeout-ms=2500       # RAG 检索超时
agent.chat-memory.max-messages=20
spring.ai.openai.api-key=${AI_API_KEY:not-configured}   # Key 仅通过环境变量注入，严禁提交仓库
```
