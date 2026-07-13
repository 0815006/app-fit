# 菜品收藏与消息提醒 PRD

> 版本：v1.0  
> 创建时间：2026-07-13  
> 状态：待实施

---

## 目录

1. [功能概述](#1-功能概述)
2. [微信订阅消息模板](#2-微信订阅消息模板)
3. [数据库设计（Flyway 迁移）](#3-数据库设计flyway-迁移)
4. [后端 API 设计](#4-后端-api-设计)
5. [定时任务设计](#5-定时任务设计)
6. [小程序前端设计](#6-小程序前端设计)
7. [实施清单](#7-实施清单)

---

## 1. 功能概述

### 1.1 核心用户故事

1. **菜品收藏**：用户在今日菜单中点击某菜品旁的星星，即可收藏/取消收藏该菜品。已收藏显示实心黄星⭐，未收藏显示空心黄星☆。
2. **收藏管理**：在"个人信息"Tab → "关于"上方新增"收藏菜品"卡片入口，进入独立页面查看、管理已收藏菜品，支持取消收藏。
3. **订阅消息收集**：用户点击星星时，前端调用 [`wx.requestSubscribeMessage`](mp-fit-ts/utils/request.js) 收集订阅授权，后端记录用户对该模板的可用推送次数。
4. **定时推送**：每天 8:00 定时任务扫描：今日菜单中出现了哪些被收藏的菜品 → 匹配有剩余推送次数的用户 → 调用微信订阅消息接口发送提醒 → 扣减次数 → 记录推送历史。
5. **次数激励**：收藏菜品页面展示剩余推送次数，建议用户"多浏览菜品攒推送次数"（每次浏览/收藏可增加次数）。

### 1.2 关键业务规则

| 规则 | 说明 |
|------|------|
| 收藏粒度 | **只记菜名**（`dish_name`），不关联日期、餐次、食堂 |
| 收藏唯一键 | `emp_no` + `dish_name` |
| 订阅次数 | 按 `emp_no` + `template_id` 唯一记录，通过收藏菜品获取（每次收藏 +1，同一菜品不重复）。每日浏览攒次数上限 +5，累计总上限 30 |
| 推送开关 | 默认开启（`push_enabled = 1`），用户可在收藏菜品页面关闭。关闭后定时任务跳过该用户 |
| 推送条件 | `remaining_count > 0` 且 `push_enabled = 1` 才推送 |
| 推送扣减 | 每发一条模板消息，次数 -1 |
| 推送历史 | 无论成功/失败，均记录到 `push_message_history` |

---

## 2. 微信订阅消息模板

### 2.1 模板信息

已在微信小程序管理后台 → 功能 → 订阅消息中选用：

| 属性 | 值 |
|------|-----|
| **模板 ID (Template ID)** | `KABRC3CxbGsD2TZQNjPWcWEl17kU1q0rNipugHkMUmA` |
| **模板编号** | 25808 |
| **标题** | 饮食计划提醒 |
| **类目** | 在线健身 |
| **添加时间** | 2026-07-13 |

### 2.2 模板字段（填空题）

```
饮食计划提醒
─────────────────────
饮食提醒    {{thing2.DATA}}     ← 限制 20 个字符
饮食推荐    {{thing3.DATA}}     ← 限制 20 个字符
时间        {{time1.DATA}}      ← 时间格式
─────────────────────
场景说明：推荐用户饮食
```

### 2.3 填值映射（定时任务发送时）

| 模板字段 | 填充内容示例 | 说明 |
|----------|-------------|------|
| `thing2` | `您收藏的菜品今日供应` | 温馨提示/饮食提醒 |
| `thing3` | `红烧肉、酸辣土豆丝` | 匹配到的收藏菜品名称，多个用顿号分隔（注意 20 字限制） |
| `time1` | `2026-07-14 11:30` | 该菜品的就餐时间（取餐次对应的典型时间） |

### 2.4 餐次时间映射

| 餐次 | `time1` 填充值 |
|------|---------------|
| 早餐 | `08:00` |
| 午餐 | `11:30` |
| 晚餐 | `17:30` |
| 夜宵 | `23:00` |

---

## 3. 数据库设计（Flyway 迁移）

### 3.1 菜品收藏表 `user_favorite_dish`

**Flyway 脚本**：`V31__create_user_favorite_dish.sql`

```sql
CREATE TABLE user_favorite_dish (
    id              VARCHAR(32)  NOT NULL COMMENT '主键雪花ID',
    emp_no          VARCHAR(7)   NOT NULL COMMENT '用户工号',
    dish_name       VARCHAR(100) NOT NULL COMMENT '收藏的菜品名称',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_emp_dish (emp_no, dish_name),
    INDEX idx_emp_no (emp_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户菜品收藏表';
```

### 3.2 用户订阅次数表 `user_subscribe_quota`

**Flyway 脚本**：`V32__create_user_subscribe_quota.sql`

```sql
CREATE TABLE user_subscribe_quota (
    id              VARCHAR(32)  NOT NULL COMMENT '主键雪花ID',
    emp_no          VARCHAR(7)   NOT NULL COMMENT '用户工号',
    template_id     VARCHAR(64)  NOT NULL COMMENT '微信订阅消息模板ID',
    remaining_count INT          NOT NULL DEFAULT 0 COMMENT '剩余可用推送次数',
    push_enabled    TINYINT      NOT NULL DEFAULT 1 COMMENT '推送开关：1-开启，0-关闭',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_emp_template (emp_no, template_id),
    INDEX idx_emp_no (emp_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户订阅消息次数表';
```

### 3.3 推送消息历史表 `push_message_history`

**Flyway 脚本**：`V33__create_push_message_history.sql`

```sql
CREATE TABLE push_message_history (
    id              VARCHAR(32)  NOT NULL COMMENT '主键雪花ID',
    emp_no          VARCHAR(7)   NOT NULL COMMENT '用户工号',
    template_id     VARCHAR(64)  NOT NULL COMMENT '微信订阅消息模板ID',
    dish_names      VARCHAR(500) NOT NULL COMMENT '推送的菜品名称（多个用逗号分隔）',
    canteen_zone    VARCHAR(20)  NOT NULL COMMENT '食堂区域：一期、二期',
    meal_type       VARCHAR(20)  NOT NULL COMMENT '餐次类型：早餐、午餐、晚餐、夜宵',
    menu_date       DATE         NOT NULL COMMENT '菜单日期',
    push_status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '推送状态：SUCCESS、FAILED',
    error_message   VARCHAR(500) DEFAULT NULL COMMENT '失败时的错误信息',
    send_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '推送时间',
    PRIMARY KEY (id),
    INDEX idx_emp_no (emp_no),
    INDEX idx_push_status (push_status),
    INDEX idx_send_time (send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推送消息历史表';
```

### 3.4 配置常量（`application.yml` 或配置表）

```yaml
wx:
  subscribe:
    template-id: KABRC3CxbGsD2TZQNjPWcWEl17kU1q0rNipugHkMUmA
```

---

## 4. 后端 API 设计

所有 API 路径前缀均为 `/api`，通过 `X-Emp-No` 请求头识别当前用户（由 `EmpContext.getEmpNo()` 获取）。

### 4.1 菜品收藏

#### 4.1.1 收藏/取消收藏（Toggle）

```
POST /api/favorite-dish/toggle
```

**Request Body:**
```json
{
  "dishName": "红烧肉"
}
```

**Response (收藏成功):**
```json
{
  "code": 200,
  "message": "success",
  "data": { "favorited": true, "dishName": "红烧肉" }
}
```

**Response (取消收藏成功):**
```json
{
  "code": 200,
  "message": "success",
  "data": { "favorited": false, "dishName": "红烧肉" }
}
```

**业务逻辑**：如果该 `emp_no` + `dish_name` 记录已存在 → 删除（取消收藏，返回 `favorited: false`）。不存在 → 插入（收藏，返回 `favorited: true`），同时调用 4.2.1 的方法增加 1 次订阅次数。

---

#### 4.1.2 批量查询收藏状态

```
GET /api/favorite-dish/check?dishNames=红烧肉,酸辣土豆丝,白切鸡
```

**Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": ["红烧肉", "白切鸡"]
}
```

> 返回 `data` 中仅包含当前用户已收藏的菜品名称列表。前端据此判断每个菜品的星星样式。

---

#### 4.1.3 获取收藏列表

```
GET /api/favorite-dish/list
```

**Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    { "id": "xxx", "dishName": "红烧肉", "createTime": "2026-07-13T12:00:00" },
    { "id": "xxx", "dishName": "酸辣土豆丝", "createTime": "2026-07-12T18:30:00" }
  ]
}
```

---

#### 4.1.4 删除单个收藏

```
DELETE /api/favorite-dish/{dishName}
```

> `dishName` 需 URL Encode。例如：`/api/favorite-dish/%E7%BA%A2%E7%83%A7%E8%82%89`

**Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

### 4.2 订阅次数

#### 4.2.1 增加订阅次数（内部调用 & 小程序显式调用）

```
POST /api/subscribe-quota/increment
```

**Request Body:**
```json
{
  "templateId": "KABRC3CxbGsD2TZQNjPWcWEl17kU1q0rNipugHkMUmA",
  "count": 1
}
```

**Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": { "remainingCount": 5 }
}
```

**业务逻辑**：
- 如果 `emp_no` + `template_id` 记录不存在 → 创建，`remaining_count = count`，`push_enabled = 1`。
- 如果已存在 → `remaining_count += count`。
- `count` 默认为 1，由前端传入。
- **每日上限**：同一用户每天通过"浏览菜品"累计最多 **+5**（由后端判断当天增量）。
- **总上限**：`remaining_count` 累计不超过 **30**。

**调用场景**：
1. 用户成功收藏一个菜品时（`favorite-dish/toggle` 内部调用）。
2. 用户在收藏菜品页面点击"浏览菜品攒次数"按钮时。

---

#### 4.2.2 查询当前用户订阅次数

```
GET /api/subscribe-quota?templateId=KABRC3CxbGsD2TZQNjPWcWEl17kU1q0rNipugHkMUmA
```

**Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "templateId": "KABRC3CxbGsD2TZQNjPWcWEl17kU1q0rNipugHkMUmA",
    "remainingCount": 3,
    "pushEnabled": true
  }
}
```

> 如果该用户尚未有任何记录，返回 `remainingCount: 0, pushEnabled: true`。

---

#### 4.2.3 切换推送开关

```
POST /api/subscribe-quota/toggle-push
```

**Request Body:**
```json
{
  "templateId": "KABRC3CxbGsD2TZQNjPWcWEl17kU1q0rNipugHkMUmA",
  "pushEnabled": false
}
```

**Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": { "pushEnabled": false }
}
```

> 切换 `user_subscribe_quota.push_enabled` 的值。记录不存在时自动创建（`remaining_count = 0`，`push_enabled` 按请求值）。

---

### 4.3 推送历史（可选，供管理端查询）

```
GET /api/push-history?page=1&size=20
```

---

## 5. 定时任务设计

### 5.1 执行流程

使用 Spring `@Scheduled` 注解，每天 **8:00** 执行。

```
Cron: 0 0 8 * * ?
```

**伪代码流程：**

```
1. [查今日菜单]
   从 canteen_menu_record 查询 menu_date = 今天 的所有菜品，
   去重得到今日所有 dish_name 集合。

2. [匹配收藏用户]
   在 user_favorite_dish 中查询 dish_name IN (今日菜品集合)，
   得到 Map<emp_no, List<dish_name>>（按用户分组）。

3. [过滤推送条件]
   对每个 emp_no，查询 user_subscribe_quota 表中
   emp_no + 固定 template_id 的记录，
   同时满足以下条件才推送：
     a. remaining_count > 0（有剩余次数）
     b. push_enabled = 1（推送开关已开启）

4. [获取 Access Token]
   从 Redis 缓存读取微信 access_token（由另一个独立定时器维护）。
   如果缓存中无有效 token，实时调用微信接口获取。

5. [组装并发送消息]
   对每个符合条件的用户：
     a. 将该用户的收藏菜品与今日菜单取交集。
     b. 按 canteen_zone + meal_type 分组。
     c. 每个分组发送一条模板消息（防止消息内容过多）：
        - thing2: "您收藏的菜品今日供应"
        - thing3: 菜品名列表（顿号分隔，20字以内，超出截断加"等"）
        - time1: 该餐次对应时间
     d. 调用微信 subscribeMessage.send 接口。

6. [后处理]
   发送成功 → user_subscribe_quota.remaining_count -= 1
             → push_message_history 记录 SUCCESS
   发送失败 → push_message_history 记录 FAILED + error_message
```

### 5.2 微信 API 调用详情

**接口**：`POST https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=ACCESS_TOKEN`

**请求体示例**：
```json
{
  "touser": "用户openid",
  "template_id": "KABRC3CxbGsD2TZQNjPWcWEl17kU1q0rNipugHkMUmA",
  "page": "pages/index/index",
  "data": {
    "thing2": { "value": "您收藏的菜品今日供应" },
    "thing3": { "value": "红烧肉、酸辣土豆丝" },
    "time1": { "value": "2026-07-14 11:30" }
  }
}
```

> **注意**：发送时需要用户的微信 `openid`，因此需要在 `User` 表或关联表中存储用户的 `openid`。如果当前 `User` 表中无此字段，需额外增加 Flyway 迁移。

### 5.3 增量次数策略

用户可通过以下行为增加推送次数：

| 行为 | 次数增量 | 触发时机 |
|------|---------|---------|
| 收藏一个新菜品（从未收藏过的） | +1 | `favorite-dish/toggle` 返回 `favorited: true` 时 |
| 浏览菜品页面 | +1 | 每次进入 `today-menu` 或 `menu` 页面（可选，前端调用 `/api/subscribe-quota/increment`） |

> - **每日上限**：浏览菜品每天最多 +5 次数（由后端 `increment` 接口判断当天累计增量）。
> - **总上限**：`remaining_count` 累计不超过 **30**。

---

## 6. 小程序前端设计

### 6.1 today-menu 组件改造：菜品收藏星标

#### 6.1.1 [`today-menu.wxml`](mp-fit-ts/components/today-menu/today-menu.wxml) 修改

在每条菜品 `.dish-top` 区域内，菜品名右侧增加星标按钮：

```xml
<!-- 菜品名 + 星标 + 辣标签 -->
<view class="dish-top">
  <!-- 星标收藏按钮 -->
  <view class="fav-star" data-dish="{{ it.dishName }}" catchtap="handleToggleFavorite">
    <text class="star-icon">{{ favSet[it.dishName] ? '⭐' : '☆' }}</text>
  </view>
  <text class="dish-name">{{ it.dishName }}</text>
  <view class="hot-tag" wx:if="{{ it.isSpicy === 1 }}">
    <text>🌶️ 辣</text>
  </view>
</view>
```

#### 6.1.2 [`today-menu.js`](mp-fit-ts/components/today-menu/today-menu.js) 修改

增加：
- `data.favSet`：`{}`，记录当前页面菜品的收藏状态
- `data.favLoading`：`false`
- `loadFavorites()` 方法：加载当前页面所有菜品的收藏状态（调用 `/api/favorite-dish/check`）
- `handleToggleFavorite(e)` 方法：点击星标 → 调用 `/api/favorite-dish/toggle` → 更新 `favSet` → 若收藏成功且首次收藏该菜品，调用 `/api/subscribe-quota/increment` → 调用 [`wx.requestSubscribeMessage`](mp-fit-ts/utils/request.js) 收集订阅授权

#### 6.1.3 星标样式（[`today-menu.wxss`](mp-fit-ts/components/today-menu/today-menu.wxss)）

```css
.fav-star {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 8rpx;
  flex-shrink: 0;
}

.star-icon {
  font-size: 36rpx;
  line-height: 1;
}
```

---

### 6.2 个人信息 Tab 新增"收藏菜品"入口

#### 6.2.1 [`index.wxml`](mp-fit-ts/pages/index/index.wxml) 修改

在"关于"卡片上方，插入收藏菜品卡片：

```xml
<!-- 收藏菜品入口卡片 -->
<view class="fav-dish-entry" bindtap="goToFavoriteDishes">
  <view class="fav-entry-left">
    <text class="fav-entry-icon">⭐</text>
    <view class="fav-entry-body">
      <text class="fav-entry-title">收藏菜品</text>
      <text class="fav-entry-desc">管理我的收藏 · 查看推送次数</text>
    </view>
  </view>
  <view class="fav-entry-arrow">›</view>
</view>
```

#### 6.2.2 [`index.js`](mp-fit-ts/pages/index/index.js) 修改

增加方法：

```js
goToFavoriteDishes: function () {
  wx.navigateTo({ url: '/pages/favorite-dishes/favorite-dishes' })
},
```

---

### 6.3 收藏菜品页面（新页面）

#### 6.3.1 路由注册

在 [`app.json`](mp-fit-ts/app.json) `pages` 数组最前面增加：

```json
"pages/favorite-dishes/favorite-dishes"
```

#### 6.3.2 目录结构

```
mp-fit-ts/pages/favorite-dishes/
├── favorite-dishes.js      # 页面逻辑
├── favorite-dishes.json    # 页面配置
├── favorite-dishes.wxml    # 页面结构
└── favorite-dishes.wxss    # 页面样式
```

#### 6.3.3 页面功能

| 区域 | 功能 |
|------|------|
| **顶部：推送次数卡片** | 展示剩余推送次数（调用 `GET /api/subscribe-quota`）和推送开关 toggle。提示文案："多浏览菜品可攒推送次数，收藏的菜品上架时会推送提醒你哦～（每日最多 +5，累计不超过 30）"。提供"去浏览菜品"按钮，跳转至今日菜单页。 |
| **推送开关** | 在推送次数卡片内，提供 switch 开关控件，绑定 `pushEnabled` 字段。切换时调用 `POST /api/subscribe-quota/toggle-push`。默认开启，关闭后定时任务跳过该用户。 |
| **中部：收藏列表** | 展示用户已收藏的菜品列表（调用 `GET /api/favorite-dish/list`）。每个菜品右侧有红色"取消收藏"按钮。 |
| **空状态** | 无收藏时显示："还没有收藏菜品，去今日菜单看看吧～" + 跳转按钮。 |

#### 6.3.4 关键交互

- **取消收藏**：点击菜品右侧"取消"按钮 → `wx.showModal` 确认 → `DELETE /api/favorite-dish/{dishName}` → 刷新列表。
- **下拉刷新**：支持 `onPullDownRefresh` 刷新收藏列表和次数。
- **加载状态**：使用 [`wx.showLoading`](mp-fit-ts/utils/request.js) / [`wx.hideLoading`](mp-fit-ts/utils/request.js)。

---

## 7. 实施清单

### 7.1 后端（java-fit-server）

| # | 任务 | 产出 |
|---|------|------|
| 1 | Flyway V31: `user_favorite_dish` 表 | SQL 迁移脚本 |
| 2 | Flyway V32: `user_subscribe_quota` 表 | SQL 迁移脚本 |
| 3 | Flyway V33: `push_message_history` 表 | SQL 迁移脚本 |
| 4 | Entity: `UserFavoriteDish` | 实体类 |
| 5 | Entity: `UserSubscribeQuota` | 实体类 |
| 6 | Entity: `PushMessageHistory` | 实体类 |
| 7 | Mapper × 3 | MyBatis Plus Mapper |
| 8 | Service: `FavoriteDishService` | 收藏 toggle / check / list / delete |
| 9 | Service: `SubscribeQuotaService` | 次数 increment / query |
| 10 | Controller: `FavoriteDishController` | `/api/favorite-dish/*` 端点 |
| 11 | Controller: `SubscribeQuotaController` | `/api/subscribe-quota/*` 端点 |
| 12 | Service: `WxSubscribeMessageService` | 封装微信 `subscribeMessage.send` 调用 |
| 13 | 定时任务: `DishReminderJob` | `@Scheduled(cron="0 0 8 * * ?")`，核心推送逻辑 |
| 14 | 定时任务: `WxAccessTokenRefreshJob` | 每小时刷新 access_token 到 Redis/内存缓存 |
| 15 | 微信 SDK 依赖配置 | `pom.xml` 增加微信 SDK 或 HTTP 客户端配置 |

### 7.2 小程序前端（mp-fit-ts）

| # | 任务 | 产出 |
|---|------|------|
| 1 | today-menu 组件增加星标 + 收藏切换逻辑 | 修改 `.js` `.wxml` `.wxss` |
| 2 | 个人信息 Tab 增加"收藏菜品"入口卡片 | 修改 `index.wxml` `index.js` `index.wxss` |
| 3 | 新建 `pages/favorite-dishes/` 页面（四个文件） | 收藏菜品管理页面 |
| 4 | `app.json` 注册新页面路由 | 路由配置 |
| 5 | `utils/request.js` 无需改动 | — |

### 7.3 依赖与前置条件

- 微信公众平台已选用模板 `KABRC3CxbGsD2TZQNjPWcWEl17kU1q0rNipugHkMUmA` ✅（已完成）
- `User` 表中需有 `openid` 字段供发送订阅消息使用。若不存在，需增加 Flyway 迁移。
- 后端需引入微信 SDK 依赖（如 `weixin-java-miniapp`）或直接使用 RestTemplate/HttpClient 调用微信 API。
