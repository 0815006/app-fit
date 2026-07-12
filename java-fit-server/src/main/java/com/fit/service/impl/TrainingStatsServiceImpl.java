package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fit.entity.*;
import com.fit.mapper.*;
import com.fit.service.TrainingStatsService;
import com.fit.vo.RankingItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingStatsServiceImpl implements TrainingStatsService {

    private final TrainingSessionMapper sessionMapper;
    private final TrainingSessionDetailMapper sessionDetailMapper;
    private final GymActionMapper actionMapper;
    private final GymActionMuscleRelMapper actionMuscleRelMapper;
    private final GymMuscleMapper muscleMapper;
    private final GymWorkoutRecordMapper workoutRecordMapper;
    private final UserMapper userMapper;

    // ═══════════════════════════════════════════════════════════
    // 1RM 计算
    // ═══════════════════════════════════════════════════════════

    @Override
    public BigDecimal calculateOneRepMax(BigDecimal weight, Integer reps) {
        if (weight == null || reps == null || weight.compareTo(BigDecimal.ZERO) <= 0 || reps <= 0) {
            return BigDecimal.ZERO;
        }
        // 1RM = w × (1 + r/30)
        return weight.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(reps).divide(BigDecimal.valueOf(30), 4, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getBestOneRepMax(String userId, String actionId) {
        // 获取该用户该动作所有已完成的训练详情
        List<TrainingSessionDetail> details = getCompletedDetailsByAction(userId, actionId);
        return details.stream()
                .map(d -> calculateOneRepMax(d.getActualWeight(), d.getActualReps()))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public Map<String, Object> getLastSessionDetail(String userId, String actionId) {
        // 获取该用户该动作最近一次训练数据
        List<TrainingSessionDetail> details = getCompletedDetailsByAction(userId, actionId);
        if (details.isEmpty()) {
            return Collections.emptyMap();
        }
        // 按时间倒序，取最新一条
        TrainingSessionDetail latest = details.get(0);
        Map<String, Object> result = new HashMap<>();
        result.put("actionId", latest.getActionId());
        result.put("actualWeight", latest.getActualWeight());
        result.put("actualReps", latest.getActualReps());
        result.put("oneRepMax", calculateOneRepMax(latest.getActualWeight(), latest.getActualReps()));
        result.put("setNo", latest.getSetNo());
        result.put("createTime", latest.getCreateTime());
        return result;
    }

    // ═══════════════════════════════════════════════════════════
    // 疲劳监控
    // ═══════════════════════════════════════════════════════════

    @Override
    public Map<String, Integer> calculateMuscleFatigue(String userId) {
        // 标准肌群列表
        String[] muscleGroups = {"CHEST", "BACK", "SHOULDER", "ARM", "LEG", "GLUTE", "CORE", "CARDIO"};
        Map<String, Integer> fatigue = new LinkedHashMap<>();

        // 获取所有训练记录（按日期倒序）
        LambdaQueryWrapper<TrainingSession> sessionQw = new LambdaQueryWrapper<>();
        sessionQw.eq(TrainingSession::getUserId, userId)
                .orderByDesc(TrainingSession::getSessionDate);
        List<TrainingSession> sessions = sessionMapper.selectList(sessionQw);

        if (sessions.isEmpty()) {
            // 从未训练，全部绿色
            for (String mg : muscleGroups) {
                fatigue.put(mg, -1); // -1 表示从未训练
            }
            return fatigue;
        }

        // 获取所有训练详情
        List<String> sessionIds = sessions.stream().map(TrainingSession::getId).toList();
        LambdaQueryWrapper<TrainingSessionDetail> detailQw = new LambdaQueryWrapper<>();
        detailQw.in(TrainingSessionDetail::getSessionId, sessionIds)
                .eq(TrainingSessionDetail::getIsCompleted, 1);
        List<TrainingSessionDetail> allDetails = sessionDetailMapper.selectList(detailQw);

        // 构建 actionId -> 肌群列表 映射
        Map<String, List<String>> actionMuscleMap = buildActionMuscleMap();

        // 构建 session -> sessionDate 映射
        Map<String, LocalDate> sessionDateMap = sessions.stream()
                .collect(Collectors.toMap(TrainingSession::getId, TrainingSession::getSessionDate, (a, b) -> a));

        LocalDateTime now = LocalDateTime.now();

        for (String mg : muscleGroups) {
            // 找到该肌群最近一次训练时间
            LocalDate latestDate = null;
            for (TrainingSessionDetail detail : allDetails) {
                List<String> muscles = actionMuscleMap.getOrDefault(detail.getActionId(), Collections.emptyList());
                if (muscles.contains(mg)) {
                    LocalDate d = sessionDateMap.get(detail.getSessionId());
                    if (d != null && (latestDate == null || d.isAfter(latestDate))) {
                        latestDate = d;
                    }
                }
            }
            if (latestDate == null) {
                fatigue.put(mg, -1); // 从未训练
            } else {
                int hours = (int) Duration.between(latestDate.atStartOfDay(), now).toHours();
                fatigue.put(mg, hours);
            }
        }
        return fatigue;
    }

    // ═══════════════════════════════════════════════════════════
    // 训练趋势 & 贡献墙
    // ═══════════════════════════════════════════════════════════

    @Override
    public List<Map<String, Object>> getVolumeTrend(String userId, String groupBy, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<TrainingSession> qw = new LambdaQueryWrapper<>();
        qw.eq(TrainingSession::getUserId, userId);
        if (startDate != null) qw.ge(TrainingSession::getSessionDate, startDate);
        if (endDate != null) qw.le(TrainingSession::getSessionDate, endDate);
        qw.orderByAsc(TrainingSession::getSessionDate);
        List<TrainingSession> sessions = sessionMapper.selectList(qw);

        // 按周/月分组统计总容量
        Map<String, BigDecimal> grouped = new LinkedHashMap<>();
        for (TrainingSession s : sessions) {
            String key;
            if ("month".equals(groupBy)) {
                key = s.getSessionDate().getYear() + "-" + String.format("%02d", s.getSessionDate().getMonthValue());
            } else {
                // 按周：使用 ISO 周
                int weekNum = s.getSessionDate().get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                int year = s.getSessionDate().get(java.time.temporal.IsoFields.WEEK_BASED_YEAR);
                key = year + "-W" + String.format("%02d", weekNum);
            }
            BigDecimal vol = s.getTotalVolume() != null ? s.getTotalVolume() : BigDecimal.ZERO;
            grouped.merge(key, vol, BigDecimal::add);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((k, v) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("period", k);
            item.put("volume", v.setScale(2, RoundingMode.HALF_UP));
            result.add(item);
        });
        return result;
    }

    @Override
    public List<Map<String, Object>> getContributionWall(String userId, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        LambdaQueryWrapper<TrainingSession> qw = new LambdaQueryWrapper<>();
        qw.eq(TrainingSession::getUserId, userId)
                .ge(TrainingSession::getSessionDate, start)
                .le(TrainingSession::getSessionDate, end);
        List<TrainingSession> sessions = sessionMapper.selectList(qw);

        // 按日期统计训练量
        Map<LocalDate, Integer> dateCount = new HashMap<>();
        for (TrainingSession s : sessions) {
            dateCount.merge(s.getSessionDate(), 1, Integer::sum);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", d.toString());
            item.put("count", dateCount.getOrDefault(d, 0));
            // 星期几（0=周一, 6=周日）
            item.put("dayOfWeek", d.getDayOfWeek().getValue());
            result.add(item);
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════
    // 榜单
    // ═══════════════════════════════════════════════════════════

    @Override
    public List<Map<String, Object>> getConsistencyRanking(int days) {
        LocalDate since = LocalDate.now().minusDays(days);

        // 查询所有在指定时间范围内有训练记录的用户
        LambdaQueryWrapper<TrainingSession> qw = new LambdaQueryWrapper<>();
        qw.ge(TrainingSession::getSessionDate, since);
        List<TrainingSession> sessions = sessionMapper.selectList(qw);

        // 按用户统计训练天数
        Map<String, Set<LocalDate>> userDays = new HashMap<>();
        for (TrainingSession s : sessions) {
            userDays.computeIfAbsent(s.getUserId(), k -> new HashSet<>()).add(s.getSessionDate());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        userDays.forEach((userId, daySet) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("userId", userId);
            item.put("days", daySet.size());
            result.add(item);
        });
        result.sort((a, b) -> Integer.compare((int) b.get("days"), (int) a.get("days")));
        return result;
    }

    @Override
    public List<Map<String, Object>> getProgressRanking(int days) {
        LocalDate since = LocalDate.now().minusDays(days);
        LocalDate before = since.minusDays(days); // 对比前一个周期

        // 获取所有训练详情（关联 session 获取日期）
        LambdaQueryWrapper<TrainingSession> sessionQw = new LambdaQueryWrapper<>();
        sessionQw.ge(TrainingSession::getSessionDate, before);
        List<TrainingSession> sessions = sessionMapper.selectList(sessionQw);
        Map<String, LocalDate> sessionDateMap = sessions.stream()
                .collect(Collectors.toMap(TrainingSession::getId, TrainingSession::getSessionDate, (a, b) -> a));

        List<String> sessionIds = sessions.stream().map(TrainingSession::getId).toList();
        if (sessionIds.isEmpty()) return Collections.emptyList();

        LambdaQueryWrapper<TrainingSessionDetail> detailQw = new LambdaQueryWrapper<>();
        detailQw.in(TrainingSessionDetail::getSessionId, sessionIds)
                .eq(TrainingSessionDetail::getIsCompleted, 1);
        List<TrainingSessionDetail> details = sessionDetailMapper.selectList(detailQw);

        // 按用户分组，计算前后两个周期的1RM
        Map<String, Map<String, List<TrainingSessionDetail>>> userActionDetails = new HashMap<>();
        for (TrainingSessionDetail d : details) {
            String userId = sessionDateMap.get(d.getSessionId()) != null ?
                    sessions.stream().filter(s -> s.getId().equals(d.getSessionId())).findFirst().map(TrainingSession::getUserId).orElse("") : "";
            if (userId.isEmpty()) continue;
            userActionDetails.computeIfAbsent(userId, k -> new HashMap<>())
                    .computeIfAbsent(d.getActionId(), k -> new ArrayList<>()).add(d);
        }

        // 三大项动作名称关键字（深蹲、卧推、硬拉）
        List<GymAction> allActions = actionMapper.selectList(null);
        Map<String, String> actionNameMap = allActions.stream()
                .collect(Collectors.toMap(GymAction::getId, GymAction::getName, (a, b) -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        userActionDetails.forEach((userId, actionMap) -> {
            BigDecimal totalBefore = BigDecimal.ZERO;
            BigDecimal totalAfter = BigDecimal.ZERO;
            int count = 0;

            for (Map.Entry<String, List<TrainingSessionDetail>> entry : actionMap.entrySet()) {
                String actionId = entry.getKey();
                String actionName = actionNameMap.getOrDefault(actionId, "");

                // 只统计三大项
                if (!isBigThree(actionName)) return;

                List<TrainingSessionDetail> actionDetails = entry.getValue();
                BigDecimal bestBefore = BigDecimal.ZERO;
                BigDecimal bestAfter = BigDecimal.ZERO;

                for (TrainingSessionDetail d : actionDetails) {
                    LocalDate date = sessionDateMap.get(d.getSessionId());
                    BigDecimal orm = calculateOneRepMax(d.getActualWeight(), d.getActualReps());
                    if (date != null && date.isBefore(since)) {
                        bestBefore = bestBefore.max(orm);
                    } else if (date != null) {
                        bestAfter = bestAfter.max(orm);
                    }
                }
                if (bestBefore.compareTo(BigDecimal.ZERO) > 0) {
                    totalBefore = totalBefore.add(bestBefore);
                    totalAfter = totalAfter.add(bestAfter);
                    count++;
                }
            }

            if (count > 0 && totalBefore.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal growthRate = totalAfter.subtract(totalBefore)
                        .divide(totalBefore, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
                Map<String, Object> item = new HashMap<>();
                item.put("userId", userId);
                item.put("growthRate", growthRate);
                item.put("beforeTotal", totalBefore.setScale(2, RoundingMode.HALF_UP));
                item.put("afterTotal", totalAfter.setScale(2, RoundingMode.HALF_UP));
                result.add(item);
            }
        });

        result.sort((a, b) -> ((BigDecimal) b.get("growthRate")).compareTo((BigDecimal) a.get("growthRate")));
        return result;
    }

    // ═══════════════════════════════════════════════════════════
    // V2 榜单（基于 gym_workout_record 新字段）
    // ═══════════════════════════════════════════════════════════

    @Override
    public List<RankingItemVO> getConsistencyRankingV2(int days) {
        // 坚持榜：周期内累计打卡天数排名，所有动作类型都计入
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        List<GymWorkoutRecord> records = workoutRecordMapper.selectList(
                new LambdaQueryWrapper<GymWorkoutRecord>()
                        .select(GymWorkoutRecord::getUserId, GymWorkoutRecord::getStartTime)
                        .in(GymWorkoutRecord::getStatus, 1, 2)
                        .ge(GymWorkoutRecord::getStartTime, since));

        // 按用户统计去重的训练天数
        Map<String, Set<LocalDate>> userDays = new HashMap<>();
        for (GymWorkoutRecord r : records) {
            userDays.computeIfAbsent(r.getUserId(), k -> new HashSet<>())
                    .add(r.getStartTime().toLocalDate());
        }

        // 批量加载用户信息
        Map<String, User> userMap = loadUserMap(userDays.keySet());

        List<RankingItemVO> result = new ArrayList<>();
        int[] rank = {0};
        userDays.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .forEach(entry -> {
                    rank[0]++;
                    String userId = entry.getKey();
                    User user = userMap.get(userId);
                    result.add(RankingItemVO.builder()
                            .rank(rank[0])
                            .userId(userId)
                            .empName(user != null ? user.getEmpName() : userId)
                            .empNo(user != null ? user.getEmpNo() : "")
                            .avatarUrl(user != null ? user.getAvatarUrl() : "")
                            .value(BigDecimal.valueOf(entry.getValue().size()))
                            .build());
                });
        return result;
    }

    @Override
    public List<RankingItemVO> getVolumeRanking(int days) {
        // 容量榜：SUM(weight * reps * set_count)
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        List<GymWorkoutRecord> records = workoutRecordMapper.selectList(
                new LambdaQueryWrapper<GymWorkoutRecord>()
                        .select(GymWorkoutRecord::getUserId,
                                GymWorkoutRecord::getWeight,
                                GymWorkoutRecord::getReps,
                                GymWorkoutRecord::getSetCount)
                        .in(GymWorkoutRecord::getStatus, 1, 2)
                        .ge(GymWorkoutRecord::getStartTime, since)
                        .isNotNull(GymWorkoutRecord::getWeight)
                        .isNotNull(GymWorkoutRecord::getReps)
                        .isNotNull(GymWorkoutRecord::getSetCount));

        // 按用户聚合总容量
        Map<String, BigDecimal> userVolume = new HashMap<>();
        for (GymWorkoutRecord r : records) {
            if (r.getWeight().compareTo(BigDecimal.ZERO) <= 0
                    || r.getReps() <= 0 || r.getSetCount() <= 0) continue;
            BigDecimal vol = r.getWeight()
                    .multiply(BigDecimal.valueOf(r.getReps()))
                    .multiply(BigDecimal.valueOf(r.getSetCount()));
            userVolume.merge(r.getUserId(), vol, BigDecimal::add);
        }

        Map<String, User> userMap = loadUserMap(userVolume.keySet());

        List<RankingItemVO> result = new ArrayList<>();
        int[] rank = {0};
        userVolume.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(entry -> {
                    rank[0]++;
                    String userId = entry.getKey();
                    User user = userMap.get(userId);
                    result.add(RankingItemVO.builder()
                            .rank(rank[0])
                            .userId(userId)
                            .empName(user != null ? user.getEmpName() : userId)
                            .empNo(user != null ? user.getEmpNo() : "")
                            .avatarUrl(user != null ? user.getAvatarUrl() : "")
                            .value(entry.getValue().setScale(2, RoundingMode.HALF_UP))
                            .build());
                });
        return result;
    }

    @Override
    public List<RankingItemVO> getPeak1RMRanking(int days, String lift) {
        // 1RM巅峰榜：单次最大 1RM 排行（深蹲/卧推/硬拉/三大项之和）
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        List<String> targetActionIds = resolveLiftActionIds(lift);

        List<GymWorkoutRecord> records = workoutRecordMapper.selectList(
                new LambdaQueryWrapper<GymWorkoutRecord>()
                        .select(GymWorkoutRecord::getUserId,
                                GymWorkoutRecord::getActionId,
                                GymWorkoutRecord::getRmEstimate)
                        .in(GymWorkoutRecord::getStatus, 1, 2)
                        .ge(GymWorkoutRecord::getStartTime, since)
                        .in(GymWorkoutRecord::getActionId, targetActionIds)
                        .isNotNull(GymWorkoutRecord::getRmEstimate));

        // 每用户每动作取单次最大 rmEstimate
        Map<String, Map<String, BigDecimal>> userActionMax = new HashMap<>();
        for (GymWorkoutRecord r : records) {
            userActionMax.computeIfAbsent(r.getUserId(), k -> new HashMap<>())
                    .merge(r.getActionId(), r.getRmEstimate(),
                            (old, val) -> val.compareTo(old) > 0 ? val : old);
        }

        // 汇总：单动作取对应值，all 取三动作之和
        Map<String, BigDecimal> userTotal = new HashMap<>();
        for (Map.Entry<String, Map<String, BigDecimal>> entry : userActionMax.entrySet()) {
            if ("all".equals(lift)) {
                BigDecimal sum = entry.getValue().values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                userTotal.put(entry.getKey(), sum);
            } else {
                BigDecimal singleMax = entry.getValue().values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::max);
                userTotal.put(entry.getKey(), singleMax);
            }
        }

        Map<String, User> userMap = loadUserMap(userTotal.keySet());

        List<RankingItemVO> result = new ArrayList<>();
        int[] rank = {0};
        userTotal.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(entry -> {
                    rank[0]++;
                    String userId = entry.getKey();
                    User user = userMap.get(userId);
                    result.add(RankingItemVO.builder()
                            .rank(rank[0])
                            .userId(userId)
                            .empName(user != null ? user.getEmpName() : userId)
                            .empNo(user != null ? user.getEmpNo() : "")
                            .avatarUrl(user != null ? user.getAvatarUrl() : "")
                            .value(entry.getValue().setScale(2, RoundingMode.HALF_UP))
                            .build());
                });
        return result;
    }

    @Override
    public List<RankingItemVO> getProgressRankingV2(int days, String lift) {
        // 进步榜：前后两个周期单次最大 1RM 对比（深蹲/卧推/硬拉/三大项之和）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentStart = now.minusDays(days);
        LocalDateTime previousStart = now.minusDays((long) days * 2);

        List<String> targetActionIds = resolveLiftActionIds(lift);

        // 查限定动作的 rmEstimate 记录（两个周期内）
        List<GymWorkoutRecord> records = workoutRecordMapper.selectList(
                new LambdaQueryWrapper<GymWorkoutRecord>()
                        .select(GymWorkoutRecord::getUserId,
                                GymWorkoutRecord::getActionId,
                                GymWorkoutRecord::getRmEstimate,
                                GymWorkoutRecord::getStartTime)
                        .in(GymWorkoutRecord::getStatus, 1, 2)
                        .ge(GymWorkoutRecord::getStartTime, previousStart)
                        .in(GymWorkoutRecord::getActionId, targetActionIds)
                        .isNotNull(GymWorkoutRecord::getRmEstimate));

        // 分前后周期聚合每用户每动作的单次最大 rmEstimate
        Map<String, Map<String, BigDecimal>> prevBest = new HashMap<>();
        Map<String, Map<String, BigDecimal>> currBest = new HashMap<>();

        for (GymWorkoutRecord r : records) {
            boolean isCurrent = !r.getStartTime().isBefore(currentStart);
            Map<String, Map<String, BigDecimal>> target = isCurrent ? currBest : prevBest;
            target.computeIfAbsent(r.getUserId(), k -> new HashMap<>())
                    .merge(r.getActionId(), r.getRmEstimate(),
                            (old, val) -> val.compareTo(old) > 0 ? val : old);
        }

        // 计算增长率：单动作取最大值，all 取三动作之和对比
        Map<String, BigDecimal> userGrowthRate = new HashMap<>();
        for (String userId : currBest.keySet()) {
            Map<String, BigDecimal> prev = prevBest.get(userId);
            if (prev == null || prev.isEmpty()) continue;
            BigDecimal prevVal = "all".equals(lift)
                    ? prev.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    : prev.values().stream().reduce(BigDecimal.ZERO, BigDecimal::max);
            if (prevVal.compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal currVal = "all".equals(lift)
                    ? currBest.get(userId).values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    : currBest.get(userId).values().stream().reduce(BigDecimal.ZERO, BigDecimal::max);
            BigDecimal rate = currVal.subtract(prevVal)
                    .divide(prevVal, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            userGrowthRate.put(userId, rate);
        }

        Map<String, User> userMap = loadUserMap(userGrowthRate.keySet());

        List<RankingItemVO> result = new ArrayList<>();
        int[] rank = {0};
        userGrowthRate.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(entry -> {
                    rank[0]++;
                    String userId = entry.getKey();
                    User user = userMap.get(userId);
                    result.add(RankingItemVO.builder()
                            .rank(rank[0])
                            .userId(userId)
                            .empName(user != null ? user.getEmpName() : userId)
                            .empNo(user != null ? user.getEmpNo() : "")
                            .avatarUrl(user != null ? user.getAvatarUrl() : "")
                            .value(entry.getValue())
                            .trend(entry.getValue().compareTo(BigDecimal.ZERO) > 0 ? "↑" : "↓")
                            .build());
                });
        return result;
    }

    @Override
    public List<RankingItemVO> getMaxSingleVolumeRanking(int days, String liftType) {
        // 容量榜：单次最大容量（深蹲/卧推/硬拉/三大项之和）
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        List<String> targetActionIds = resolveLiftActionIds(liftType);

        List<GymWorkoutRecord> records = workoutRecordMapper.selectList(
                new LambdaQueryWrapper<GymWorkoutRecord>()
                        .select(GymWorkoutRecord::getUserId,
                                GymWorkoutRecord::getActionId,
                                GymWorkoutRecord::getWeight,
                                GymWorkoutRecord::getReps,
                                GymWorkoutRecord::getSetCount)
                        .in(GymWorkoutRecord::getActionId, targetActionIds)
                        .in(GymWorkoutRecord::getStatus, 1, 2)
                        .ge(GymWorkoutRecord::getStartTime, since)
                        .isNotNull(GymWorkoutRecord::getWeight)
                        .isNotNull(GymWorkoutRecord::getReps)
                        .isNotNull(GymWorkoutRecord::getSetCount));

        // 每用户每动作取单次最大容量
        Map<String, Map<String, BigDecimal>> userActionMax = new HashMap<>();
        for (GymWorkoutRecord r : records) {
            if (r.getWeight().compareTo(BigDecimal.ZERO) <= 0
                    || r.getReps() <= 0 || r.getSetCount() <= 0) continue;
            BigDecimal vol = r.getWeight()
                    .multiply(BigDecimal.valueOf(r.getReps()))
                    .multiply(BigDecimal.valueOf(r.getSetCount()));
            userActionMax.computeIfAbsent(r.getUserId(), k -> new HashMap<>())
                    .merge(r.getActionId(), vol,
                            (old, val) -> val.compareTo(old) > 0 ? val : old);
        }

        // 汇总：单动作取对应值，all 取三动作之和
        Map<String, BigDecimal> userVolume = new HashMap<>();
        for (Map.Entry<String, Map<String, BigDecimal>> entry : userActionMax.entrySet()) {
            if ("all".equals(liftType)) {
                BigDecimal sum = entry.getValue().values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                userVolume.put(entry.getKey(), sum);
            } else {
                BigDecimal singleMax = entry.getValue().values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::max);
                userVolume.put(entry.getKey(), singleMax);
            }
        }

        Map<String, User> userMap = loadUserMap(userVolume.keySet());

        List<RankingItemVO> result = new ArrayList<>();
        int[] rank = {0};
        userVolume.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(entry -> {
                    rank[0]++;
                    String userId = entry.getKey();
                    User user = userMap.get(userId);
                    result.add(RankingItemVO.builder()
                            .rank(rank[0])
                            .userId(userId)
                            .empName(user != null ? user.getEmpName() : userId)
                            .empNo(user != null ? user.getEmpNo() : "")
                            .avatarUrl(user != null ? user.getAvatarUrl() : "")
                            .value(entry.getValue().setScale(2, RoundingMode.HALF_UP))
                            .build());
                });
        return result;
    }

    // ═══════════════════════════════════════════════════════════
    // V2 榜单辅助
    // ═══════════════════════════════════════════════════════════

    /** 三大项 actionId 常量映射 */
    private static final String BIG3_BENCH = "ACT1007";
    private static final String BIG3_SQUAT = "ACT3001";
    private static final String BIG3_DEADLIFT = "ACT2013";
    private static final List<String> ALL_BIG3_IDS = List.of(BIG3_BENCH, BIG3_SQUAT, BIG3_DEADLIFT);

    /**
     * 根据 lift 参数解析目标动作 ID 列表
     */
    private List<String> resolveLiftActionIds(String lift) {
        return switch (lift) {
            case "squat" -> List.of(BIG3_SQUAT);
            case "deadlift" -> List.of(BIG3_DEADLIFT);
            case "all" -> ALL_BIG3_IDS;
            default -> List.of(BIG3_BENCH); // bench
        };
    }

    /**
     * 根据 userId 集合批量加载 User，返回 userId → User 映射
     */
    private Map<String, User> loadUserMap(Collection<String> userIds) {
        if (userIds.isEmpty()) return Map.of();
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().in(User::getId, userIds));
        return users.stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    }

    // ═══════════════════════════════════════════════════════════
    // 平台期检测
    // ═══════════════════════════════════════════════════════════

    @Override
    public List<Map<String, Object>> detectPlateau(String userId, int weeks) {
        LocalDate since = LocalDate.now().minusWeeks(weeks);

        // 获取该用户所有训练详情
        LambdaQueryWrapper<TrainingSession> sessionQw = new LambdaQueryWrapper<>();
        sessionQw.eq(TrainingSession::getUserId, userId)
                .ge(TrainingSession::getSessionDate, since);
        List<TrainingSession> sessions = sessionMapper.selectList(sessionQw);
        if (sessions.isEmpty()) return Collections.emptyList();

        List<String> sessionIds = sessions.stream().map(TrainingSession::getId).toList();
        LambdaQueryWrapper<TrainingSessionDetail> detailQw = new LambdaQueryWrapper<>();
        detailQw.in(TrainingSessionDetail::getSessionId, sessionIds)
                .eq(TrainingSessionDetail::getIsCompleted, 1);
        List<TrainingSessionDetail> details = sessionDetailMapper.selectList(detailQw);

        // 按动作分组，检测是否停滞
        Map<String, List<TrainingSessionDetail>> actionDetails = details.stream()
                .collect(Collectors.groupingBy(TrainingSessionDetail::getActionId));

        Map<String, String> actionNameMap = actionMapper.selectList(null).stream()
                .collect(Collectors.toMap(GymAction::getId, GymAction::getName, (a, b) -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        actionDetails.forEach((actionId, actionDetailList) -> {
            if (actionDetailList.size() < 3) return; // 数据不足，跳过

            // 按周分组计算总容量
            Map<Integer, BigDecimal> weeklyVolume = new TreeMap<>();
            Map<String, LocalDate> sessionDateMap = sessions.stream()
                    .collect(Collectors.toMap(TrainingSession::getId, TrainingSession::getSessionDate, (a, b) -> a));

            for (TrainingSessionDetail d : actionDetailList) {
                LocalDate date = sessionDateMap.get(d.getSessionId());
                if (date == null) continue;
                int weekNum = date.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                BigDecimal vol = (d.getActualWeight() != null ? d.getActualWeight() : BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(d.getActualReps() != null ? d.getActualReps() : 0));
                weeklyVolume.merge(weekNum, vol, BigDecimal::add);
            }

            // 检查连续N周是否无增长
            if (weeklyVolume.size() >= weeks) {
                List<BigDecimal> volumes = new ArrayList<>(weeklyVolume.values());
                boolean plateau = true;
                for (int i = 1; i < volumes.size(); i++) {
                    if (volumes.get(i).compareTo(volumes.get(i - 1)) > 0) {
                        plateau = false;
                        break;
                    }
                }
                if (plateau) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("actionId", actionId);
                    item.put("actionName", actionNameMap.getOrDefault(actionId, "未知动作"));
                    item.put("weeks", weeklyVolume.size());
                    item.put("latestVolume", volumes.get(volumes.size() - 1).setScale(2, RoundingMode.HALF_UP));
                    result.add(item);
                }
            }
        });
        return result;
    }

    // ═══════════════════════════════════════════════════════════
    // PR 检测
    // ═══════════════════════════════════════════════════════════

    @Override
    public boolean checkIsPr(String userId, String actionId, BigDecimal weight, Integer reps) {
        BigDecimal currentOrm = calculateOneRepMax(weight, reps);
        BigDecimal bestOrm = getBestOneRepMax(userId, actionId);
        // 如果当前1RM大于历史最佳（排除当前记录），则为PR
        // 注意：这里 getBestOneRepMax 会包含当前刚保存的记录，所以需要比较的是"之前的最佳"
        // 简化处理：如果当前1RM >= 历史最佳，就认为是PR
        return currentOrm.compareTo(BigDecimal.ZERO) > 0 && currentOrm.compareTo(bestOrm) >= 0;
    }

    // ═══════════════════════════════════════════════════════════
    // 力量对比
    // ═══════════════════════════════════════════════════════════

    @Override
    public Map<String, Object> getStrengthComparison(String userId1, String userId2) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId1", userId1);
        result.put("userId2", userId2);

        // 总搬运吨位
        result.put("totalVolume1", getTotalVolume(userId1));
        result.put("totalVolume2", getTotalVolume(userId2));

        // 三大项最高重量
        Map<String, String> bigThreeActions = findBigThreeActions();
        List<Map<String, Object>> comparison = new ArrayList<>();
        bigThreeActions.forEach((name, actionId) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("actionName", name);
            item.put("best1", getBestOneRepMax(userId1, actionId));
            item.put("best2", getBestOneRepMax(userId2, actionId));
            comparison.add(item);
        });
        result.put("bigThree", comparison);

        // 力量平衡雷达图（胸、背、腿、肩、核心 五个部位的训练总容量比重）
        result.put("radar1", getStrengthRadar(userId1));
        result.put("radar2", getStrengthRadar(userId2));

        return result;
    }

    // ═══════════════════════════════════════════════════════════
    // 私有辅助方法
    // ═══════════════════════════════════════════════════════════

    private List<TrainingSessionDetail> getCompletedDetailsByAction(String userId, String actionId) {
        // 先获取该用户所有session
        LambdaQueryWrapper<TrainingSession> sessionQw = new LambdaQueryWrapper<>();
        sessionQw.eq(TrainingSession::getUserId, userId);
        List<TrainingSession> sessions = sessionMapper.selectList(sessionQw);
        if (sessions.isEmpty()) return Collections.emptyList();

        List<String> sessionIds = sessions.stream().map(TrainingSession::getId).toList();
        LambdaQueryWrapper<TrainingSessionDetail> detailQw = new LambdaQueryWrapper<>();
        detailQw.in(TrainingSessionDetail::getSessionId, sessionIds)
                .eq(TrainingSessionDetail::getActionId, actionId)
                .eq(TrainingSessionDetail::getIsCompleted, 1)
                .orderByDesc(TrainingSessionDetail::getCreateTime);
        return sessionDetailMapper.selectList(detailQw);
    }

    private Map<String, List<String>> buildActionMuscleMap() {
        List<GymActionMuscleRel> rels = actionMuscleRelMapper.selectList(null);
        List<GymMuscle> muscles = muscleMapper.selectList(null);
        Map<String, String> muscleNameToGroup = muscles.stream()
                .collect(Collectors.toMap(GymMuscle::getId, m -> {
                    // 将具体肌肉映射到肌群
                    String name = m.getMuscleName();
                    if (name.contains("胸")) return "CHEST";
                    if (name.contains("背") || name.contains("斜方")) return "BACK";
                    if (name.contains("肩") || name.contains("三角")) return "SHOULDER";
                    if (name.contains("臂") || name.contains("肱") || name.contains("前臂")) return "ARM";
                    if (name.contains("腿") || name.contains("股四") || name.contains("腘绳") || name.contains("臀") || name.contains("小腿")) return "LEG";
                    if (name.contains("臀")) return "GLUTE";
                    if (name.contains("腹") || name.contains("核心")) return "CORE";
                    return "FULL_BODY";
                }, (a, b) -> a));

        Map<String, List<String>> result = new HashMap<>();
        for (GymActionMuscleRel rel : rels) {
            if (rel.getIsPrimary() != null && rel.getIsPrimary() == 1) {
                String group = muscleNameToGroup.getOrDefault(rel.getMuscleId(), "FULL_BODY");
                result.computeIfAbsent(rel.getActionId(), k -> new ArrayList<>()).add(group);
            }
        }
        return result;
    }

    private boolean isBigThree(String actionName) {
        return actionName.contains("深蹲") || actionName.contains("卧推") || actionName.contains("硬拉");
    }

    private Map<String, String> findBigThreeActions() {
        Map<String, String> result = new LinkedHashMap<>();
        List<GymAction> actions = actionMapper.selectList(null);
        for (GymAction a : actions) {
            if (a.getName().contains("深蹲") && !result.containsKey("深蹲")) result.put("深蹲", a.getId());
            if (a.getName().contains("卧推") && !result.containsKey("卧推")) result.put("卧推", a.getId());
            if (a.getName().contains("硬拉") && !result.containsKey("硬拉")) result.put("硬拉", a.getId());
        }
        return result;
    }

    private BigDecimal getTotalVolume(String userId) {
        LambdaQueryWrapper<TrainingSession> qw = new LambdaQueryWrapper<>();
        qw.eq(TrainingSession::getUserId, userId);
        List<TrainingSession> sessions = sessionMapper.selectList(qw);
        return sessions.stream()
                .map(s -> s.getTotalVolume() != null ? s.getTotalVolume() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> getStrengthRadar(String userId) {
        String[] groups = {"CHEST", "BACK", "LEG", "SHOULDER", "CORE"};
        String[] groupNames = {"胸", "背", "腿", "肩", "核心"};
        Map<String, BigDecimal> radar = new LinkedHashMap<>();

        // 获取所有训练详情
        LambdaQueryWrapper<TrainingSession> sessionQw = new LambdaQueryWrapper<>();
        sessionQw.eq(TrainingSession::getUserId, userId);
        List<TrainingSession> sessions = sessionMapper.selectList(sessionQw);
        Map<String, LocalDate> sessionDateMap = sessions.stream()
                .collect(Collectors.toMap(TrainingSession::getId, TrainingSession::getSessionDate, (a, b) -> a));

        if (sessions.isEmpty()) {
            for (String gn : groupNames) radar.put(gn, BigDecimal.ZERO);
            return radar;
        }

        List<String> sessionIds = sessions.stream().map(TrainingSession::getId).toList();
        LambdaQueryWrapper<TrainingSessionDetail> detailQw = new LambdaQueryWrapper<>();
        detailQw.in(TrainingSessionDetail::getSessionId, sessionIds)
                .eq(TrainingSessionDetail::getIsCompleted, 1);
        List<TrainingSessionDetail> details = sessionDetailMapper.selectList(detailQw);

        Map<String, List<String>> actionMuscleMap = buildActionMuscleMap();

        Map<String, BigDecimal> groupVolume = new HashMap<>();
        for (String gn : groupNames) groupVolume.put(gn, BigDecimal.ZERO);

        for (TrainingSessionDetail d : details) {
            BigDecimal vol = (d.getActualWeight() != null ? d.getActualWeight() : BigDecimal.ZERO)
                    .multiply(BigDecimal.valueOf(d.getActualReps() != null ? d.getActualReps() : 0));
            List<String> muscles = actionMuscleMap.getOrDefault(d.getActionId(), Collections.emptyList());
            for (int i = 0; i < groups.length; i++) {
                if (muscles.contains(groups[i])) {
                    groupVolume.merge(groupNames[i], vol, BigDecimal::add);
                }
            }
        }

        // 计算比重
        BigDecimal total = groupVolume.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        for (String gn : groupNames) {
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                radar.put(gn, groupVolume.get(gn).divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP));
            } else {
                radar.put(gn, BigDecimal.ZERO);
            }
        }
        return radar;
    }
}
