package com.fit.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fit.entity.*;
import com.fit.mapper.*;
import com.fit.service.PushMessageHistoryService;
import com.fit.service.WxSubscribeMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜品收藏定时推送任务 —— 每天 8:00 执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DishReminderJob {

    private static final String TEMPLATE_ID = "KABRC3CxbGsD2TZQNjPWcWEl17kU1q0rNipugHkMUmA";

    /** 餐次 → 典型时间映射 */
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

    @Scheduled(cron = "0 0 8 * * ?")
    public void execute() {
        log.info("========== 菜品收藏定时推送任务开始 ==========");
        LocalDate today = LocalDate.now();

        try {
            // 1. 查询今日菜单，去重得到所有 dish_name
            LambdaQueryWrapper<CanteenMenuRecord> menuQw = new LambdaQueryWrapper<>();
            menuQw.eq(CanteenMenuRecord::getMenuDate, today);
            List<CanteenMenuRecord> todayMenus = canteenMenuRecordMapper.selectList(menuQw);

            if (todayMenus.isEmpty()) {
                log.info("今日无菜单数据，跳过推送");
                return;
            }

            // 构建 dishName → 菜单记录映射（用于获取 canteen_zone 和 meal_type）
            Map<String, List<CanteenMenuRecord>> dishMenuMap = todayMenus.stream()
                    .collect(Collectors.groupingBy(CanteenMenuRecord::getDishName));

            Set<String> todayDishNames = dishMenuMap.keySet();
            log.info("今日菜单共 {} 种菜品", todayDishNames.size());

            // 2. 匹配收藏用户：查询所有收藏了这些菜品的记录
            if (todayDishNames.isEmpty()) {
                log.info("今日菜品名称为空，跳过推送");
                return;
            }

            LambdaQueryWrapper<UserFavoriteDish> favQw = new LambdaQueryWrapper<>();
            favQw.in(UserFavoriteDish::getDishName, todayDishNames);
            List<UserFavoriteDish> matchedFavorites = userFavoriteDishMapper.selectList(favQw);

            if (matchedFavorites.isEmpty()) {
                log.info("无用户收藏今日菜品，跳过推送");
                return;
            }

            // 按 emp_no 分组
            Map<String, List<UserFavoriteDish>> empFavMap = matchedFavorites.stream()
                    .collect(Collectors.groupingBy(UserFavoriteDish::getEmpNo));

            log.info("匹配到 {} 个用户的收藏菜品", empFavMap.size());

            // 3. 过滤推送条件：remaining_count > 0 且 push_enabled = 1
            int successCount = 0;
            int failCount = 0;

            for (Map.Entry<String, List<UserFavoriteDish>> entry : empFavMap.entrySet()) {
                String empNo = entry.getKey();
                List<UserFavoriteDish> favDishes = entry.getValue();

                // 查询订阅配额
                LambdaQueryWrapper<UserSubscribeQuota> quotaQw = new LambdaQueryWrapper<>();
                quotaQw.eq(UserSubscribeQuota::getEmpNo, empNo)
                        .eq(UserSubscribeQuota::getTemplateId, TEMPLATE_ID);
                UserSubscribeQuota quota = userSubscribeQuotaMapper.selectOne(quotaQw);

                if (quota == null || quota.getRemainingCount() <= 0 || quota.getPushEnabled() != 1) {
                    log.debug("用户 {} 不满足推送条件: quota={}", empNo,
                            quota == null ? "null" :
                                    "remaining=" + quota.getRemainingCount() + ", enabled=" + quota.getPushEnabled());
                    continue;
                }

                // 查询用户 openid
                LambdaQueryWrapper<User> userQw = new LambdaQueryWrapper<>();
                userQw.eq(User::getEmpNo, empNo);
                User user = userMapper.selectOne(userQw);
                if (user == null || user.getWxOpenid() == null || user.getWxOpenid().isBlank()) {
                    log.warn("用户 {} 没有 openid，跳过推送", empNo);
                    continue;
                }

                // 4. 从收藏的菜品中拿出今日供应的，按 canteen_zone + meal_type 分组
                List<String> matchedDishNames = favDishes.stream()
                        .map(UserFavoriteDish::getDishName)
                        .collect(Collectors.toList());

                // 构建分组：Map<canteenZone_mealType, Set<dishName>>
                Map<String, Set<String>> groupMap = new LinkedHashMap<>();
                for (String dishName : matchedDishNames) {
                    List<CanteenMenuRecord> records = dishMenuMap.get(dishName);
                    if (records == null) continue;
                    for (CanteenMenuRecord record : records) {
                        String key = record.getCanteenZone() + "_" + record.getMealType();
                        groupMap.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(dishName);
                    }
                }

                // 5. 每个分组发送一条模板消息
                for (Map.Entry<String, Set<String>> groupEntry : groupMap.entrySet()) {
                    String key = groupEntry.getKey();
                    String[] parts = key.split("_", 2);
                    String canteenZone = parts[0];
                    String mealType = parts.length > 1 ? parts[1] : "";

                    Set<String> dishSet = groupEntry.getValue();
                    String dishNamesStr = String.join("、", dishSet);
                    // thing3 限制 20 个字符，超出截断加 "等"（在分隔符处截断，避免切断菜名）
                    String thing3 = dishNamesStr;
                    if (thing3.length() > 20) {
                        int cut = 18;
                        int lastSep = thing3.lastIndexOf('、', cut);
                        if (lastSep > 0) {
                            cut = lastSep;
                        }
                        thing3 = thing3.substring(0, cut) + "等";
                    }

                    String thing2 = "您收藏的菜品今日供应";
                    String time1 = MEAL_TIME_MAP.getOrDefault(mealType, "11:30");

                    // 调用微信订阅消息发送
                    Map<String, Object> sendResult = wxSubscribeMessageService.sendSubscribeMessage(
                            user.getWxOpenid(), TEMPLATE_ID, thing2, thing3, time1, "pages/index/index");

                    int errcode = (int) sendResult.getOrDefault("errcode", -1);
                    String errmsg = (String) sendResult.getOrDefault("errmsg", "未知");

                    // 6. 记录推送历史
                    PushMessageHistory history = new PushMessageHistory();
                    history.setEmpNo(empNo);
                    history.setTemplateId(TEMPLATE_ID);
                    history.setDishNames(String.join(",", dishSet));
                    history.setCanteenZone(canteenZone);
                    history.setMealType(mealType);
                    history.setMenuDate(today);

                    if (errcode == 0) {
                        history.setPushStatus("SUCCESS");
                        successCount++;

                        // 扣减次数
                        quota.setRemainingCount(quota.getRemainingCount() - 1);
                        userSubscribeQuotaMapper.updateById(quota);
                    } else {
                        history.setPushStatus("FAILED");
                        history.setErrorMessage(errmsg);
                        failCount++;
                    }

                    history.setSendTime(LocalDateTime.now());
                    pushMessageHistoryService.save(history);
                }
            }

            log.info("定时推送任务完成: 成功={}, 失败={}", successCount, failCount);
        } catch (Exception e) {
            log.error("定时推送任务异常", e);
        }

        log.info("========== 菜品收藏定时推送任务结束 ==========");
    }
}
