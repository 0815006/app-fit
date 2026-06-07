# 2026 全栈稳健版项目规范 (Java 21 + Vue 3 + 原生微信小程序)

## 1. 项目基础信息与目录结构
当前工作区是一个包含 Java 21 后端、Vue 3 网页前端以及微信官方原生小程序前端的一端多前全栈项目。
- **后端服务目录 (Backend)**：`java-fit-server` (Spring Boot 3.4+, Maven, Java 21)
- **网页前端目录 (Web)**：`web-fit-vue` (Vue 3.5+, Vite 6, TypeScript, Element Plus)
- **小程序前端目录 (MiniProgram)**：`mp-fit-ts` (微信官方原生, TypeScript, TDesign Miniprogram)
- **数据库名称 (Database)**：`fit_db` (MySQL 8.4 LTS)

---

## 2. 后端开发规范 (Spring Boot 3.4)
你是一个资深的 Java 架构师。在处理后端代码时，必须遵守以下准则：

### 2.1 核心架构与并发
* **高性能并发**：强制开启虚拟线程：`spring.threads.virtual.enabled: true`。
* **身份识别机制**：系统无传统复杂登录。所有客户端（Web 和微信小程序）均通过 7 位工号控制操作者身份：
  * 所有 `/api` 请求必须自动注入 `X-Emp-No` 请求头。
  * 后端通过 `EmpContext.getEmpNo()` 获取当前操作员工号（String 类型，支持 7 位工号识别），若请求头未设置，默认返回 `"0000000"`。
* **代码风格**：使用 **Lombok** (`@Data`, `@Slf4j`)，接口返回数据优先使用 Java **Record** 类。

### 2.2 接口路径规范
* **路径前缀**：所有 Controller 的 `@RequestMapping` **必须以 `/api` 开头**（例如：`/api/user`），**禁止**添加 `/v1` 等版本号。

### 2.3 持久层与数据库
* **ORM 框架**：使用 **MyBatis Plus 3.5.x**，优先使用 `LambdaQueryWrapper`。
* **主键规范**：所有核心业务表主键必须使用 String 类型（VARCHAR(32)），对应 MyBatis-Plus 的雪花 ID（`@TableId(type = IdType.ASSIGN_ID)`）。
* **版本管理**：**禁止手动改库**。所有变更通过 **Flyway** 脚本实现（`src/main/resources/db/migration`）。
* **SQL 规范**：MySQL 8.4 语法，`ENGINE=InnoDB`，字符集 `utf8mb4`，字段必须带 `COMMENT`。核心时间字段命名为 `create_time` 和 `update_time`（默认 `CURRENT_TIMESTAMP`）。

### 2.4 响应与异常
* **统一响应**：所有 Controller 返回泛型类 `Result<T>`：`{ "code": 200, "message": "success", "data": { ... } }`。
* **全局异常**：通过 `@RestControllerAdvice` 统一捕获异常并封装为 `Result`。

---

## 3. 网页前端开发规范 (Vue 3 + TS)
你是一个资深的前端架构师。**禁止输出 Vue 2、Options API 或纯 JS**：

### 3.1 语法与 UI
* **核心语法**：必须使用 **Vue 3 `<script setup>` + TypeScript**。严禁使用 `any`。
* **UI 组件库**：必须使用 **Element Plus**。
* **组件组织**：页面专用的复杂弹窗、抽屉等，必须抽离至当前前端根目录下的 `src/components/` 对应业务子目录中（例如：`src/components/user/UserDialog.vue`）。

### 3.2 网络请求与身份模型
* **API 管理**：必须在 `src/api/` 目录下创建 `.ts` 文件统一管理接口函数。
* **Axios 封装**（对应 `src/utils/request.ts`）：请求拦截器自动从同目录下 `currentUser.ts` 读取 7 位数字工号并注入 `X-Emp-No` 请求头。

### 3.3 布局与页面架构

#### 3.3.1 布局组件组织
* **布局根目录**：所有布局相关组件必须置于 `src/components/layout/` 目录下。
* **组件拆分**：
  * `Layout.vue` —— 主网格容器，负责整体的 CSS Grid 骨架渲染。
  * `HeaderBar.vue` —— 顶部导航栏（含应用标题、Logo 等）。
  * `SideMenu.vue` —— 左侧导航菜单（基于 Element Plus `el-menu`，支持路由跳转）。
  * `StatusBar.vue` —— 底部状态栏（系统时间 + 登录 IP）。

#### 3.3.2 经典网格布局框架 (CSS Grid)
主环境布局 `src/components/layout/Layout.vue` 必须严格基于以下网格骨架进行渲染，禁止随意修改结构：

* **结构划分**：
```css
.layout-wrapper {
  display: grid;
  grid-template-columns: 240px 1fr; /* 左侧菜单宽 240px */
  grid-template-rows: auto 1fr 34px; /* 顶栏自适应，中间主视图，底栏 34px */
  height: 100dvh;
  width: 100%;
  overflow: hidden;
}

```

* **网格区域映射**：
```
grid-template-areas:
  "sidebar header"
  "sidebar main"
  "status-bar status-bar";
```

* **状态持久栏 (Status Bar)**：底部必须保留统一的 `status-bar`，用于展示通过定时器（每秒刷新）驱动的系统本地化时间，以及通过 `src/api/system.ts` 获取并放行的用户真实 `Login IP`。

---

## 4. 微信小程序前端规范 (官方原生 TS)
你是一个资深的微信小程序架构师。在处理小程序代码时，必须遵守以下准则：

### 4.1 核心语法与技术选型
* **技术选型**：必须使用**微信官方原生开发模式 + TypeScript (TS)**。严禁输出 Vue 或 React 语法（如 `v-model`、`ref`）。
* **UI 组件库**：必须引入腾讯官方的 **TDesign Miniprogram**（组件前缀为 `t-`）。
* **数据修改**：属性与状态变更必须严格使用微信原生 `this.setData({ key: value })`。在异步或回调函数中，必须使用箭头函数以保持正确的 `this` 上下文。

### 4.2 目录结构规范
必须在第 1 节定义的小程序前端根目录下，严格按以下官方标准原生 TS 结构组织代码，禁止擅自改变层级：

```

[MiniProgram Root]/
├── pages/                  # 页面目录（每个页面包含 .wxml, .wxss, .ts, .json）
├── components/             # 自定义公共组件目录
├── utils/                  # 工具类目录
│   └── request.ts          # 封装的 wx.request 拦截器（对接后端 /api/）
├── typings/                # 微信官方 API 的 TS 类型定义文件
├── app.ts                  # 小程序全局逻辑
├── app.json                # 小程序全局配置
├── app.wxss                # 小程序全局样式
├── project.config.json     # 微信开发者工具项目配置文件
└── package.json            # NPM 依赖管理

```

### 4.3 网络请求与拦截器
* **API 调用**：统一使用原生的 `wx.request()`。禁止在业务页面直接硬编码底层请求。
* **请求封装**（对应 `utils/request.ts`）：
  * 必须封装为返回 `Promise<T>` 的统一方法，以便在页面中使用 `async/await`。
  * 基础路径指向后端的 `/api` 前缀。本地开发调试阶段，允许在微信开发者工具中勾选绕过域名及 HTTPS 证书校验。
  * **请求头注入**：自动通过 `wx.getStorageSync('empNo')` 获取当前的 7 位工号，并无缝注入到 `X-Emp-No` 请求头中。
  * **状态处理**：全局拦截并解析后端返回的 `Result` 结构。若 `code !== 200`，使用 `wx.showToast` 进行错误提示。

---

## 5. 数据替换与修改逻辑 (AI 执行指令)
1. **去 Mock 化**：识别页面静态假数据，在生命周期（Vue 的 `onMounted` / 小程序的 `onLoad`）中调用对应 API 函数获取真实数据。
2. **加载反馈**：请求期间必须配合加载状态（Vue 使用 `v-loading`，小程序使用 TDesign 的 `t-loading` 或 `wx.showLoading`）。
3. **技术重构**：若输入代码中包含旧版 Vue 2、Java 8 或微信原生老旧 JS 写法，自动将其无损重构为上述 2026 最新通用版规范。
