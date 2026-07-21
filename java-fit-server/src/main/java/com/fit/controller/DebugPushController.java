package com.fit.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fit.common.Result;
import com.fit.entity.*;
import com.fit.mapper.*;
import com.fit.service.PushMessageHistoryService;
import com.fit.service.WxSubscribeMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 调试用 Controller —— 手动触发菜品收藏推送，仅本地开发使用。
 *
 * <p>通过 userId（user 表主键 id）直接查询收藏、配额和 openid，
 * 复刻 DishReminderJob 的完整推送流程。
 *
 * <p>Postman 调用示例：
 * <pre>
 * POST http://localhost:8091/api/debug/push-dish-reminder
 * Content-Type: application/json
 *
 * { "userId": "2076474727246811138" }
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugPushController {

    private static final String TEMPLATE_ID = "KABRC3CxbGsD2TZQNjPWcWEl17kU1q0rNipugHkMUmA";

    private static final Map<String, String> MEAL_TIME_MAP = Map.of(
            "早餐", "08:00",
            "午餐", "11:30",
            "晚餐", "17:30",
            "夜宵", "23:00"
    );

    private final CanteenMenuRecordMapper canteenMenuRecordMapper;
    private final UserFavoriteDishMapper userFavoriteDishMapper;
    private final UserSubscribeQuotaMapper userSubscribeQuotaMapper;
    private final UserMapper userMapper;
    private final PushMessageHistoryService pushMessageHistoryService;
    private final WxSubscribeMessageService wxSubscribeMessageService;

    @PostMapping("/push-dish-reminder")
    public Result<Map<String, Object>> pushDishReminder(@RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        if (userId == null || userId.isBlank()) {
            return Result.error("userId 不能为空（传 user 表主键 id）");
        }
        userId = userId.trim();

        log.info("========== [DEBUG] 手动触发推送: userId={} ==========", userId);
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("userId", userId);

        try {
            // 0. 通过 userId 获取用户信息（含 openid）
            User user = userMapper.selectById(userId);
            if (user == null) {
                s.put("result", "SKIPPED");
                s.put("reason", "user 表未找到 userId=" + userId);
                return Result.success(s);
            }
            s.put("nickname", user.getNickname());

            if (user.getWxOpenid() == null || user.getWxOpenid().isBlank()) {
                s.put("result", "SKIPPED");
                s.put("reason", "用户无 wx_openid，请先用小程序登录一次");
                return Result.success(s);
            }
            s.put("openidPrefix", user.getWxOpenid().substring(0, Math.min(8, user.getWxOpenid().length())) + "...");

            LocalDate today = LocalDate.now();
            s.put("menuDate", today.toString());

            // 1. 今日菜单
            LambdaQueryWrapper<CanteenMenuRecord> menuQw = new LambdaQueryWrapper<>();
            menuQw.eq(CanteenMenuRecord::getMenuDate, today);
            List<CanteenMenuRecord> todayMenus = canteenMenuRecordMapper.selectList(menuQw);
            if (todayMenus.isEmpty()) {
                s.put("result", "SKIPPED");
                s.put("reason", "今日无菜单数据");
                return Result.success(s);
            }
            Map<String, List<CanteenMenuRecord>> dishMenuMap = todayMenus.stream()
                    .collect(Collectors.groupingBy(CanteenMenuRecord::getDishName));
            Set<String> todayDishNames = dishMenuMap.keySet();
            s.put("todayDishCount", todayDishNames.size());

            // 2. 收藏 —— 按 userId 查询
            List<UserFavoriteDish> allFav = userFavoriteDishMapper.selectList(
                    new LambdaQueryWrapper<UserFavoriteDish>()
                            .eq(UserFavoriteDish::getUserId, userId));
            List<String> allFavNames = allFav.stream().map(UserFavoriteDish::getDishName).distinct().toList();
            s.put("allFavoriteDishes", allFavNames);

            List<UserFavoriteDish> matched = userFavoriteDishMapper.selectList(
                    new LambdaQueryWrapper<UserFavoriteDish>()
                            .eq(UserFavoriteDish::getUserId, userId)
                            .in(UserFavoriteDish::getDishName, todayDishNames));
            if (matched.isEmpty()) {
                s.put("result", "SKIPPED");
                s.put("reason", "userId=" + userId + " 共收藏了 " + allFavNames.size()
                        + " 个菜品，今日共 " + todayDishNames.size() + " 个，交集为 0");
                if (!todayDishNames.isEmpty()) {
                    s.put("sampleTodayDishes", todayDishNames.stream().limit(10).collect(Collectors.toList()));
                }
                return Result.success(s);
            }

            List<String> matchedDishNames = matched.stream()
                    .map(UserFavoriteDish::getDishName).distinct().collect(Collectors.toList());
            s.put("matchedDishes", matchedDishNames);

            // 3. quota —— 按 userId 查询
            UserSubscribeQuota quota = userSubscribeQuotaMapper.selectOne(
                    new LambdaQueryWrapper<UserSubscribeQuota>()
                            .eq(UserSubscribeQuota::getUserId, userId)
                            .eq(UserSubscribeQuota::getTemplateId, TEMPLATE_ID));
            if (quota == null) {
                s.put("result", "SKIPPED");
                s.put("reason", "user_subscribe_quota 无记录（userId=" + userId + "），没收藏过菜品或 quota 未初始化");
                return Result.success(s);
            }
            s.put("remainingCount", quota.getRemainingCount());
            s.put("pushEnabled", quota.getPushEnabled());
            if (quota.getRemainingCount() <= 0) {
                s.put("result", "SKIPPED");
                s.put("reason", "剩余推送次数不足: " + quota.getRemainingCount());
                return Result.success(s);
            }
            if (quota.getPushEnabled() != 1) {
                s.put("result", "SKIPPED");
                s.put("reason", "推送开关已关闭");
                return Result.success(s);
            }

            // 4. 按 meal_type 分组（跨食堂区域合并去重）
            Map<String, Set<String>> dishGroupMap = new LinkedHashMap<>();
            Map<String, Set<String>> zoneGroupMap = new LinkedHashMap<>();
            for (String dishName : matchedDishNames) {
                List<CanteenMenuRecord> records = dishMenuMap.get(dishName);
                if (records == null) continue;
                for (CanteenMenuRecord r : records) {
                    String mealType = r.getMealType();
                    dishGroupMap.computeIfAbsent(mealType, k -> new LinkedHashSet<>()).add(dishName);
                    zoneGroupMap.computeIfAbsent(mealType, k -> new LinkedHashSet<>())
                            .add(r.getCanteenZone());
                }
            }

            // 5. 每个餐次发送一条模板消息
            List<Map<String, Object>> details = new ArrayList<>();
            int ok = 0, fail = 0;
            for (Map.Entry<String, Set<String>> g : dishGroupMap.entrySet()) {
                String meal = g.getKey();
                Set<String> canteenZones = zoneGroupMap.get(meal);
                String zone = canteenZones != null && !canteenZones.isEmpty()
                        ? String.join("、", canteenZones)
                        : "";
                Set<String> dishes = g.getValue();
                String thing3 = String.join("、", dishes);
                if (thing3.length() > 20) {
                    int cut = 18;
                    int sep = thing3.lastIndexOf('、', cut);
                    if (sep > 0) cut = sep;
                    thing3 = thing3.substring(0, cut) + "等";
                }

                log.info("[DEBUG] 发送: zone={}, meal={}, dishes={}", zone, meal, dishes);
                Map<String, Object> resp = wxSubscribeMessageService.sendSubscribeMessage(
                        user.getWxOpenid(), TEMPLATE_ID,
                        "您收藏的菜品今日供应", thing3,
                        MEAL_TIME_MAP.getOrDefault(meal, "11:30"),
                        "pages/index/index");

                int code = (int) resp.getOrDefault("errcode", -1);
                String msg = (String) resp.getOrDefault("errmsg", "未知");

                PushMessageHistory h = new PushMessageHistory();
                h.setUserId(userId);
                h.setTemplateId(TEMPLATE_ID);
                h.setDishNames(String.join(",", dishes));
                h.setCanteenZone(zone);
                h.setMealType(meal);
                h.setMenuDate(today);
                h.setSendTime(LocalDateTime.now());

                Map<String, Object> d = new LinkedHashMap<>();
                d.put("canteenZone", zone);
                d.put("mealType", meal);
                d.put("dishNames", String.join(",", dishes));
                d.put("errcode", code);
                d.put("errmsg", msg);

                if (code == 0) {
                    h.setPushStatus("SUCCESS");
                    ok++;
                    quota.setRemainingCount(quota.getRemainingCount() - 1);
                    userSubscribeQuotaMapper.updateById(quota);
                } else {
                    h.setPushStatus("FAILED");
                    h.setErrorMessage(msg);
                    fail++;
                }
                pushMessageHistoryService.save(h);
                d.put("pushStatus", h.getPushStatus());
                details.add(d);
            }

            s.put("result", "DONE");
            s.put("successCount", ok);
            s.put("failCount", fail);
            s.put("remainingCountAfter", quota.getRemainingCount());
            s.put("details", details);

        } catch (Exception e) {
            log.error("[DEBUG] 异常: {}", e.getMessage(), e);
            s.put("result", "ERROR");
            s.put("error", e.getMessage());
        }

        log.info("========== [DEBUG] 手动推送结束 ==========");
        return Result.success(s);
    }
}
