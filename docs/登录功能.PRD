# sa-token，web端和小程序端登录



是的，**登录 API 必须是两个完全独立的接口**。

因为这两个前端在登录时，提供给后端的**凭证（数据）完全不同**，后端的校验逻辑也天差地别：

1. **Web端登录接口（如 `/api/auth/web-login`）：** 接收的是 `username` 和 `password`。后端需要去查用户表、比对密码对不对。
2. **小程序端登录接口（如 `/api/auth/wx-login`）：** 接收的是微信的 `code`。后端不需要密码，而是要去请求微信服务器换取 `openid`。

但是在 Sa-Token 眼里，这两个接口虽然进来的路不同，但**终点是一样的**——只要校验通过，它们都调用相同的 `StpUtil.login(userId)` 塞入用户 ID。

既然你决定用 Sa-Token 走极简路线，下面是这两个接口在后端的标准落地写法：

## ## 1. 两个独立的登录接口（Java 后端）

Java

```
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;
    @Autowired
    private WxService wxService; // 负责调用微信接口的组件

    /**
     * 1. Web端登录：账号密码
     */
    @PostMapping("/web-login")
    public SaResult webLogin(@RequestBody WebLoginDTO dto) {
        // 查数据库
        User user = userService.getByUsername(dto.getUsername());
        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            return SaResult.error("账号或密码错误");
        }
        
        // Sa-Token 登录（统一的 StpUtil）
        StpUtil.login(user.getId()); 
        
        // 返回 Token 给 Web 端
        return SaResult.data(StpUtil.getTokenValue());
    }

    /**
     * 2. 小程序端登录：微信自动登录
     */
    @PostMapping("/wx-login")
    public SaResult wxLogin(@RequestBody WxLoginDTO dto) {
        // 拿着小程序传来的 code，去微信换取 openid
        String openid = wxService.getOpenIdByCode(dto.getCode());
        
        // 去用户表查这个 openid 的记录
        User user = userService.getByOpenid(openid);
        boolean isNewUser = false;
        
        if (user == null) {
            // 新用户：静默创建一条记录
            user = new User();
            user.setWxOpenid(openid);
            user.setStatus(0); // 0 代表资料未完善
            userService.save(user);
            isNewUser = true;
        }
        
        // Sa-Token 登录（和 Web 端用同一个 StpUtil）
        StpUtil.login(user.getId()); 
        
        // 返回 Token，并告诉小程序是不是新用户
        return SaResult.ok()
                .set("token", StpUtil.getTokenValue())
                .set("isNewUser", isNewUser || user.getStatus() == 0);
    }
}
```

## ## 2. Sa-Token 的极简全局拦截器配置

登录接口分开后，其余的所有业务接口，不管是 Web 端的还是小程序的，Sa-Token 统一当成“已登录用户”来拦截。

在你的配置类中，只需要**把这两个登录接口放行**即可：

Java

```
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 路由拦截器
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 全局接口都要登录校验
            SaRouter.match("/api/**").check(r -> StpUtil.checkLogin());
        }))
        .addPathPatterns("/api/**")
        // 关键：把 Web端和小程序端的登录 API 统统放行
        .excludePathPatterns("/api/auth/web-login", "/api/auth/wx-login"); 
    }
}
```

## ## 3. 前端拿到了 Token 怎么用？

因为两端共享同一套 Token 逻辑，不管是 Web 还是小程序，接口成功后的处理方式完全一致：

- **Web 端（前端）：**

  拿到 `token` 字符串 ➔ 存入 `localStorage` ➔ 以后写 axios 拦截器，每次请求都在请求头带上 `satoken: xxxx`（Sa-Token默认的 header 名字是 `satoken`）。

- **小程序端（前端）：**

  拿到 `token` 字符串 ➔ 存入 `wx.setStorageSync('satoken', token)` ➔ 以后封装 `wx.request`，每次请求也在 header 里带上 `satoken: xxxx`。

同时，小程序端如果看到 `isNewUser == true`，就直接用 `wx.navigateTo` 引导跳转到录入信息的页面。

## ## 4. 业务层如何区别“这个人是谁”？

登录成功后，所有的业务接口（比如获取商品列表、提交订单、修改资料），在 Controller 里只需要一行代码，Sa-Token 就能自动从请求头里把当前用户的 ID 抠出来：

Java

```
@GetMapping("/api/user/info")
public SaResult getUserInfo() {
    // 无论是 Web 管理员，还是小程序用户，Sa-Token 都能直接拿到对应的用户 ID
    Long currentUserId = StpUtil.getLoginIdAsLong();
    
    User user = userService.getById(currentUserId);
    return SaResult.data(user);
}
```

接口完全独立，鉴权完全统一。这套架构既清晰地分开了两端的登录入口，又实现了后端的极简管理，非常适合你现在的空白项目起步。



# 静默登录

从业务场景和产品体验的视角来看，微信小程序的**静默登录**（`wx.login`）是在后台悄悄发生的。对于初次登录的新用户，他可能只是点开了小程序，甚至还没意识到自己已经“注册”了账号，这时就需要一套自然、合规且不打扰用户的引导流程。

结合微信官方现行的规则，最标准的业务落地视觉与交互流程如下：

## ## 1. 业务交互流程：两阶段“软引导”

不要在用户一进小程序、连首页都没看清时就强制弹窗逼他录入信息，这样流失率极高。推荐采用“静默登录 + 关键动作拦截”的软引导模式。

### ### 阶段一：静默登录，发放临时身份

1. 用户首次打开小程序。
2. 前端静默调用 `/api/auth/wx-login`，后端在数据库创建了一条“无名氏”记录（`status = 0`），发放 Token。
3. 用户此时可以正常浏览小程序的首页、商品、公开文章等（体验极佳，不需要做任何点击）。

### ### 阶段二：触发关键动作，拦截并引导录入

当用户想要点击某个**必须知道他是谁**的按钮时（例如：*“联系管理员”*、*“预约服务”*、*“发布内容”* 或 *“点击个人中心”*）：

1. 小程序判断当前用户的 `status == 0`（或者本地缓存的 `isNewUser == true`）。
2. **不上火的拦截：** 页面不跳转，而是**从底部弹出一个半透明的优雅抽屉（Modal弹窗）**，提示用户完善资料。

## ## 2. 完美的引导界面设计（符合微信合规）

微信目前严禁“一键获取微信昵称头像”，必须由用户自主填写。但微信提供了**快捷填充组件**。你的引导弹窗应该长这样：

### ### 弹窗视觉设计：

> **「 完善您的个人信息 」**
>
> 为了方便在后续服务中称呼您，请填写以下信息：
>
> - **头像：** [ 🔘 点击选择头像按钮 ] *(点击后可选微信头像或相册)*
> - **名字：** [ 填入名字的输入框 ] *(点击后键盘上方会自动弹出他的微信昵称，一键点击即可填入)*
>
> [  确认提交并继续操作  ]

## ## 3. 前端核心组件实现（微信特性）

为了让用户录入名字和头像的操作尽可能简单，前端必须使用微信的 `chooseAvatar` 和 `nickname` 特性：

### ### WXML (界面代码)

HTML

```
<view class="profile-modal" wx:if="{{showGuideModal}}">
  <view class="modal-content">
    <text class="title">完善个人信息</text>
    
    <!-- 1. 头像选择快捷按钮 -->
    <button class="avatar-wrapper" open-type="chooseAvatar" bindchooseavatar="onChooseAvatar">
      <image class="avatar" src="{{avatarUrl || '/images/default-avatar.png'}}"></image>
    </button>
    
    <!-- 2. 名字快捷输入框 -->
    <input type="nickname" class="name-input" placeholder="请输入您的名字或昵称" bindblur="onNameBlur" bindinput="onNameInput"/>
    
    <!-- 3. 提交按钮 -->
    <button class="submit-btn" bindtap="submitProfile">开启体验</button>
  </view>
</view>
```

### ### JS (交互逻辑)

JavaScript

```
Page({
  data: {
    showGuideModal: false,
    avatarUrl: '', // 临时头像路径
    nickname: ''   // 用户输入的或微信提供的值
  },

  // 用户点击了必须登录的按钮时触发
  checkUserStatus() {
    const isNewUser = wx.getStorageSync('isNewUser');
    if (isNewUser) {
      this.setData({ showGuideModal: true });
    }
  },

  // 微信头像选择回调
  onChooseAvatar(e) {
    const { avatarUrl } = e.detail;
    this.setData({ avatarUrl });
    // 实际业务中，拿到临时路径后，需要调用 wx.uploadFile 把图片上传到你的Java后端存起来
  },

  // 名字输入框失去焦点或输入时拿到名字
  onNameBlur(e) {
    this.setData({ nickname: e.detail.value });
  },

  // 提交给后端
  submitProfile() {
    if (!this.data.nickname) {
      wx.showToast({ title: '请输入名字', icon: 'none' });
      return;
    }
    
    // 调用统一的后端 API 更新资料（Header 里带着 Sa-Token 登录时给的 Token）
    wx.request({
      url: 'https://yourdomain.com/api/user/update-profile',
      method: 'POST',
      header: { 'satoken': wx.getStorageSync('satoken') },
      data: {
        nickname: this.data.nickname,
        avatarUrl: this.data.avatarUrl // 上传后的真实URL
      },
      success: (res) => {
        wx.showToast({ title: '资料完善成功' });
        this.setData({ showGuideModal: false });
        wx.setStorageSync('isNewUser', false); // 更新本地状态
        
        // 页面上直接展示刚刚录入的名字
        this.setData({ currentUserName: this.data.nickname });
      }
    });
  }
})
```

## ## 4. 后端配合：用 Token 认人并摘除“新用户”标签

当用户在上述弹窗点击“确认提交”后，请求打到 Java 后端：

Java

```
@PostMapping("/api/user/update-profile")
public SaResult updateProfile(@RequestBody UpdateProfileDTO dto) {
    // 1. 此时用户带着 Sa-Token，直接通过框架获取其对应的数据库自增ID
    Long userId = StpUtil.getLoginIdAsLong();
    
    // 2. 根据 ID 更新名字、头像，并将状态 status 改为 1（已完善）
    User user = userService.getById(userId);
    if (user != null) {
        user.setNickname(dto.getNickname());
        user.setAvatarUrl(dto.getAvatarUrl());
        user.setStatus(1); // 摘除新用户/未完善标签
        userService.updateById(user);
    }
    
    return SaResult.ok("资料完善成功");
}
```

## ## 5. 业务场景总结

通过这套设计，整个流程在业务上闭环得非常自然：

- 用户进小程序 ➔ **静默注册**，分发专属 ID（此时数据库里他叫“用户_10023”，名字为空）。
- 用户要用核心功能了 ➔ **弹出半透明抽屉**。
- 用户利用微信特性，**点一下头像、点一下推荐的名字** ➔ 顺手点击提交。
- 页面上的无名氏变成 **“欢迎您，张三”**，抽屉收回，业务继续。



# web端登录

是的，**Web 端依然保持最纯粹的【用户名+密码】登录就行**。

不需要做任何改变。因为我们采用了“一张用户表 + 同一套 Token + 两个独立登录接口”的设计。

我们可以从业务和代码两个层面来看，为什么 Web 端完全不受影响：

### 💻 Web 端的业务体验（保持原样）

对于你的系统管理员或后台运营人员：

1. 打开 Web 后台管理页面。
2. 输入你分配给他的账号（如 `admin`）和密码。
3. 点击登录，后端验证通过后，塞给他一个 Sa-Token。
4. 他直接进入管理后台，开始上架商品、查看小程序刚注册的用户列表。他**完全不需要**跟微信发生任何交互。

### 🛠️ 后端两边的对比（各走各的路，终点相同）

在 Java 后端，你的两个登录接口分工非常明确：

- **Web 登录接口：** 只认 `username` 和 `password`，查到是管理员，就执行 `StpUtil.login(userId)`。
- **小程序登录接口：** 只认微信 `code`，查到/新建了普通用户，也执行 `StpUtil.login(userId)`。

也就是说，不管是 Web 端的**管理员输入密码**进来的，还是小程序端的**微信用户静默登录**进来的，只要登录成功了，在 Sa-Token 眼里都是一个拿到了有效 Token 的“合法用户”。

### 总结

你现在的架构非常优雅：

- **Web 端：** 简单直接，用最传统的**用户名密码**，开发最快，最适合内部管理。
- **小程序端：** 顺滑无感，用**微信静默登录**，只有新用户首次使用时，才会触发我们前面聊的“弹出抽屉录入名字”的业务流程。

两边互不干扰，各自用最适合自己场景的方式登录，后端却用一套标准轻松管住。这就是前后端分离和统一 API 架构的魅力所在。