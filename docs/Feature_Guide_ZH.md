# RecruitAssist — 功能介绍与使用手册

> **版本**: v4.0.0 + Sprint 5 部署增强说明  
> **日期**: 2026年5月  
> **团队**: 第38组, EBU6304 软件工程

---

## 目录

1. [系统概述](#1-系统概述)
2. [技术栈](#2-技术栈)
3. [快速开始](#3-快速开始)
4. [功能详解](#4-功能详解)
   - 4.1 [认证与会话管理](#41-认证与会话管理)
   - 4.2 [TA（助教）功能](#42-ta助教功能)
   - 4.3 [MO（课程负责人）功能](#43-mo课程负责人功能)
   - 4.4 [Admin（管理员）功能](#44-admin管理员功能)
   - 4.5 [推荐引擎](#45-推荐引擎)
5. [数据架构](#5-数据架构)
6. [系统配置](#6-系统配置)
7. [接口列表](#7-接口列表)

---

## 1. 系统概述

**RecruitAssist** 是一个轻量级的 Java Servlet/JSP 原型系统，专为**教学助理（TA）招聘流程**设计。系统支持三种用户角色 —— TA、MO（课程负责人）和 Admin（管理员），每种角色拥有独立的仪表盘和功能界面。

### 核心亮点

- **可解释推荐引擎**：6 维加权评分模型，为每个推荐结果提供可读的匹配解释
- **完整申请生命周期**：提交 → 入围 → 接受/拒绝 → 撤回，岗位满额或过期时自动关闭
- **基于角色的访问控制**：三种角色拥有不同的视图、操作和数据可见性
- **零数据库架构**：所有数据以 JSON/CSV/TXT 文件存储，内置缓存和并发控制
- **输入验证与安全防护**：XSS 防护、文件类型白名单、配额一致性校验

### 系统规模统计（v4.0.0）

| 指标 | 数量 |
|------|------|
| Java 类 | 50 |
| JSP 页面 | 8 |
| Servlet | 19（1个抽象基类 + 18个具体实现） |
| Service 服务层 | 8 |
| Repository 仓储层 | 7 |
| 演示用户 | 100+ |
| 演示岗位 | 50+ |
| 演示申请 | 500+ |

### Sprint 5：服务器部署与生产化增强（Haopeng Jin）

Sprint 5 作为 Sprint 4 之后的部署与生产化扩展，主要由 **Haopeng Jin** 负责，重点不是新增业务角色，而是让系统可以稳定运行在外部服务器，并把 PDF 分享、并发安全和部署校验做成可演示的工程能力。

- **公网部署地址**：`http://RecruitAssistGroup38.21.214.216.122.nip.io:8080/home`
- **服务器启动记录**：`homework/plan/服务器端指令.md`
- **部署包目录**：`homework/releases/server/RecruitAssist-server-v4.0.0/`
- **部署归档**：`homework/releases/server/RecruitAssist-server-v4.0.0.tar.gz`
- **环境要求**：Java 17、Maven、Jetty 12 EE10、端口 8080、可写 `data/` 与 `logs/`
- **启动脚本**：`scripts/start-maven-jetty.sh` 会自动设置 `RECRUITASSIST_BASE_DIR`，保证服务器读取发布包内的 `data/`
- **健康检查**：`/health` 返回 JSON 状态，用于确认用户、岗位、申请数据已正确加载
- **数据完整性校验**：发布包内加入 `DATA_MANIFEST.json`，打包时校验 `users/jobs/applications/cv/system` 文件数量和 hash，避免缺用户或缺申请数据

Sprint 5 同时补充了两个生产化能力：

- **永久受保护 PDF Token 链接**：`PdfShareService.java` 为每个 CV 所有者生成稳定随机 token；`SharedPdfServlet.java` 通过 `/pdf/share?token=...` 返回内联 PDF，但仍要求登录并校验 TA 本人、MO 岗位归属或 Admin 角色。
- **并发与高可用延伸**：沿用 Sprint 4 的 CSRF、POST logout、注册同步锁、跨进程文件锁、原子写入、坏 JSON 隔离读取和 `scripts/load_test_recruitassist.py`，并通过服务器端部署验证 Java 17/Jetty 12 的实际运行兼容性。

### 功能负责人速查

为了方便演示和评分，每个功能区域都对应到负责人、代码路径和可展示例子：

| 负责人 | 功能区域 | 代码路径 | 用户可见例子 |
|--------|----------|----------|--------------|
| Yi Qi | 登录、注册、首页、会话入口 UI | `LoginServlet.java`, `RegisterServlet.java`, `LogoutServlet.java`, `HomeServlet.java`, `login.jsp`, `register.jsp`, `home.jsp` | 访客打开 `/home` 查看实时统计，使用快速登录或注册 TA/MO 账号，然后进入对应仪表盘。 |
| Tianyu Zhao | TA 仪表盘、Profile、CV 上传/下载、申请/撤回流程 | `UpdateProfileServlet.java`, `UploadCvServlet.java`, `DownloadCvServlet.java`, `ApplyServlet.java`, `WithdrawApplicationServlet.java`, `dashboard-ta.jsp` | TA 修改技能、上传 CV、申请推荐岗位，并在申请历史中看到记录。 |
| Jie Ren | MO 仪表盘、岗位 CRUD、候选人管理 | `CreateJobServlet.java`, `UpdateJobServlet.java`, `ChangeJobStatusServlet.java`, `JobService.java`, `dashboard-mo.jsp`, `job-detail.jsp` | MO 创建岗位、编辑要求、查看排序后的候选人，并将申请改为 Shortlisted/Accepted/Rejected。 |
| Haopeng Jin | 推荐引擎、申请生命周期、Sprint 5 部署、永久 PDF Token 分享、安全并发与高可用加固 | `RecommendationService.java`, `ApplicationService.java`, `AppServlet.java`, `JsonFileStore.java`, `IdCounterRepository.java`, `PdfShareService.java`, `SharedPdfServlet.java`, `scripts/load_test_recruitassist.py`, `homework/releases/server/`, `homework/plan/服务器端指令.md` | 系统解释 TA-岗位匹配原因，防重复申请，校验 CSRF token，提供 `/health`，支持负载测试，并已部署到公网服务器。 |
| Zhuang Hou | Admin 仪表盘、工作量监控、CSV 导出 | `DashboardServlet.renderAdminDashboard()`, `WorkloadService.java`, `AdminExportServlet.java`, `dashboard-admin.jsp` | Admin 查看 TA 工作量风险，并导出 jobs/applications/workload CSV 报表。 |
| Zexuan Dong | 文件基础设施、Repository、启动初始化和测试 | `AppPaths.java`, `AppServices.java`, `JsonFileStore.java`, repositories, `AppBootstrapListener.java`, `src/test/java/**` | 项目无需数据库即可运行：用户、岗位、申请、通知、配置和审计日志存储在 JSON/CSV/TXT 文件中。 |

---

## 2. 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 编程语言 | Java | 17 |
| 构建工具 | Maven | 3.9+ |
| Web 框架 | Jakarta Servlet | 6.0.0 |
| 视图层 | JSP + JSTL | 3.0 |
| JSON 处理 | Gson | 2.10.1 |
| 应用服务器 | Jetty（内嵌） | 12.0.15 |
| 打包方式 | WAR | - |
| 数据存储 | JSON / CSV / TXT 文件 | - |
| 测试框架 | JUnit Jupiter | 5.10.2 |

---

## 3. 快速开始

### 环境要求

- Java 17 (JDK)
- Maven 3.9+
- Python 3（可选，用于生成演示数据）

### 启动步骤

```bash
# 克隆仓库
git clone https://github.com/yi-Q945/Group38-TA_Recruitment.git
cd Group38-TA_Recruitment

# （可选）生成大规模演示数据
python3 scripts/generate_demo_load.py

# 启动应用
export RECRUITASSIST_BASE_DIR=$(pwd)
mvn -f framework/recruitassist-web/pom.xml \
    org.eclipse.jetty.ee10:jetty-ee10-maven-plugin:12.0.15:run \
    -Djetty.http.port=8081 -Djetty.contextPath=/
```

浏览器访问：http://127.0.0.1:8081/

### 演示账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| TA | `alice.ta` | `demo123` |
| TA | `ben.ta` | `demo123` |
| MO | `mo.chen` | `demo123` |
| MO | `recruiter.01` | `demo123` |
| Admin | `admin.sarah` | `demo123` |

---

## 4. 功能详解

### 4.1 认证与会话管理

**登录** (`/login`)
- 基于用户名 + 密码的表单认证
- 基于 `HttpSession` 的会话状态管理
- **会话固定攻击防护**：登录成功后先调用 `session.invalidate()` 销毁旧会话，再调用 `getSession(true)` 创建新会话
- **用户名枚举防护**：登录失败时统一显示 "Invalid username or password"，不区分是用户名还是密码错误
- **密码哈希存储**：新注册密码使用 PBKDF2-HMAC-SHA256 保存；旧演示数据中的明文密码仍可兼容读取
- **失败登录节流**：同一用户名连续 5 次登录失败后，会触发 5 分钟临时锁定
- **CSRF 防护**：所有 POST 表单都带会话 token，`AppServlet` 会对缺失或错误 token 返回 HTTP 403
- **Session 加固**：登录后的会话 30 分钟无操作自动过期；所有 Servlet 响应带基础安全头（`X-Content-Type-Options`、`X-Frame-Options`、`Referrer-Policy`、`Cache-Control`）
- Flash 消息系统用于成功/错误反馈（如 "Signed in as Amelia Chen."）
- 登录页面提供**演示用户快速选择面板**：最多展示 3 个 TA、2 个 MO、1 个 Admin 账号，点击 "Use account" 按钮自动填充表单
- **实时统计头部**：显示系统总账号数、TA 数、MO 数、Admin 数
- **粘性表单**：登录失败后自动保留已输入的用户名
- 已登录用户访问 `/login` 会自动重定向到 `/dashboard`

**注册** (`/register`)
- 新用户注册表单，包含 6 个字段：用户名（必填）、显示名称、邮箱、角色（TA 或 MO）、密码（必填，≥6 位）、确认密码
- **多层验证机制**：

| 检查项 | 层级 | 实现方式 |
|--------|------|----------|
| 用户名格式 | 前端 + 后端 | HTML5 `pattern="[a-zA-Z0-9._-]{3,30}"` + Java 正则 |
| 用户名唯一性 | 后端 | `findByUsername()` 数据库查找 |
| 显示名称唯一性 | 后端 | 遍历所有用户，大小写不敏感比较 |
| 密码长度 | 前端 + 后端 | HTML5 `minlength=6` + Java `length < 6` 检查 |
| 密码一致性 | 后端 | `password.equals(confirmPassword)`，通过后再做 PBKDF2 哈希持久化 |
| Admin 角色拦截 | 后端 | `role == ADMIN` → 直接拒绝 |
| 邮箱格式 | 前端 + 后端 | HTML5 `type=email` + Java 正则 |
| XSS 防护 | 后端 | `cleanText()` 去除 `<>` 标签和控制字符 |

- **Admin 注册封锁**：角色下拉菜单仅提供 TA 和 MO 选项；即使通过请求篡改提交 `role=ADMIN`，服务端也会返回明确错误
- **粘性表单**：验证失败后保留所有已输入字段（用户名、姓名、邮箱、已选角色），用户无需重新填写
- 注册成功后设置 Flash 消息（"Account created successfully!"），重定向到登录页
- `UserService.registerUser()` 使用同步临界区，保证并发注册时“用户名唯一性检查 + 用户创建”是原子操作

**登出** (`/logout`)
- 调用 `session.invalidate()` 彻底销毁当前会话的所有数据
- 创建新会话并写入 "You have been signed out." 提示
- 使用 POST + CSRF token，避免 GET 链接触发状态变更，然后重定向到登录页

**首页** (`/home`)
- 未登录用户的公开着陆页，采用现代化 Hero 布局
- 展示 **5 项实时统计**：TA 总数、MO 总数、Admin 总数、岗位数、申请数——全部从数据层实时计算
- **一键快速登录卡片**：每个角色展示一个代表账户，点击按钮直接 POST 登录
- **功能展示网格**：三张卡片分别介绍 TA/MO/Admin 工作流
- **演示路径引导**：3 步骤演示建议（TA → MO → Admin）
- 已登录用户自动重定向到对应角色的仪表盘

**健康检查** (`/health`)
- Sprint 4 部署/可用性检查入口
- 检查必要目录是否存在，并返回实时用户数、岗位数和申请数
- 文件存储可读时返回 HTTP 200 和 `{"status":"UP"}`；运行时存储检查失败时返回 HTTP 503 和 `{"status":"DOWN"}`

### 4.2 TA（助教）功能

#### 4.2.1 仪表盘（`/dashboard` — TA 视图）

TA 仪表盘提供全方位的工作台：

- **顶部概览区**：欢迎语 + KPI 卡片（工作量、活跃申请数、Profile 完成度）
- **最佳推荐聚焦**：突出显示匹配度最高的岗位及评分标签
- **Profile 管理表单**：编辑姓名、学号、邮箱、专业、技能、可用时间、经验、CV 文本
- **CV 上传**：支持 PDF、DOC、DOCX、TXT（最大 5MB）
- **申请历史表格**：展示所有申请的状态、评分和时间戳
- **申请通知列表**：展示 MO 状态变更通知、未读数量，并支持标记已读
- **推荐岗位网格**：可搜索/排序的岗位卡片列表，包含：
  - 综合匹配百分比和适配标签
  - 匹配/缺失技能标签
  - 6 维进度条（技能、可用性、经验、工作量、Profile、竞争）
  - 可读的推荐理由
  - 申请/查看详情按钮

**搜索与排序**：支持按关键词（标题、模块、技能、描述）过滤；按推荐分数、截止日期或工作量排序。

#### 4.2.2 申请岗位（`/apply`）

- 可从仪表盘或岗位详情页一键申请
- 提交前验证：
  - Profile 必须足够完整（姓名、邮箱、技能必填）
  - 岗位必须为 OPEN 状态且未过期
  - 配额未满
  - 不能重复申请（已有未撤回的申请时阻止）
- 提交时计算并保存推荐评分
- 创建审计日志记录

#### 4.2.3 撤回申请（`/applications/withdraw`）

- 适用于 SUBMITTED 或 SHORTLISTED 状态的申请
- 状态设为 WITHDRAWN
- 撤回后可以重新申请该岗位

#### 4.2.4 申请通知（`/notifications/read`）

- 当 MO 将申请状态更新为 SHORTLISTED、ACCEPTED 或 REJECTED 时自动创建通知
- TA 仪表盘展示最近通知和未读数量
- TA 可以将自己的通知标记为已读；服务端会校验只能操作本人通知

#### 4.2.5 Profile 管理（`/profile/update`）

可编辑字段：
- 姓名、学号、邮箱（格式验证）
- 专业、技能（支持逗号/分号/换行分隔）
- 可用时间、经验描述、CV 文本
- 输入清理：自动去除 HTML 标签、控制字符，限制最大长度

#### 4.2.6 CV 上传（`/profile/cv/upload`）

- 支持格式：PDF、DOC、DOCX、TXT
- 最大文件大小：5MB
- 存储为 `{userId}_cv.{扩展名}` 在 `data/cv/` 目录
- 重新上传时自动删除旧 CV
- 文件元数据（文件名、上传时间）保存到用户 Profile
- 每个 CV 所有者会获得一个永久随机 `pdfShareToken`，保存在 `UserProfile` 中
- token 链接格式为 `/pdf/share?token=...`；TA 可打开自己的链接，MO 只能打开自己岗位候选人的链接，Admin 可打开所有有效 CV 链接
- `SharedPdfServlet.java` 代理文件响应并做角色校验，不把 `data/cv/` 目录暴露为公开静态资源

#### 4.2.7 永久 PDF 链接设计（`/pdf/share`）

PDF 链接按用户维度长期有效，不是临时链接；但 token 本身并不等于完全公开访问，后端仍会校验当前登录用户和业务关系：

- **Token 所属者**：`UserProfile.pdfShareToken`，由 `PdfShareService.ensureToken()` 生成
- **访问路径**：TA/Admin 使用 `GET /pdf/share?token={token}`，MO 审核候选人时使用 `GET /pdf/share?token={token}&jobId={jobId}`
- **TA 规则**：只能打开自己的 CV 链接
- **MO 规则**：只有当候选人申请了该 MO 拥有的岗位时，MO 才能打开该候选人链接
- **Admin 规则**：可以打开任意有效 CV 链接，用于审核和验收
- **存储规则**：真实文件仍保存在 `data/cv/`；Servlet 会标准化路径并防止目录穿越
- **体验例子**：TA 在 Profile & CV 区看到 "Open permanent PDF link"；MO 在候选人队列看到 "Open PDF"；Admin 在 Latest applications 表中看到 "Open PDF"

### 4.3 MO（课程负责人）功能

#### 4.3.1 仪表盘（`/dashboard` — MO 视图）

- **KPI 卡片**：开放岗位数、总申请数、入围数、已接受数
- **岗位概览卡片**：每个自有岗位的模块、标题、状态、截止日期、配额、工时
- **创建新岗位表单**：标题、模块代码、截止日期、配额、每周工时、必需技能、优选技能、描述
- **候选人队列**：每个岗位的申请人表格，显示：
  - 姓名、技能、工作量（当前工时）
  - 推荐评分百分比 + 解释摘要
  - 状态下拉菜单（接受/拒绝/入围）
  - 开放/关闭岗位切换按钮

#### 4.3.2 创建岗位（`/jobs/create`）

验证规则：
- 标题（必填，最长 200 字符）
- 模块代码（必填）
- 截止日期（必须为未来日期，`yyyy-MM-dd` 格式）
- 配额（正整数）
- 每周工时（正整数）
- 必需技能（至少 1 个）
- 优选技能（可选）
- 描述（必填）

XSS 防护：所有文本字段自动清除 `<>` 标签和控制字符。

#### 4.3.3 编辑岗位（`/jobs/update`）

- 仅岗位所有者可编辑
- 配额不能低于已接受的申请数
- 编辑后如果截止日期已过或配额已满，岗位自动关闭

#### 4.3.4 关闭/重开岗位（`/jobs/status`）

- **关闭**：将岗位状态设为 CLOSED
- **重开**：将岗位状态恢复为 OPEN
  - 验证截止日期未过
  - 验证配额未满
- 所有状态变更写入审计日志

#### 4.3.5 审核申请（`/applications/status`）

MO 可执行的状态转换：
- SUBMITTED → SHORTLISTED（入围）
- SUBMITTED → ACCEPTED（接受）
- SUBMITTED → REJECTED（拒绝）
- SHORTLISTED → ACCEPTED
- SHORTLISTED → REJECTED

**自动关闭**：当最后一个配额名额被 ACCEPTED 填满时，岗位自动关闭。

**状态通知**：状态更新成功后会为 TA 创建站内通知，TA 可在仪表盘查看入围/接受/拒绝结果。

#### 4.3.6 打开候选人 PDF（`/pdf/share`）

访问控制：
- MO 只能打开申请了自己岗位的候选人 CV/PDF 链接
- Admin 可以打开任何 CV/PDF 链接
- TA 只能打开自己的 CV/PDF 链接
- `/pdf/share` 以内联方式返回文件，方便浏览器预览；`/cv/download` 仍保留用于附件下载

### 4.4 Admin（管理员）功能

#### 4.4.1 仪表盘（`/dashboard` — Admin 视图）

- **KPI 卡片**：跟踪的 TA 数、近期申请数、策略阈值、筛选岗位数
- **招聘概览表格**：所有岗位，可按状态（Open/Closed）和模块代码搜索
  - 列：岗位名、负责人、截止日期、申请人数、已接受/配额、状态
- **TA 工作负载表**：所有 TA 的：
  - 已接受工时、活跃申请数
  - "均衡" / "超出阈值" 指示器
- **最新申请列表**：系统范围内最新的 10 条申请记录
- **CSV 导出**：可下载岗位、申请或工作量 CSV 报表

#### 4.4.2 CSV 导出（`/admin/export`）

- 仅 Admin 可访问
- 导出类型：
  - `type=jobs`：岗位 ID、模块、标题、负责人、截止日期、状态、配额、工时、申请数、已接受数
  - `type=applications`：申请 ID、岗位、模块、申请人、状态、推荐百分比、提交时间
  - `type=workload`：TA 用户 ID、姓名、专业、已接受工时、活跃申请数、阈值、状态
- 通过 `Content-Disposition: attachment` 返回带日期的 CSV 文件名

### 4.5 推荐引擎

推荐引擎是 RecruitAssist 的核心创新。它对每个 TA-岗位配对进行 **6 个维度的加权评分**：

#### 评分公式

```
最终评分 = Σ(维度得分 × 权重) / Σ(权重)
```

#### 六维评分

| 维度 | 权重 |
|------|------|
| **技能匹配** | 40% |
| **经验证据** | 18% |
| **时间可用性** | 12% |
| **工作量平衡** | 12% |
| **Profile 证据** | 10% |
| **竞争压力** | 8% |

**技能匹配（40%）**
必需技能覆盖率 ×0.72 + 优选技能 ×0.18 + 技能广度加分 + 全覆盖加分 − 缺失惩罚。
使用 Jaccard 相似度 ≥0.55 进行模糊匹配，并进行技能别名归一化。

**经验证据（18%）**
基准 0.3 + 文本证据加分。分析短语覆盖率（岗位关键词在 Profile 中出现）、
Token 重叠度、证据关键词命中（lab, marking, debugging 等 16 个关键词）。

**时间可用性（12%）**
基准 0.34 + 关键词加分：weekday/weekend、具体星期几、时间段（上午/下午/晚上）、灵活性指标。

**工作量平衡（12%）**
未超阈值：0.55 + 剩余比例 ×0.45。超出阈值：按超出幅度施加惩罚。促进公平分配。

**Profile 证据（10%）**
5 个 Profile 字段完整度（58%）+ 岗位对齐度（22%）+ 技能广度信号（20%）。

**竞争压力（8%）**
每剩余名额的活跃申请人比率。≤1/名额 → 0.95；≤2 → 0.82；≤3 → 0.68；更高则递减。

#### 可解释性

每个维度生成一段自然语言解释，展示在 TA 仪表盘和岗位详情页中，帮助 TA 理解推荐原因和 Profile 改进方向。

#### 技能匹配细节

- **三级匹配**：(1) 规范化精确匹配 → (2) Jaccard 相似度 ≥ 0.55 → (3) Token 包含匹配
- **别名归一化**："OOP" ↔ "面向对象编程"，"JS" ↔ "JavaScript" 等（8 组别名）
- **分词**：正则 `[a-z0-9]{2,}` + 停用词过滤

---

## 5. 数据架构

### 存储结构

```
data/
├── users/          # 用户 Profile JSON (U*.json)
├── jobs/           # 岗位 JSON (J*.json)
├── applications/   # 申请记录 JSON (A*.json)
├── notifications/  # TA 站内通知 JSON (N*.json)
├── cv/             # 上传的 CV 文件 ({userId}_cv.{ext})
└── system/
    ├── config.json       # 系统配置（权重、阈值）
    └── id-counters.json  # 自增 ID 计数器
logs/
└── access/
    └── audit.csv   # 审计日志（操作、用户ID、时间戳）
```

### 缓存与并发控制

**JsonFileStore** 提供：
- **两级缓存**：文件级缓存（key = 路径 + 修改时间 + 大小）和目录级缓存
- **路径级读写锁**：`ConcurrentHashMap<Path, ReentrantReadWriteLock>` 实现细粒度并发控制
- **原子写入**：先写入临时文件，再原子移动到目标路径
- **跨进程写锁**：写入 JSON/CSV 前通过同级 `.lock` 文件和 `FileChannel.lock()` 串行化，降低多 Tomcat 实例同时写同一文件的风险
- **失败清理与容错**：写入失败时尽力删除临时文件；目录读取遇到坏 JSON 会跳过该文件，避免一个损坏文件拖垮整个列表
- **缓存失效**：写操作后自动清除文件缓存；目录缓存按前缀匹配失效

### Sprint 4 并发验证

- `ApplicationService` 对申请提交和状态更新使用写锁串行化处理，重复申请检查与写入处于同一个临界区。
- `IdCounterRepository` 对计数器读、更新、写入同时使用 JVM 内锁和跨进程文件锁，避免并发创建时生成重复 ID。
- `ApplicationConcurrencyTest` 同时发起 24 次同一 TA/岗位申请，验证只有 1 次成功，且磁盘上只持久化 1 条 JSON 申请记录。
- `PasswordHasherTest` 与 `AuthServiceTest` 覆盖 PBKDF2 验证、旧演示密码兼容，以及连续失败登录后的临时锁定。
- `NotificationServiceTest` 验证 MO 状态更新会创建 TA 未读通知，并验证已读状态只能由通知本人更新。
- `mvn verify` 会生成 JaCoCo 覆盖率报告，位置为 `framework/recruitassist-web/target/site/jacoco/`。

---

## 6. 系统配置

### 系统配置文件（`data/system/config.json`）

```json
{
  "appName": "RecruitAssist",
  "storage": {
    "mode": "text-files-only",
    "formats": ["json", "csv", "txt"]
  },
  "workload": {
    "defaultMaxHours": 12
  },
  "recommendation": {
    "skillMatchWeight": 0.4,
    "availabilityWeight": 0.12,
    "experienceWeight": 0.18,
    "workloadBalanceWeight": 0.12,
    "profileEvidenceWeight": 0.1,
    "competitionWeight": 0.08
  }
}
```

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `RECRUITASSIST_BASE_DIR` | 项目根目录 | 从当前工作目录自动检测 |
| `recruitassist.baseDir` | 同上（Java 系统属性） | - |

---

## 7. 接口列表

| 方法 | URL | 角色 | 说明 |
|------|-----|------|------|
| GET | `/home` | 公开 | 着陆页，展示系统统计 |
| GET | `/health` | 公开 | 部署健康检查 / 可用性探针 |
| GET/POST | `/login` | 公开 | 登录表单 / 认证 |
| GET/POST | `/register` | 公开 | 注册表单 / 创建新账户（仅 TA 和 MO） |
| POST | `/logout` | 已登录 | 带 CSRF token 结束会话 |
| GET | `/dashboard` | 已登录 | 角色化仪表盘 |
| GET | `/jobs/detail?id={jobId}` | 已登录 | 岗位详情（角色化视图） |
| POST | `/jobs/create` | MO | 创建新岗位 |
| POST | `/jobs/update` | MO | 编辑岗位 |
| POST | `/jobs/status` | MO | 关闭/重开岗位 |
| POST | `/apply` | TA | 提交申请 |
| POST | `/applications/withdraw` | TA | 撤回申请 |
| POST | `/applications/status` | MO | 接受/拒绝/入围申请 |
| POST | `/notifications/read` | TA | 标记通知为已读 |
| GET | `/admin/export?type={jobs|applications|workload}` | Admin | 下载 CSV 报表 |
| POST | `/profile/update` | TA | 更新个人 Profile |
| POST | `/profile/cv/upload` | TA | 上传 CV 文件 |
| GET | `/cv/download?userId={id}` | 认证+ACL | 下载 CV 文件 |
| GET | `/pdf/share?token={token}` | 认证+ACL | 打开永久受保护 CV/PDF 链接 |
