# RecruitAssist Sprint 4 — 网页体验与高并发评估

> 范围：`framework/recruitassist-web` 当前 Servlet/JSP 实现、角色仪表盘、核心业务 Servlet/Service，以及 Sprint 4 并发与可用性目标。

## 1. 总体判断

当前系统已经具备完整演示闭环：TA 可以浏览推荐岗位、申请、撤回、维护 Profile/CV；Recruiter/MO 可以创建岗位、查看候选人队列、更新申请状态、关闭/重开岗位；Admin 可以查看全局招聘概览、工作量和最新申请。

最值得继续优化的方向有三类：

- **体验层**：页面信息量已经很足，但部分页面密度偏高，缺少“下一步引导”“异常解释”和批量操作。
- **安全层**：已补 PBKDF2 密码哈希、登录失败锁定、Session 超时、安全响应头、CSRF、POST logout 和 `/health`；下一步可补结构化日志和更完整的审计可视化。
- **高并发层**：单 JVM 内的锁、跨进程文件写锁、原子写、申请去重已经能支撑课程级并发演示；如果要做真正生产级多实例/负载均衡，还需要共享 session、集中式存储或单写节点策略。

## 2. TA 视角评估

### 当前做得好的点

- TA Dashboard 把“当前工作量、活跃申请数、Profile 完成度、Top recommendation”放在首屏，适合演示推荐系统。
- 推荐卡展示 6 个维度分数、匹配/缺失技能、竞争压力和解释原因，能支撑 viva 中解释“为什么推荐”。
- 筛选项覆盖关键词、技能、最大工时、截止日期、排序，实际能帮助 TA 缩小岗位范围。
- 申请历史和撤回入口在同一页面，用户可以理解申请生命周期。

### 体验问题

- 推荐卡信息很完整，但视觉密度高。对普通 TA 来说，6 个进度条加多组标签容易压过“我现在应该申请哪个”的主行动。
- Profile 区域在推荐和历史之后，若 Profile 不完整，用户要滚动到后面才知道如何修复。
- “Profile evidence readiness” 是百分比，但没有直接列出缺哪几项。
- Apply 按钮提交后依赖页面跳转和 flash；高延迟下用户只看到按钮 loading，缺少更明确的“正在防重复提交”提示。

### 优化建议

- 首屏增加 “Next best action”：例如 “Update availability to improve recommendation quality” 或 “Apply to top job”。
- Profile 不完整时，把缺失字段以小 checklist 放到推荐区上方。
- 推荐卡默认只展开总分、Top 2 reasons 和 Apply/View details；6 维细节放在详情页或折叠区。
- 申请成功后在历史表中高亮最新申请 3-5 秒，降低用户不确定感。

## 3. Recruiter/MO 视角评估

### 当前做得好的点

- MO Dashboard 有岗位总览、创建岗位表单和候选人队列，流程连贯。
- 候选人队列显示推荐分、解释摘要、当前工作量、CV 下载和状态更新，评审信息比较完整。
- Job Detail 页支持编辑岗位、状态切换、候选人排序/筛选，适合 Recruiter 深度操作。
- 自动刷新能让 MO 在演示中较快看到 TA 申请和状态变化。

### 体验问题

- 创建岗位表单和候选人队列同页，页面过长；当岗位很多时，MO 需要大量滚动。
- Candidate queue 缺少批量操作，比如批量 shortlist、按缺失技能分组。
- MO Dashboard 搜索只按岗位内容匹配，页面文案写了 “Search jobs or candidates”，但后端 `matchesMoSearch()` 目前不匹配候选人姓名。
- 状态更新是每行一个下拉+按钮，操作安全，但大量候选人时效率较低。

### 优化建议

- 把 “Create Job” 改成折叠区或独立 `/jobs/new` 页面，Dashboard 优先展示运营队列。
- 修正 MO 搜索：真正把候选人姓名、programme、skills、application status 加入搜索范围。
- 给候选人队列增加 “only risky workload / missing required skills / strong fit” 快捷筛选。
- 对接收/拒绝操作增加二次确认或轻量 undo，避免误操作。

## 4. Admin 视角评估

### 当前做得好的点

- Admin Dashboard 能看到岗位、开放数量、申请人数、工作量阈值和最近申请。
- 工作量表以 TA 为中心，能展示 workload balancing 的课程亮点。
- 招聘总览支持状态和关键词筛选，适合快速定位模块/岗位。

### 体验问题

- Admin 仍偏“只读看板”，缺少系统健康、压测结果、审计日志摘要等 Sprint 4 运维视角。
- 工作量表只有 Balanced/Over threshold，缺少风险等级和趋势。
- 最新申请只显示 10 条，无法按时间/状态/模块进一步过滤。
- `/health` 已有，但没有在 Admin 页面暴露健康状态。

### 优化建议

- Admin 首屏新增 “System health” 卡片：`/health` 状态、数据目录可读、最近 audit 时间、总错误数。
- 工作量状态从二级改为三级：Balanced / Near threshold / Over threshold。
- Admin 加一个 Audit Log 面板，读取 `logs/access/audit.csv` 最近 20 条，用于展示可追踪性。
- 增加导出 CSV 功能，便于报告展示和验收截图。

## 5. 高并发与负载均衡实验设计

### 当前后端事实

- 单 JVM 内，`ApplicationService` 对提交/撤回/状态更新使用写锁，`JsonFileStore` 对文件读写使用路径级读写锁。
- JSON 写入采用临时文件 + atomic move，`IdCounterRepository` 对计数器更新加锁。
- Session 存在 `HttpSession` 内存中，所以**多实例负载均衡必须使用 sticky session**，除非引入共享 session 存储。课程限制下建议用 sticky session 或单写节点方案。
- `JsonFileStore` 已加入跨进程 `.lock` 文件写锁，能降低多 Tomcat 实例同时写同一文件的风险；但内存 Session 仍要求 sticky session 或共享 session 存储。
- 真正生产级 HA 仍建议迁移到数据库/对象存储，并配合反向代理健康检查、备份恢复和集中式日志。

### 新增压测脚本

脚本位置：`scripts/load_test_recruitassist.py`

它支持：

- 多个 `--base-url`，按 round-robin 或 random 把虚拟用户分配到不同后端实例。
- `readonly` 场景：访问 `/home` 和 `/health`，适合安全压测。
- `ta-browse` 场景：登录 TA，访问 `/dashboard` 和岗位详情，模拟真实浏览。
- `ta-apply` 场景：并发 POST `/apply`，必须显式加 `--allow-mutations`，避免误改演示数据。
- 输出总请求数、成功率、RPS、mean/p50/p95/p99/max latency，并可写 CSV/JSON 明细。

### 推荐实验矩阵

1. **基础健康压测**
   - 目标：验证首页和健康检查在高并发读流量下是否稳定。
   - 命令：
     ```bash
     python3 scripts/load_test_recruitassist.py \
       --base-url http://127.0.0.1:8081 \
       --scenario readonly \
       --concurrency 50 \
       --duration 60 \
       --out logs/build/readonly-load.csv
     ```

2. **真实 TA 浏览压测**
   - 目标：验证登录、Session、推荐计算、Dashboard 和 Job Detail 的整体延迟。
   - 命令：
     ```bash
     python3 scripts/load_test_recruitassist.py \
       --base-url http://127.0.0.1:8081 \
       --scenario ta-browse \
       --concurrency 30 \
       --duration 60 \
       --account alice.ta:demo123 \
       --account ben.ta:demo123 \
       --out logs/build/ta-browse-load.csv
     ```

3. **并发申请写入实验**
   - 目标：验证重复申请去重、写锁、ID 生成和 JSON 持久化。
   - 注意：会修改 `data/applications` 和 `logs/access/audit.csv`，建议复制一份临时 `data/` 后运行。
   - 命令：
     ```bash
     python3 scripts/load_test_recruitassist.py \
       --base-url http://127.0.0.1:8081 \
       --scenario ta-apply \
       --apply-job-id J2003 \
       --concurrency 20 \
       --duration 10 \
       --allow-mutations \
       --out logs/build/apply-load.csv
     ```

4. **多实例分流实验**
   - 目标：观察两个后端实例各自承压表现。建议不同实例使用不同数据目录，或只跑 readonly/ta-browse。
   - 命令：
     ```bash
     python3 scripts/load_test_recruitassist.py \
       --base-url http://127.0.0.1:8081 \
       --base-url http://127.0.0.1:8082 \
       --balance round-robin \
       --scenario readonly \
       --concurrency 80 \
       --duration 60 \
       --out logs/build/multi-instance-readonly.csv
     ```

## 6. 已补风险点与剩余风险

### 已补 P0 风险点

- 所有 POST 表单已补 CSRF token，统一由 `AppServlet` 校验。
- Logout 已从 GET 改为 POST，避免链接型请求触发登出。
- `UserService.registerUser()` 已加同步临界区，降低并发注册同名用户的 TOCTOU 风险。
- `JsonFileStore` 已加入跨进程文件写锁、失败临时文件清理和坏 JSON 隔离读取。
- `IdCounterRepository` 已将计数器读/改/写包在 JVM 锁和文件锁内，降低并发 ID 冲突风险。
- 压测脚本已支持 CSRF 登录流程，仍要求 `ta-apply` 场景显式传入 `--allow-mutations`，避免污染正式演示数据。

### 剩余风险与建议

- **Session HA**：当前 `HttpSession` 仍保存在单个 Tomcat 进程内，多实例负载均衡应使用 sticky session；若要生产级 HA，需要 Redis/session replication。
- **共享文件存储**：文件锁能保护同一共享目录上的写入，但不等同于数据库事务；生产环境建议迁移到数据库或单写节点。
- **备份恢复**：目前没有自动备份、快照和恢复脚本；提交/验收前应保留干净 `data/` 快照。
- **审计可视化**：已有 CSV audit trail，但 Admin 页面还未展示最近审计日志和错误统计。
- **压测证据**：脚本已完成，但需要在本地启动 Tomcat 后运行 readonly/ta-browse/ta-apply 三组实验并保存 CSV 截图或结果。
- **操作防误触**：MO 接受/拒绝、岗位关闭等高影响操作可以继续补 confirmation 或 undo。
