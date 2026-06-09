# 发布说明 - v1.0.2（首次正式上线）

**发布日期：** 2026-06-09  
**项目：** app-fit（健身应用平台）  
**仓库：** https://github.com/0815006/app-fit.git  
**提交版本：** 3059f51

---

## 概述

**app-fit** 首次正式上线，是一个综合性的健身应用平台，包含三大核心模块：

- **后端服务** (`java-fit-server`) — 基于 Spring Boot 构建的 RESTful API 服务
- **Web 前端** (`web-fit-vue`) — 基于 Vue 3 构建的 SPA 管理控制台
- **微信小程序** (`mp-fit-ts`) — 面向终端用户的移动客户端

---

## 模块详情

### 1. java-fit-server (v1.0.0)

**技术栈：** Spring Boot 3.4.3 · Java 21 · MyBatis Plus 3.5.11 · MySQL · Flyway · Apache POI 5.3.0

#### 核心依赖
- Spring Boot Starter Web、Actuator、Validation
- MyBatis Plus（plus-boot3-starter、extension、jsqlparser 4.9）
- MySQL Connector/J
- Flyway（flyway-mysql）数据库迁移管理
- Apache POI（poi-ooxml）Excel 文件解析
- Lombok

#### 数据库迁移
- 通过 Flyway 管理，迁移脚本位于 `db/migration/` 目录下
- 支持版本化的数据库结构演进

---

### 2. web-fit-vue (v1.0.0)

**技术栈：** Vue 3.5 · TypeScript 5.7 · Vite 6.1 · Element Plus 2.9 · Vue Router 4.5 · Axios 1.7

#### 页面 / 视图
| 视图 | 说明 |
|------|------|
| `HomeView` | 仪表盘 / 主页 |
| `WorkoutView` | 训练管理 |
| `GymActionLibraryView` | 健身动作库浏览 |
| `CanteenMenuView` | 食堂菜单管理 |
| `HealthView` | 健康数据概览 |

#### API 模块 (`src/api/`)
- `canteenMenu` — 食堂菜单 CRUD
- `gymAction` — 健身动作管理
- `gymActionEquipmentRel` — 动作与器械关联关系
- `gymActionMuscleRel` — 动作与肌肉关联关系
- `gymActionRecommendation` — 动作推荐引擎
- `gymEquipment` — 健身器械目录
- `gymMuscle` — 肌肉群目录
- `loginRecord` — 用户登录记录
- `system` — 系统级接口

#### 组件
- 布局组件 (`components/layout/`)
- 用户相关组件 (`components/user/`)

#### 工具函数
- `request.ts` — 基于 Axios 的 HTTP 客户端封装（含拦截器）
- `currentUser.ts` — 当前用户状态管理

---

### 3. mp-fit-ts (v1.0.0)

**技术栈：** 微信原生小程序 · TypeScript · TDesign Miniprogram 1.9

#### 页面
- `pages/index/` — 首页
- `pages/action-detail/` — 健身动作详情页

#### 工具函数
- `utils/request.js` — 小程序 HTTP 请求辅助工具

---

## 已交付功能

### 后端
- 基于 Spring Boot 的 RESTful API 服务
- 通过 MyBatis Plus 与 MySQL 的数据库集成
- 基于 Flyway 的数据库版本管理
- 支持 Excel 文件解析（Apache POI）
- 服务健康监控（Actuator）

### Web 前端
- 仪表盘及多页面 SPA
- 健身动作库浏览与管理
- 肌肉群与器械管理
- 食堂菜单管理
- 训练计划与健康数据页面
- 布局系统与可复用组件
- 基于 Axios 的 HTTP 客户端（含请求/响应拦截器）

### 微信小程序
- 首页及动作详情页
- TDesign 组件库集成
- HTTP 请求工具函数

---

## 已知问题与限制

- 此为初始版本，后端 API 接口和前端页面在后续版本中会有较大调整。
- 微信小程序当前页面较少（仅首页和动作详情页）。
- 数据库迁移脚本为基础框架，后续需进一步完善。

---

## 后续计划（未来版本规划）

- 用户认证与授权（登录 / 注册流程）
- 增强训练追踪功能（进度图表）
- 营养与饮食计划模块
- 社交功能（训练分享、排行榜）
- 管理后台功能增强
- 扩展微信小程序页面与功能
- 单元测试与集成测试
- CI/CD 流水线搭建

---

## 安装与部署

### 环境要求
- Java 21+
- Node.js 18+
- MySQL 8.0+
- Maven 3.9+
- 微信开发者工具（用于小程序）

### 快速启动

```bash
# 后端
cd java-fit-server
mvn spring-boot:run

# Web 前端
cd web-fit-vue
npm install
npm run dev

# 微信小程序
# 使用微信开发者工具打开 mp-fit-ts/ 目录
```

---

## 贡献者

- 开发团队

---

**说明：** 本文档为初始基线版本发布说明。后续所有版本将包含详细的变更日志，记录相较于前一版本的新增、修改与问题修复内容。