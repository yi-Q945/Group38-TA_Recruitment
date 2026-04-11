# RecruitAssist — 功能介绍与使用手册

> **版本**: v3.0.0 (Sprint 3 完整版)  
> **日期**: 2026年4月  
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

### 系统规模统计（v3.0.0）

| 指标 | 数量 |
|------|------|
| Java 类 | 42 |
| JSP 页面 | 7 |
| Servlet | 14（1个抽象基类 + 13个具体实现） |
| Service 服务层 | 6 |
| Repository 仓储层 | 6 |
| 演示用户 | 100+ |
| 演示岗位 | 50+ |
| 演示申请 | 500+ |

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
- Flash 消息系统用于成功/错误反馈
- 登录页面提供演示用户快速选择面板
- 登出时销毁会话确保安全

**登出** (`/logout`)
- 销毁当前会话
- 创建新会话并写入"已登出"提示
- 重定向到登录页

**首页** (`/home`)
- 未登录用户的公开着陆页
- 展示系统统计数据：TA 总数、MO 总数、Admin 总数、岗位数、申请数
- 已登录用户自动重定向到对应角色的仪表盘

### 4.2 TA（助教）功能

#### 4.2.1 仪表盘（`/dashboard` — TA 视图）

TA 仪表盘提供全方位的工作台：

- **顶部概览区**：欢迎语 + KPI 卡片（工作量、活跃申请数、Profile 完成度）
- **最佳推荐聚焦**：突出显示匹配度最高的岗位及评分标签
- **Profile 管理表单**：编辑姓名、学号、邮箱、专业、技能、可用时间、经验、CV 文本
- **CV 上传**：支持 PDF、DOC、DOCX、TXT（最大 5MB）
- **申请历史表格**：展示所有申请的状态、评分和时间戳
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

#### 4.2.4 Profile 管理（`/profile/update`）

可编辑字段：
- 姓名、学号、邮箱（格式验证）
- 专业、技能（支持逗号/分号/换行分隔）
- 可用时间、经验描述、CV 文本
- 输入清理：自动去除 HTML 标签、控制字符，限制最大长度

#### 4.2.5 CV 上传（`/profile/cv/upload`）

- 支持格式：PDF、DOC、DOCX、TXT
- 最大文件大小：5MB
- 存储为 `{userId}_cv.{扩展名}` 在 `data/cv/` 目录
- 重新上传时自动删除旧 CV
- 文件元数据（文件名、上传时间）保存到用户 Profile

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

#### 4.3.6 下载 CV（`/cv/download`）

访问控制：
- MO 只能下载申请了自己岗位的候选人 CV
- Admin 可以下载任何 CV
- TA 只能下载自己的 CV
- 返回文件流 + `Content-Disposition: attachment`

### 4.4 Admin（管理员）功能

#### 4.4.1 仪表盘（`/dashboard` — Admin 视图）

- **KPI 卡片**：跟踪的 TA 数、近期申请数、策略阈值、筛选岗位数
- **招聘概览表格**：所有岗位，可按状态（Open/Closed）和模块代码搜索
  - 列：岗位名、负责人、截止日期、申请人数、已接受/配额、状态
- **TA 工作负载表**：所有 TA 的：
  - 已接受工时、活跃申请数
  - "均衡" / "超出阈值" 指示器
- **最新申请列表**：系统范围内最新的 10 条申请记录

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
- **缓存失效**：写操作后自动清除文件缓存；目录缓存按前缀匹配失效

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
| GET/POST | `/login` | 公开 | 登录表单 / 认证 |
| GET | `/logout` | 已登录 | 结束会话 |
| GET | `/dashboard` | 已登录 | 角色化仪表盘 |
| GET | `/jobs/detail?id={jobId}` | 已登录 | 岗位详情（角色化视图） |
| POST | `/jobs/create` | MO | 创建新岗位 |
| POST | `/jobs/update` | MO | 编辑岗位 |
| POST | `/jobs/status` | MO | 关闭/重开岗位 |
| POST | `/apply` | TA | 提交申请 |
| POST | `/applications/withdraw` | TA | 撤回申请 |
| POST | `/applications/status` | MO | 接受/拒绝/入围申请 |
| POST | `/profile/update` | TA | 更新个人 Profile |
| POST | `/profile/cv/upload` | TA | 上传 CV 文件 |
| GET | `/cv/download?userId={id}` | 认证+ACL | 下载 CV 文件 |
