# RecruitAssist — 组员分工与代码实现报告

> **版本**: v3.0.0 (Sprint 3) | **日期**: 2026年4月 | **课程**: EBU6304 软件工程 — 第38组

---

## 1. 团队概览

| 成员 | 主要职责 | 核心文件 | 代码量（约） |
|------|---------|---------|------------|
| Yi Qi | 登录与首页与 UI | 7 个文件 | ~500 行 |
| Tianyu Zhao | TA 仪表盘与简历与申请 | 8 个文件 | ~700 行 |
| Jie Ren | MO 仪表盘与岗位 CRUD | 6 个文件 | ~700 行 |
| Haopeng Jin | 推荐引擎与申请服务 | 7 个文件 | ~1,500 行 |
| Zhuang Hou | 管理员仪表盘与工作量 | 6 个文件 | ~500 行 |
| Zexuan Dong | 数据层与基础设施 | 15 个文件 | ~1,200 行 |

---

## 2. 各成员贡献详情

### 2.1 Yi Qi — 登录与首页与 UI 资产

**负责文件**: `LoginServlet.java`, `LogoutServlet.java`, `HomeServlet.java`, `login.jsp`, `home.jsp`, `index.jsp`, README 与架构图

**对应 Backlog**: #1 用户登录与角色访问, #13 响应式 UI

#### 功能介绍（演示说明）

**登录页面** (`/login`)
- 基于用户名 + 密码的表单认证，使用 HttpSession 管理会话状态
- 登录页面展示**演示用户快速选择面板**，按角色分组显示最多 3 个 TA、2 个 MO、1 个 Admin 账号，方便快速登录
- 登录成功后先销毁旧会话再创建新会话（防止会话固定攻击）
- Flash 消息系统显示成功/错误反馈（如 "Welcome back, Alice!"）
- 登录失败仅显示通用错误 "Invalid username or password"，不暴露具体是用户名还是密码错误

**登出** (`/logout`)
- 销毁当前会话，创建新会话并写入"已登出"提示，重定向到登录页

**首页** (`/home`)
- 未登录用户的公开着陆页
- 实时展示系统统计数据：TA 总数、MO 总数、Admin 总数、岗位数、申请数
- 已登录用户自动重定向到对应角色的仪表盘

**演示路径**：打开应用 → 看到首页的系统统计 → 点击登录 → 用快选面板选择 `alice.ta` → 输入密码 `demo123` → 跳转到 TA 仪表盘并显示欢迎提示。

#### 核心实现

**登录认证流程** — `LoginServlet.java` 第31-53行

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

**演示用户快选面板** — `LoginServlet.java` 第55-68行：按角色分组（最多3个TA + 2个MO + 1个Admin）展示在登录页。

**首页统计** — `HomeServlet.java` 第22-53行：展示 TA/MO/Admin 人数、岗位数、申请数。

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

**CV 下载权限控制** — `DownloadCvServlet.java` 第50-71行：Admin 全权限；本人可下载自己的；MO 仅能下载申请了自己岗位的候选人 CV。

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

### 2.4 Haopeng Jin — 推荐引擎与申请服务与搜索过滤

**负责文件**: `RecommendationService.java`(626行), `ApplicationService.java`(415行), `JobDetailServlet.java`, `UpdateApplicationStatusServlet.java`, `DashboardServlet.java`(搜索/过滤逻辑), `JobRecommendation.java`, `job-detail.jsp`, `dashboard-ta.jsp`(搜索 UI), `dashboard-mo.jsp`(搜索 UI), `DownloadCvServlet.java`(文件名优化), `data/applications/`

**对应 Backlog**: #4 浏览岗位, #8 接受/拒绝, #14 搜索过滤, #16 实时申请人数, #19 AI 技能匹配

#### 功能介绍（演示说明）

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
- 接收用户名和密码，去除空白后按用户名查找用户并比对密码。返回 `Optional<UserProfile>` — 失败为空，成功返回用户对象。（当前为演示目的使用明文比对。）

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
| 12 | 用户注册与认证 | ✅ 完成 | Zexuan Dong |
| 13 | 响应式一致 UI | ✅ 完成 | Yi Qi + Tianyu Zhao |
| 14 | TA 岗位搜索过滤 | ✅ 完成 | Zhuang Hou |
| 15 | Admin 历史记录 | ✅ 完成 | Zhuang Hou |
| 16 | 实时申请人数 | ✅ 完成 | Haopeng Jin |
| 17 | 隐私与角色访问控制 | ✅ 完成 | Zexuan Dong + Tianyu Zhao |
| 18 | MO 编辑/关闭岗位 | ✅ 完成 | Jie Ren |
| 19 | AI 技能匹配与排序 | ✅ 完成 | Haopeng Jin |
| 20 | AI 缺失技能识别 | ✅ 完成 | Haopeng Jin |
| 21 | 工作量监控与预警 | ✅ 完成 | Zhuang Hou |
