# RecruitAssist — 组员分工与代码实现报告

> **版本**: v4.0.0 + Sprint 5 部署增强说明 | **日期**: 2026年5月 | **课程**: EBU6304 软件工程 — 第38组

---

## 1. 团队概览

| 成员 | 主要职责 | 核心文件 | 代码量（约） |
|------|---------|---------|------------|
| Yi Qi | 登录、注册、首页与 UI | 12 个文件 | ~900 行 |
| Tianyu Zhao | TA 仪表盘与简历与申请 | 8 个文件 | ~700 行 |
| Jie Ren | MO 仪表盘与岗位 CRUD | 6 个文件 | ~700 行 |
| Haopeng Jin | 推荐引擎、申请服务、PDF Token 分享、Sprint 5 服务器部署、安全并发与高可用 | 18 个文件 | ~2,400 行 |
| Zhuang Hou | 管理员仪表盘与工作量 | 6 个文件 | ~500 行 |
| Zexuan Dong | 数据层与基础设施 | 15 个文件 | ~1,200 行 |

### 1.1 答辩用成员功能地图

下表用于演示和答辩快速说明。每个人都列出负责功能、主要代码路径、功能说明和一个可现场展示的例子。

| 成员 | 负责功能区域 | 主要代码路径 | 功能说明 | 具体例子 / 演示证据 |
|------|--------------|--------------|----------|---------------------|
| Yi Qi | 登录、注册、首页、会话入口流程和 UI 优化 | `LoginServlet.java`, `RegisterServlet.java`, `LogoutServlet.java`, `HomeServlet.java`, `AuthService.java`, `UserService.registerUser()`, `login.jsp`, `register.jsp`, `home.jsp`, `assets/css/app.css`, `assets/js/app.js` | 负责所有角色进入系统前后的入口体验：用户可以访问首页、查看实时系统统计、使用演示账号快速登录、注册 TA/MO 账号、登录后进入对应仪表盘，并通过 Flash 消息获得反馈。登录/注册路径包含输入校验、粘性表单、统一错误提示、密码哈希、失败锁定和会话清理。 | 演示：打开 `/home`，点击 TA 快速登录卡片或进入 `/login` 选择 `alice.ta`，登录后进入 TA 仪表盘；也可以通过 `/register` 创建一个新 TA 账号，看到成功提示后再用新账号登录。 |
| Tianyu Zhao | TA 仪表盘、TA Profile 编辑、CV 上传/下载、申请和撤回入口 | `DashboardServlet.renderTaDashboard()`, `UpdateProfileServlet.java`, `UploadCvServlet.java`, `DownloadCvServlet.java`, `ApplyServlet.java`, `WithdrawApplicationServlet.java`, `dashboard-ta.jsp`, `UserService.updateTaProfile()` | 负责 TA 的主要工作区：TA 可以维护个人资料证据、上传 CV、查看推荐岗位卡片、申请合适岗位、撤回可撤回申请，并在申请历史里跟踪状态。CV 流程包含文件类型白名单、大小限制、旧文件替换和基于角色的访问权限控制。 | 演示：登录 `alice.ta`，修改 skills 或 availability，上传一份 `.txt`/PDF CV，点击推荐岗位的 Apply，随后在申请历史表中看到新增申请、推荐分和状态。 |
| Jie Ren | MO 仪表盘、岗位创建/编辑、岗位开启关闭、候选人审核表 | `CreateJobServlet.java`, `UpdateJobServlet.java`, `ChangeJobStatusServlet.java`, `JobService.java`, `UpdateApplicationStatusServlet.java`, `dashboard-mo.jsp`, `job-detail.jsp`, `data/jobs/` | 负责 Recruiter/MO 工作流：MO 可以创建经过校验的岗位、编辑自己发布的岗位、关闭或重开岗位、查看候选人列表、下载符合权限的 CV，并更新候选人申请状态。岗位校验覆盖必填项、未来截止日期、正数配额/工时、技能解析和 XSS 文本清洗。 | 演示：登录 `mo.chen`，创建一个 required skills 包含 `Java, Testing` 的岗位，进入岗位详情页查看候选人排名，然后在候选人表中将 TA 设为 shortlisted 或 accepted。 |
| Haopeng Jin | 推荐引擎、申请生命周期、搜索过滤、永久 PDF token 分享、Sprint 5 服务器部署、安全并发与高可用加固 | `RecommendationService.java`, `ApplicationService.java`, `JobDetailServlet.java`, `DashboardServlet.java`, `AppServlet.java`, `JsonFileStore.java`, `IdCounterRepository.java`, `LogoutServlet.java`, `PdfShareService.java`, `SharedPdfServlet.java`, `scripts/load_test_recruitassist.py`, `homework/releases/server/`, `homework/plan/服务器端指令.md` | 负责可解释推荐和关键状态变更层，并把系统推进到可部署状态。推荐引擎从 6 个维度评分；申请服务负责防重复申请、状态更新和配额满自动关闭岗位；Sprint 5 进一步完成公网服务器部署、永久受保护 PDF token 链接、发布包数据校验、Java 17/Jetty 12 兼容性处理和负载测试脚本。 | 演示：访问 `http://RecruitAssistGroup38.21.214.216.122.nip.io:8080/home`，登录 `alice.ta` 对比推荐百分比并申请岗位；再登录 `mo.chen` 接受/拒绝申请。安全/部署证据：查看 `csrfToken`、打开 `/pdf/share?token=...`、访问 `/health`，并展示 `homework/releases/server/` 发布包和 `服务器端指令.md`。 |
| Zhuang Hou | Admin 仪表盘、工作量监控、全局招聘概览、CSV 导出 | `DashboardServlet.renderAdminDashboard()`, `WorkloadService.java`, `AdminExportServlet.java`, `dashboard-admin.jsp`, `WorkloadEntry.java`, `SystemConfig.java`, `data/system/config.json` | 负责管理员视角：Admin 可以查看全局岗位和最近申请，按配置阈值监控 TA 工作量，识别超载 TA，筛选岗位，并导出 jobs/applications/workload 三类 CSV 供报告和验收截图使用。 | 演示：登录 `admin`，查看 Admin Dashboard 的 KPI 和 TA workload 表，按状态/关键词筛选岗位，然后点击 CSV 导出按钮下载岗位、申请和工作量报表。 |
| Zexuan Dong | 文件数据层、仓储、应用启动、种子数据、审计日志和测试 | `AppPaths.java`, `AppServices.java`, `JsonFileStore.java`, `UserRepository.java`, `JobRepository.java`, `ApplicationRepository.java`, `AuditRepository.java`, `SystemConfigRepository.java`, `IdCounterRepository.java`, `AppBootstrapListener.java`, `src/test/java/**` | 负责课程要求的零数据库持久化基础：用户、岗位、申请、通知、配置、计数器、CV 文件和审计日志都存储在 JSON/CSV/TXT 文件中。Repository 层统一文件访问，`AppServices` 完成服务装配，启动监听器创建必要目录，测试保护核心数据完整性。 | 演示证据：展示 `data/users`, `data/jobs`, `data/applications`, `data/system/config.json`, `logs/access/audit.csv`；运行 `mvn verify` 展示仓储/服务测试和 JaCoCo 覆盖率报告。 |

---

## 2. 各成员贡献详情

### 2.1 Yi Qi — 登录、注册、首页与 UI 资产

**负责文件**:
- `LoginServlet.java`, `LogoutServlet.java`, `HomeServlet.java`, `RegisterServlet.java`
- `AuthService.java`（认证逻辑）
- `UserService.java` — `registerUser()` 方法（注册校验与持久化）
- `login.jsp`, `register.jsp`, `home.jsp`, `index.jsp`
- README、中文 README 与架构图

**对应 Backlog**: #1 用户登录与角色访问, #12 用户注册与认证, #13 响应式 UI

#### 功能介绍（演示说明）

**登录页面** (`/login`)
- 基于用户名 + 密码的表单认证，使用 `HttpSession` 管理会话状态
- 登录页面展示**演示用户快速选择面板**：最多 3 个 TA、2 个 MO、1 个 Admin 账号，每个账号带 "Use account" 按钮，可通过前端 `data-fill-login` 自动填充表单
- 登录页顶部显示**实时账号统计**：总账号数、TA 数、MO 数、Admin 数，便于 demo 时说明数据规模
- 登录成功后显式销毁旧会话并创建新会话，防止会话固定攻击
- 新注册用户密码通过 PBKDF2-HMAC-SHA256 哈希后保存；旧演示账号的明文密码保持兼容
- 同一用户名连续 5 次失败会触发 5 分钟临时锁定，降低暴力尝试风险
- Flash 消息系统显示成功/错误反馈（如 "Signed in as Amelia Chen."）
- 登录失败仅显示通用错误 "Invalid username or password"，不暴露具体是用户名还是密码错误，降低用户名枚举风险
- 登录失败后保留已输入用户名，提升表单体验；已登录用户访问 `/login` 会自动跳转到 `/dashboard`

**注册页面** (`/register`)
- 新用户注册表单包含 6 个字段：用户名、显示名称、邮箱、角色（TA/MO）、密码、确认密码
- **Admin 注册封锁**：页面只提供 TA 和 MO 角色；即使用户篡改请求提交 `role=ADMIN`，后端也会拒绝
- **用户名校验**：3-30 位，只允许字母、数字、点、下划线和连字符；保存前统一转小写并检查唯一性
- **显示名称唯一性校验**：对现有用户名称做大小写不敏感比较，避免 demo 中出现重复展示名
- **邮箱与密码校验**：邮箱格式前后端双层校验；密码至少 6 位，并要求与确认密码一致
- **密码持久化加固**：注册通过后调用 `PasswordHasher.hash()`，不再把新用户密码明文写入 JSON
- **输入清洗与 XSS 防护**：通过 `cleanText()` 去除 `< >` 和控制字符，并限制字段最大长度
- **粘性表单**：注册失败后保留用户名、姓名、邮箱和已选角色，用户无需重新填写
- 注册成功后写入 Flash 消息并跳转登录页，引导用户用新账号登录

**登出** (`/logout`)
- 调用 `session.invalidate()` 清理当前会话，重新创建只携带提示信息的新会话，并重定向到登录页
- 避免登出后残留 `userId`、Flash 或其他 Session 属性

**首页** (`/home`)
- 未登录用户的公开着陆页，采用 Hero 区域 + 功能卡片的展示结构
- 实时展示 5 类系统统计数据：TA 总数、MO 总数、Admin 总数、岗位数、申请数
- 提供 TA/MO/Admin 三种角色的一键快速登录卡片，方便课堂演示和角色切换
- 通过功能展示卡说明 TA 推荐、MO 岗位管理、Admin 工作量监控三条核心路径
- 提供 3 步 demo 路线建议：TA 视角 → MO 视角 → Admin 视角
- 已登录用户自动重定向到对应角色的仪表盘

**认证服务** (`AuthService.java`)
- 接收用户名和密码，先进行空值与空白输入检查
- 调用 `UserService.findByUsername()` 查询用户，并通过 `PasswordHasher.verify()` 验证 PBKDF2 哈希或旧演示明文密码
- 记录失败尝试次数，达到阈值后临时锁定对应用户名键
- 返回 `Optional<UserProfile>`，失败时为空，Servlet 层据此决定转发回登录页或进入仪表盘
- 认证逻辑与 Servlet 展示逻辑分离，便于测试和复用

**演示路径**：打开应用 → 查看首页统计和功能卡片 → 点击 "Open demo login" → 在登录页查看账号统计与快选表格 → 点击 `alice.ta` 的 "Use account" 自动填充 → 登录进入 TA 仪表盘；也可以点击注册入口创建新的 TA/MO 账号并回到登录页验证。

#### 核心实现

**1. 登录认证与会话安全** — `LoginServlet.java`

```java
protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
    String username = req.getParameter("username");
    String password = req.getParameter("password");
    Optional<UserProfile> userOpt = services(req).authService()
            .authenticate(username, password);
    if (userOpt.isEmpty()) {
        req.setAttribute("loginError", "Invalid username or password.");
        populateLoginView(req);
        req.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(req, resp);
        return;
    }
    // 防止会话固定攻击：先销毁旧会话
    req.getSession().invalidate();
    HttpSession session = req.getSession(true);
    session.setAttribute("userId", userOpt.get().getUserId());
    setFlash(req, "success", "Welcome back, " + userOpt.get().getName() + "!");
    redirect(req, resp, "/dashboard");
}
```

关键点：
- 成功登录前先销毁旧 Session，再创建新 Session 存储 `userId`
- 失败时统一返回通用错误，不泄露账号是否存在
- 失败后保留用户名，并重新填充 demo 用户、账号统计等页面数据

**2. 注册多层校验** — `RegisterServlet.java` + `UserService.registerUser()`

```java
ActionResult result = services(req).userService().registerUser(
        username, password, confirmPassword, role, name, email,
        services(req).idCounterRepository());
if (!result.isSuccess()) {
    req.setAttribute("error", result.getMessage());
    req.setAttribute("username", username);
    req.setAttribute("name", name);
    req.setAttribute("email", email);
    req.setAttribute("selectedRole", role);
    req.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(req, resp);
    return;
}
```

验证链覆盖用户名格式、用户名唯一性、显示名称唯一性、密码长度、密码确认、Admin 注册封锁、邮箱格式和文本清洗。成功后通过 `IdCounterRepository` 生成新用户 ID，并将 PBKDF2 密码哈希写入 `data/users/{userId}.json`。

**3. 演示用户快选面板** — `LoginServlet.java`

`populateLoginView()` 从用户仓储读取全部账号，组合 3 个 TA、2 个 MO、1 个 Admin 作为演示账号，同时向 JSP 注入 `demoPassword`、`totalUserCount`、`taCount`、`moCount` 和 `adminCount`，由 `login.jsp` 与 `app.js` 完成自动填表。

**4. 首页统计与快速登录** — `HomeServlet.java`

`HomeServlet` 每次请求实时统计 TA/MO/Admin 数量、岗位数和申请数，并为首页 Quick Sign-In 卡片挑选代表账号，确保首页展示内容跟当前 JSON 数据保持一致。

---

### 2.2 Tianyu Zhao — TA 仪表盘与申请与简历

**负责文件**: `UpdateProfileServlet.java`, `UploadCvServlet.java`, `DownloadCvServlet.java`, `ApplyServlet.java`, `WithdrawApplicationServlet.java`, `UserService.java`, `dashboard-ta.jsp`, CSS/JS

**对应 Backlog**: #2 TA Profile, #5 TA 申请, #6 上传 CV, #9 查看申请状态

#### 功能介绍（演示说明）

**TA 仪表盘** (`/dashboard` — TA 视图)
- **顶部概览区**：欢迎语 + KPI 卡片，显示当前工作量工时、活跃申请数、Profile 完成度百分比
- **最佳推荐聚焦**：突出显示匹配度最高的岗位及评分标签（如 "Strong Fit 82%"）
- **Profile 管理表单**：内联编辑姓名、学号、邮箱、专业、技能（逗号/分号/换行分隔）、可用时间、经验、CV 文本
- **CV 上传区**：点击上传 CV 文件（PDF/DOC/DOCX/TXT，最大 5MB），重新上传时自动删除旧 CV
- **申请历史表格**：展示所有已提交申请的岗位名称、状态标签（Submitted/Shortlisted/Accepted/Rejected/Withdrawn）、推荐评分、提交时间
- **推荐岗位网格**：可搜索/排序的岗位卡片列表，每张卡片包含综合匹配百分比、匹配/缺失技能标签、6 维进度条、可读的推荐理由、申请/查看详情按钮

**申请岗位** (`/apply`)
- 可从仪表盘或岗位详情页一键申请。提交前验证：Profile 完整、岗位 OPEN 且未过期、配额未满、无重复申请。提交时实时计算并保存推荐评分。

**撤回申请** (`/applications/withdraw`)
- 适用于 SUBMITTED 或 SHORTLISTED 状态的申请，设为 WITHDRAWN 后可重新申请。

**Profile 更新** (`/profile/update`)
- 输入清洗：去除 HTML 标签和控制字符，限制最大长度。验证邮箱格式。技能支持逗号/分号/换行解析。

**CV 上传** (`/profile/cv/upload`)
- 白名单：pdf/doc/docx/txt。大小限制 5MB。存储为 `{userId}_cv.{ext}`，旧 CV 自动删除。

**CV 下载** (`/cv/download`)
- 角色访问控制：Admin 可下载任何 CV；TA 只能下载自己的；MO 只能下载申请了自己岗位的候选人 CV。

**演示路径**：登录 `alice.ta` → 看到 TA 仪表盘的推荐岗位和 KPI 卡片 → 滚动到 Profile 区域编辑（添加技能如 "Python"）→ 上传一份 CV 文件 → 点击推荐岗位的 "Apply" 按钮 → 在申请历史表中看到新申请 → 点击 "Withdraw" 演示撤回功能。

#### 核心实现

**Profile 更新与输入清洗** — `UserService.java` 第51-97行

```java
public ActionResult updateTaProfile(UserProfile actor, String name,
        String studentId, String email, String programme,
        String rawSkills, String availability, String experience, String cvText) {
    if (actor.getRole() != UserRole.TA)
        return ActionResult.failure("Only TA accounts can update TA profiles.");
    name = cleanText(name, 100);  // 去除HTML标签、控制字符、限制长度
    email = cleanText(email, 150);
    // 邮箱格式验证
    if (email != null && !email.isEmpty()
            && !email.matches("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$"))
        return ActionResult.failure("Please provide a valid email address.");
    // 技能解析：支持逗号、分号、换行分隔
    List<String> skills = Arrays.stream(rawSkills.split("[,;\\n]+"))
            .map(String::trim).filter(s -> !s.isEmpty())
            .map(s -> cleanText(s, 80)).toList();
    // ... 设置所有字段并保存
    save(actor);
    return ActionResult.success("Profile updated successfully.");
}
```

**CV 上传安全控制** — `UploadCvServlet.java` 第28-67行：白名单校验（pdf/doc/docx/txt）→ 5MB 大小限制 → 删除旧 CV → 保存新 CV（路径标准化防遍历）→ 更新元数据。

**CV 下载权限控制** — `DownloadCvServlet.java`：Admin 全权限；本人可下载自己的；MO 仅能下载申请了自己岗位的候选人 CV。

---

### 2.3 Jie Ren — MO 仪表盘与岗位管理

**负责文件**: `CreateJobServlet.java`, `UpdateJobServlet.java`, `ChangeJobStatusServlet.java`, `JobService.java`, `dashboard-mo.jsp`, `data/jobs/`

**对应 Backlog**: #3 MO 发布岗位, #18 编辑/关闭岗位, #7 查看候选人

#### 功能介绍（演示说明）

**MO 仪表盘** (`/dashboard` — MO 视图)
- **KPI 卡片**：开放岗位数、总申请数、入围数、已接受数
- **岗位概览卡片**：每个自有岗位显示模块代码、标题、状态（Open/Closed）、截止日期、配额、每周工时
- **创建新岗位表单**：标题（必填，最长 200 字符）、模块代码（必填）、截止日期（必须为未来日期）、配额（正整数）、每周工时（正整数）、必需技能（至少 1 个）、优选技能（可选）、描述（必填）。所有文本字段自动清除 `<>` 标签防 XSS。
- **候选人队列**：每个岗位的申请人表格，显示姓名、技能、当前工作量工时、推荐评分百分比和解释摘要、状态下拉菜单（Submitted/Shortlisted/Accepted/Rejected）

**创建岗位** (`/jobs/create`)
- MO 填写表单后系统验证所有字段，生成唯一岗位 ID，保存为 JSON，并写入审计日志。

**编辑岗位** (`/jobs/update`)
- 仅岗位所有者可编辑。配额不能低于已接受申请数。编辑后如截止日期已过或配额已满则自动关闭。

**关闭/重开岗位** (`/jobs/status`)
- 关闭将状态设为 CLOSED。重开前验证截止日期未过且配额未满。所有状态变更写入审计日志。

**自动状态同步**
- 每次查询岗位列表时，系统自动检查所有 OPEN 岗位：过期或满额的自动设为 CLOSED，保证数据一致性。

**演示路径**：登录 `mo.chen` → 看到 MO 仪表盘的岗位卡片和 KPI 统计 → 滚动到"创建新岗位"表单 → 填入标题 "Python Lab Assistant"、模块 "EBU6304"、截止日期、配额 3、技能 "Python, Testing" → 提交 → 看到新岗位卡片出现 → 点击某个岗位查看候选人 → 用下拉菜单接受/拒绝 → 点击 "Close Job" 演示状态切换。

#### 核心实现

**岗位创建** — `JobService.java` 第80-128行

```java
public ActionResult createJob(UserProfile actor, String title,
        String moduleCode, String deadlineStr, String quotaStr,
        String workloadStr, String requiredSkillsRaw,
        String preferredSkillsRaw, String description) {
    if (actor.getRole() != UserRole.MO)
        return ActionResult.failure("Only Module Organisers can create jobs.");
    ActionResult validation = validateJobInput(title, moduleCode,
            deadlineStr, quotaStr, workloadStr, requiredSkillsRaw, description);
    if (!validation.success()) return validation;
    String jobId = idCounterRepository.nextId("job", "J");
    // 构建 JobPosting 对象，设置所有字段
    jobRepository.save(job);
    auditRepository.log("CREATE_JOB", actor.getUserId(), jobId);
    return ActionResult.success("Job '" + title + "' created successfully.");
}
```

**输入验证引擎** — `JobService.java` 第268-312行：必填校验、日期验证（不能在过去）、数值验证（正整数）、技能验证（至少1个）、XSS 防护（`cleanText()` 去除 `<>` 标签）。

**自动状态同步** — `JobService.java` 第245-260行：每次查询 `listAllJobs()` 时检查所有 OPEN 岗位，过期或配额已满的自动设为 CLOSED。

---

### 2.4 Haopeng Jin — 推荐引擎、申请服务与 Sprint 4 安全/高可用加固

**负责文件**: `RecommendationService.java`(626行), `ApplicationService.java`(415行), `AppServlet.java`, `LogoutServlet.java`, `UserService.java`, `JsonFileStore.java`, `IdCounterRepository.java`, `HealthServlet.java`, `PasswordHasher.java`, `PdfShareService.java`, `SharedPdfServlet.java`, `JobDetailServlet.java`, `UpdateApplicationStatusServlet.java`, `DashboardServlet.java`(搜索/过滤逻辑), `JobRecommendation.java`, `job-detail.jsp`, `dashboard-ta.jsp`(搜索 UI), `dashboard-mo.jsp`(搜索 UI), `DownloadCvServlet.java`(文件名优化), `data/applications/`

**对应 Backlog**: #4 浏览岗位, #8 接受/拒绝, #14 搜索过滤, #16 实时申请人数, #19 AI 技能匹配, Sprint 4 安全与高可用加固

#### 功能介绍（演示说明）

**Sprint 5 服务器部署、PDF 永久链接与并发生产化准备**：
- **公网访问地址**：`http://RecruitAssistGroup38.21.214.216.122.nip.io:8080/home`。部署过程记录在 `homework/plan/服务器端指令.md`，说明了如何在服务器上显式设置 `JAVA_HOME=/usr/lib/jvm/java-17-openjdk`，解决系统默认 Java 8 与 Jetty 12 不兼容的问题。
- **服务器启动方式**：通过 `nohup env JAVA_HOME=... PORT=8080 ./scripts/start-maven-jetty.sh > logs/server/jetty.log 2>&1 &` 后台启动，PID 写入 `logs/server/jetty.pid`，日志写入 `logs/server/jetty.log`。
- **发布包与数据校验**：发布目录为 `homework/releases/server/RecruitAssist-server-v4.0.0/`，归档为 `RecruitAssist-server-v4.0.0.tar.gz`。包内包含 WAR、源码模块、`data/`、`logs/`、文档、启动脚本和 `DATA_MANIFEST.json`，用于确认用户、岗位、申请、CV、系统配置文件完整。
- **健康检查**：部署后通过 `/health` 返回 JSON 状态，例如 `{"status":"UP","users":99,"jobs":67,"applications":965}`，用于确认服务器正确读取发布包数据而不是误读其他目录。
- **永久受保护 PDF token 分享**：`PdfShareService.java` 为每个 CV 所有者生成稳定随机 token；`SharedPdfServlet.java` 通过 `/pdf/share?token=...` 返回内联 PDF，但仍校验登录状态、TA 本人、MO 岗位归属或 Admin 角色，避免把 `data/cv/` 暴露成公开静态目录。
- **并发与高可用贡献延伸**：延续 Sprint 4 中的 CSRF 校验、POST logout、注册同步锁、跨进程文件锁、原子写入、坏 JSON 隔离读取和 `scripts/load_test_recruitassist.py`，并通过真实服务器部署验证 Java 17/Jetty 12 运行链路。

**Sprint 4 安全与高可用加固**：
- 在 `AppServlet` 中补齐全站 CSRF 防护：所有 POST 表单携带 session token，缺失或不匹配时返回 HTTP 403。
- 将 logout 从 GET 链接改为 POST 表单，避免爬虫、预览请求或恶意链接触发登出状态变更。
- 在 `UserService.registerUser()` 中加入同步临界区，使“用户名唯一性检查 + 用户创建”在并发注册时保持原子性。
- 加固文件持久化层 `JsonFileStore`：JSON/CSV 写入前通过同级 `.lock` 文件和 `FileChannel.lock()` 串行化，写失败时清理临时文件，目录读取遇到损坏 JSON 时跳过单个文件而不是让整页失败。
- `IdCounterRepository` 的计数器读/更新/写入被包在跨进程文件锁中，降低多本地服务实例共享同一 `data/` 目录时的重复 ID 风险。
- 新增永久受保护 PDF token 分享：`PdfShareService` 为每个 CV 所有者生成稳定随机 token，`SharedPdfServlet` 在返回内联 PDF 前仍校验登录状态、TA 本人访问、MO 岗位归属或 Admin 角色。
- 保留 `/health` 作为可用性探针，方便部署和负载均衡实验在转发流量前检查文件存储是否可读。

**全角色搜索与过滤系统** — v3.0.3 新增

TA 仪表盘搜索（`/dashboard` — TA 视图）：
- **关键词搜索**：在搜索框输入文字，会匹配岗位标题、模块代码、描述、必需技能、优选技能、已匹配技能和缺失技能。后端实现在 `DashboardServlet.matchesRecommendationQuery()` 中，将所有字段拼接为一个可搜索字符串后用 `contains()` 匹配。
- **技能过滤**：输入逗号分隔的技能名（如 "Python, Java"），系统匹配岗位的 required/preferred skills。只要任一技能命中就显示该岗位。后端实现在 `DashboardServlet.matchesSkillFilter()` 中。
- **最大工时过滤**：输入数字（如 8），只显示每周工时 ≤ 该值的岗位。后端通过 `job.getWorkloadHours() <= maxHours` 过滤。
- **截止日期过滤**：输入日期（yyyy-mm-dd 格式），只显示截止日期在该日期之前的岗位。后端通过 `LocalDate.parse(job.getDeadline()).isBefore(deadlineBefore)` 过滤。
- **排序选项**：最佳匹配优先（默认）/ 截止日期优先 / 工作量最低优先。
- **清除过滤**："Clear filters" 按钮重置所有过滤条件，回到完整推荐列表。

MO 仪表盘搜索（`/dashboard` — MO 视图）：
- **搜索框**：输入关键词匹配岗位标题、模块代码、技能、描述。后端实现在 `DashboardServlet.matchesMoSearch()` 中。
- **候选人表格增强**：新增 CV 下载列和 programme 信息显示。

Admin 仪表盘搜索（`/dashboard` — Admin 视图）：
- **搜索范围扩展**：除模块代码和标题外，现在也匹配技能关键词。后端在 `DashboardServlet.matchesAdminFilter()` 中扩展了搜索范围。

**CV 下载文件名优化**：
- 下载的 CV 文件自动命名为 `{姓名}_{模块代码}.{扩展名}`（如 `Alice_Zhang_EBU6304.pdf`），而非原始的 `U1001_cv.pdf`，方便 MO 识别和归档。

**演示路径**：登录 `alice.ta` → 在搜索框输入 "Python" → 看到只显示含 Python 技能的岗位 → 在技能过滤输入 "Java, Testing" → 结果进一步收窄 → 设置最大工时 6 → 只保留轻量岗位 → 点击 "Clear filters" 重置 → 再按截止日期排序查看。

**推荐引擎** — RecruitAssist 的核心创新
- 对每个 TA-岗位配对进行 **6 个维度的加权评分**，权重从 `config.json` 配置：
  - **技能匹配（40%）**：必需技能覆盖率 ×0.72 + 优选 ×0.18 + 广度加分 + 全覆盖加分 − 缺失惩罚。三级匹配：(1) 规范化精确匹配 → (2) Jaccard 相似度 ≥0.55 模糊匹配 → (3) Token 包含匹配。含技能别名归一化（如 "OOP" ↔ "Object Oriented Programming"，8 组别名）。
  - **经验证据（18%）**：基准 0.3 + 短语覆盖率、Token 重叠度、证据关键词命中（lab, marking, debugging 等 16 个关键词）。
  - **时间可用性（12%）**：基准 0.34 + weekday/weekend、具体星期、时间段、灵活性关键词加分。
  - **工作量平衡（12%）**：未超阈值 → 0.55 + 剩余比例 ×0.45。超出 → 按超出幅度惩罚。促进公平分配。
  - **Profile 证据（10%）**：5 字段完整度（58%）+ 岗位对齐度（22%）+ 技能广度信号（20%）。
  - **竞争压力（8%）**：每剩余名额的活跃申请人比率。≤1/名额 → 0.95；更高则递减。
- **可解释性**：每个维度生成自然语言解释，展示在 TA 仪表盘和岗位详情页。

**岗位详情页** (`/jobs/detail`)
- **TA 视图**：展示推荐匹配快照（6 维进度条、匹配/缺失技能、解释理由）+ Apply/Withdraw 按钮
- **MO 视图**：岗位编辑表单 + 候选人表格（支持按 score/workload/submitted/status 多维排序和状态过滤）+ 状态更新下拉
- **Admin 视图**：只读概览 + 候选人统计
- 实时显示申请人数、已接受/配额比、剩余名额

**申请状态管理** (`/applications/status`)
- MO 可执行：SUBMITTED → SHORTLISTED/ACCEPTED/REJECTED，SHORTLISTED → ACCEPTED/REJECTED
- 最后一个名额被 ACCEPTED 填满时岗位自动关闭
- 所有状态转换写入审计日志

**多维候选人排序**
- 排序选项：score（默认，4 级比较器）、workload（升序）、submitted（最新优先）、status（优先级：Accepted > Shortlisted > Submitted > Rejected > Withdrawn）

**演示路径**：登录 `alice.ta` → 在仪表盘看到推荐岗位的匹配百分比 → 点击 "View Detail" 进入岗位详情 → 看到 6 维进度条和解释理由 → 申请该岗位 → 登出 → 登录 `mo.chen` → 进入该岗位详情 → 看到候选人按推荐分排序 → 用下拉菜单接受排名最高的候选人 → 观察配额计数器更新 → 配额满时岗位自动关闭。

#### 核心实现

**六维推荐评分** — `RecommendationService.java` 第103-180行

```java
public JobRecommendation recommend(UserProfile user, JobPosting job) {
    SkillProfile skillProfile = buildSkillProfile(user);
    Set<String> profileTokens = tokenize(buildProfileText(user));
    // 六维评分
    double skillScore = calculateSkillScore(...);       // 第211-224行
    double availScore = calculateAvailabilityScore(...); // 第226-261行
    double expScore = calculateExperienceScore(...);     // 第263-292行
    double profileEvidence = calculateProfileEvidenceScore(...); // 第294-320行
    double workloadBalance = calculateWorkloadBalance(...);      // 第322-334行
    CompetitionSnapshot competition = calculateCompetition(...); // 第336-364行
    // 加权平均（权重从配置文件读取）
    double score = (skillScore * cfg.getSkillMatchWeight()
            + availScore * cfg.getAvailabilityWeight()
            + expScore * cfg.getExperienceWeight()
            + workloadBalance * cfg.getWorkloadBalanceWeight()
            + profileEvidence * cfg.getProfileEvidenceWeight()
            + competition.score() * cfg.getCompetitionWeight()) / totalWeight;
    List<String> reasons = buildReasons(...); // 生成自然语言解释
    return new JobRecommendation(job, score, ...);
}
```

**三级技能匹配** — `RecommendationService.java` 第196-209行

```java
private boolean matchesSkill(SkillProfile profile, String jobSkill,
        Set<String> profileTokens) {
    String canonJob = canonicalizeSkill(jobSkill);
    // 第1级：规范化精确匹配
    if (profile.canonicalSkills().contains(canonJob)) return true;
    // 第2级：Jaccard 相似度 >= 0.55 模糊匹配
    Set<String> jobTokens = tokenize(canonJob);
    for (String declaredSkill : profile.declaredSkills()) {
        if (jaccard(jobTokens, tokenize(canonicalizeSkill(declaredSkill))) >= 0.55)
            return true;
    }
    // 第3级：Token 全包含匹配
    return profileTokens.containsAll(jobTokens);
}
```

**申请提交状态机** — `ApplicationService.java` 第164-212行：角色校验 → Profile 完整性校验 → 岗位有效性校验（OPEN+未过期+未满额）→ 去重检查 → 推荐引擎计算评分 → 创建记录 → 审计日志。

**多维候选人排序** — `ApplicationService.java` 第291-324行：支持按 score（默认）/ workload / submitted / status 排序，每种排序都有多级比较器。

---

### 2.5 Zhuang Hou — 管理员仪表盘与工作量监控

**负责文件**: `DashboardServlet.java`(224行), `WorkloadService.java`, `WorkloadEntry.java`, `SystemConfig.java`, `dashboard-admin.jsp`, `data/system/config.json`

**对应 Backlog**: #10 Admin 总览, #14 搜索过滤, #15 历史记录, #21 工作量监控

#### 功能介绍（演示说明）

**Dashboard 路由** (`/dashboard`)
- `DashboardServlet` 是中央路由枢纽：认证后根据用户角色分发到 `renderTaDashboard()`、`renderMoDashboard()` 或 `renderAdminDashboard()`。三种角色共用同一个 `/dashboard` URL 但看到完全不同的页面。

**Admin 仪表盘** (`/dashboard` — Admin 视图)
- **KPI 卡片**：跟踪的 TA 数、近期申请数、策略工作量阈值（默认 12h/周）、筛选后岗位数
- **招聘概览表格**：列出系统内所有岗位，可按状态（Open/Closed）过滤、按模块代码或标题搜索。列：岗位标题、负责人（MO 名字）、截止日期、申请人数、已接受/配额比、状态标签
- **TA 工作负载表**：列出所有 TA 的已接受工时、活跃申请数、"均衡"/"超出阈值"指示器。按工时降序排列以突出可能过载的 TA。阈值可在 `config.json` 中配置（默认 12 小时/周）。
- **最新申请列表**：系统范围内最新 10 条申请，显示申请人、岗位、状态、推荐分、提交时间

**工作量监控**
- `WorkloadService` 计算每个 TA 的总工作量：遍历所有 ACCEPTED 申请，累加对应岗位的 `workloadHours`
- `buildEntries()` 为 Admin 仪表盘生成排序后的 `WorkloadEntry` 列表，每个条目包含：用户 Profile、已接受工时、活跃申请数、是否超载标志

**TA 仪表盘搜索过滤**（也在 `DashboardServlet` 中实现）
- TA 视图支持按岗位标题、模块代码、描述、技能、匹配技能的关键词搜索
- 排序选项：推荐分数（默认）、截止日期、工作量影响

**演示路径**：登录 `admin.sarah` → 看到 Admin 仪表盘的 KPI 卡片 → 浏览招聘概览表格 → 按 "Open" 状态过滤 → 搜索特定模块代码 → 滚动到 TA 工作负载表检查是否有 TA "超出阈值" → 查看最新申请列表。

#### 核心实现

**角色路由分发** — `DashboardServlet.java` 第25-45行

```java
protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
    UserProfile user = requireAuthenticatedUser(req, resp);
    if (user == null) return;
    switch (user.getRole()) {
        case TA    -> renderTaDashboard(req, resp, user);
        case MO    -> renderMoDashboard(req, resp, user);
        case ADMIN -> renderAdminDashboard(req, resp, user);
    }
}
```

**工作量计算算法** — `WorkloadService.java` 第48-66行

```java
public Map<String, Integer> workloadByUserId() {
    Map<String, Integer> result = new HashMap<>();
    Map<String, JobPosting> jobIndex = jobService.indexById();
    for (ApplicationRecord app : applicationRepository.findAll()) {
        if (app.getStatus() != ApplicationStatus.ACCEPTED) continue;
        JobPosting job = jobIndex.get(app.getJobId());
        if (job == null) continue;
        result.merge(app.getApplicantId(), job.getWorkloadHours(), Integer::sum);
    }
    return result;
}
```

**工作量条目构建** — `WorkloadService.java` 第68-79行：遍历所有 TA，计算每人已接受工时、活跃申请数、是否超阈值，按工时降序排列。

---

### 2.6 Zexuan Dong — 数据层与基础设施与测试

**负责文件**: `JsonFileStore.java`(174行), `AppPaths.java`, `AppServices.java`, `AppBootstrapListener.java`, `AppContextKeys.java`, `AppServlet.java`, `AuthService.java`, 全部 Repository (6个), 全部 Model (8个), `AppPathsTest.java`, `scripts/`

**对应 Backlog**: #11 JSON 持久化, #12 注册与认证, #17 隐私与权限, #24 测试

#### 功能介绍（演示说明）

**JSON 数据持久化层** (`JsonFileStore.java`)
- 所有数据以 JSON/CSV/TXT 文件存储，无需数据库。`JsonFileStore` 是核心持久化引擎，提供：
  - **两级缓存**：文件级缓存（key = 路径 + 修改时间 + 文件大小）和目录级缓存。缓存命中时完全避免磁盘 I/O。
  - **路径级读写锁**：每个文件路径通过 `ConcurrentHashMap.computeIfAbsent()` 获得独立的 `ReentrantReadWriteLock`，实现细粒度并发 — 不同文件可同时被多个读者访问。
  - **原子写入**：数据先写入 `.tmp` 临时文件，再通过 `Files.move(ATOMIC_MOVE)` 原子移动到目标路径，防止部分写入导致数据损坏。
  - **自动缓存失效**：写操作后移除文件缓存，并按前缀匹配失效父目录缓存。

**智能路径解析** (`AppPaths.java`)
- 系统自动检测项目根目录，优先级：(1) Java 系统属性 `recruitassist.baseDir` → (2) 环境变量 `RECRUITASSIST_BASE_DIR` → (3) 从 CWD 自动检测（若在 `framework/recruitassist-web` 下则自动上溯两级）。无论从哪里执行 `mvn` 都能正确运行。

**依赖注入容器** (`AppServices.java`)
- 启动时 `AppBootstrapListener` 初始化 `AppServices` 单例：创建 `JsonFileStore` → 6 个 Repository → 6 个 Service，按依赖顺序注入。所有 Service 通过构造函数注入依赖。

**认证服务** (`AuthService.java`)
- 接收用户名和密码，去除空白后按用户名查找用户，验证 PBKDF2 密码哈希并兼容旧演示明文种子数据；连续失败会临时锁定用户名键。返回 `Optional<UserProfile>` — 失败为空，成功返回用户对象。

**数据结构**：`data/users/`（用户 Profile JSON）、`data/jobs/`（岗位 JSON）、`data/applications/`（申请记录 JSON）、`data/cv/`（上传的 CV 文件）、`data/system/config.json`（系统配置）、`data/system/id-counters.json`（自增 ID 计数器）、`logs/access/audit.csv`（审计日志）。

**审计日志** (`AuditRepository.java`)
- 每次写操作（创建岗位、提交申请、更新状态等）追加一行到 `audit.csv`，包含时间戳、操作类型、用户 ID、目标 ID。

**演示路径**：数据层对终端用户不可见但驱动一切。演示方式：展示 `data/` 目录结构 → 打开一个用户 JSON 文件展示 schema → 执行一个操作（如申请岗位）→ 展示 `data/applications/` 中新创建的申请 JSON 文件 → 展示 `logs/access/audit.csv` 中的审计日志条目。

#### 核心实现

**带缓存的 JSON 文件存储** — `JsonFileStore.java` 第65-126行

```java
// 读取：两级缓存（文件级 + 目录级）
public <T> T read(Path file, Type type) {
    rwLock.readLock().lock();
    try {
        FileCacheEntry cached = fileCache.get(file);
        long modifiedAt = Files.getLastModifiedTime(file).toMillis();
        long size = Files.size(file);
        if (cached != null && cached.modifiedAt() == modifiedAt
                && cached.size() == size) return (T) cached.value(); // 缓存命中
        T value = gson.fromJson(Files.readString(file), type); // 缓存未命中
        fileCache.put(file, new FileCacheEntry(type, modifiedAt, size, value));
        return value;
    } finally { rwLock.readLock().unlock(); }
}

// 写入：原子操作（先写临时文件再原子移动）
public void write(Path file, Object value) {
    rwLock.writeLock().lock();
    try {
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temp, gson.toJson(value));
        Files.move(temp, file, REPLACE_EXISTING, ATOMIC_MOVE); // 原子移动
        fileCache.remove(file);
        invalidateDirectoryCache(file.getParent());
    } finally { rwLock.writeLock().unlock(); }
}
```

**智能路径解析** — `AppPaths.java` 第16-42行：优先级 Java 系统属性 > 环境变量 > CWD 自动检测（若在 `framework/recruitassist-web` 下则自动上溯两级）。

**依赖注入容器** — `AppServices.java` 第22-68行：初始化 `JsonFileStore` → 6 个 Repository → 6 个 Service，按依赖顺序注入。

---

## 3. 横切关注点

### Flash 消息系统（全 Servlet 共用）

`AppServlet.java` 第58-80行：`setFlash()` 写入 Session，`moveFlashToRequest()` 一次性消费后移除。

### 审计日志

`AuditRepository.java` 第18-25行：每次写操作记录 `时间戳,操作,用户ID,目标ID` 到 `logs/access/audit.csv`。

---

## 4. 产品待办项覆盖表

| # | Story | 状态 | 主要实现者 |
|---|-------|------|----------|
| 1 | 用户登录与角色访问 | ✅ 完成 | Yi Qi |
| 2 | TA Profile 创建与编辑 | ✅ 完成 | Tianyu Zhao |
| 3 | MO 发布 TA 岗位 | ✅ 完成 | Jie Ren |
| 4 | 浏览可用 TA 岗位 | ✅ 完成 | Haopeng Jin |
| 5 | TA 申请岗位 | ✅ 完成 | Tianyu Zhao |
| 6 | TA 上传 CV | ✅ 完成 | Tianyu Zhao |
| 7 | MO 查看候选人与 CV | ✅ 完成 | Jie Ren + Haopeng Jin |
| 8 | MO 接受/拒绝申请 | ✅ 完成 | Haopeng Jin |
| 9 | TA 查看申请状态 | ✅ 完成 | Tianyu Zhao |
| 10 | Admin 总览仪表盘 | ✅ 完成 | Zhuang Hou |
| 11 | JSON 数据持久化层 | ✅ 完成 | Zexuan Dong |
| 12 | 用户注册与认证 | ✅ 完成 | Yi Qi（RegisterServlet、AuthService）+ Zexuan Dong（基础设施） |
| 13 | 响应式一致 UI | ✅ 完成 | Yi Qi + Tianyu Zhao |
| 14 | TA 岗位搜索过滤 | ✅ 完成 | Zhuang Hou |
| 15 | Admin 历史记录 | ✅ 完成 | Zhuang Hou |
| 16 | 实时申请人数 | ✅ 完成 | Haopeng Jin |
| 17 | 隐私与角色访问控制 | ✅ 完成 | Zexuan Dong + Tianyu Zhao |
| 18 | MO 编辑/关闭岗位 | ✅ 完成 | Jie Ren |
| 19 | AI 技能匹配与排序 | ✅ 完成 | Haopeng Jin |
| 20 | AI 缺失技能识别 | ✅ 完成 | Haopeng Jin |
| 21 | 工作量监控与预警 | ✅ 完成 | Zhuang Hou |
