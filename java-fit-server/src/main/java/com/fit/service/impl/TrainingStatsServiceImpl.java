package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fit.entity.*;
import com.fit.mapper.*;
import com.fit.service.TrainingStatsService;
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
    public BigDecimal getBestOneRepMax(String empNo, String actionId) {
        // 获取该用户该动作所有已完成的训练详情
        List<TrainingSessionDetail> details = getCompletedDetailsByAction(empNo, actionId);
        return details.stream()
                .map(d -> calculateOneRepMax(d.getActualWeight(), d.getActualReps()))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public Map<String, Object> getLastSessionDetail(String empNo, String actionId) {
        // 获取该用户该动作最近一次训练数据
        List<TrainingSessionDetail> details = getCompletedDetailsByAction(empNo, actionId);
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
    public Map<String, Integer> calculateMuscleFatigue(String empNo) {
        // 标准肌群列表
        String[] muscleGroups = {"CHEST", "BACK", "SHOULDER", "ARM", "LEG", "GLUTE", "CORE"};
        Map<String, Integer> fatigue = new LinkedHashMap<>();

        // 获取所有训练记录（按日期倒序）
        LambdaQueryWrapper<TrainingSession> sessionQw = new LambdaQueryWrapper<>();
        sessionQw.eq(TrainingSession::getEmpNo, empNo)
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
    public List<Map<String, Object>> getVolumeTrend(String empNo, String groupBy, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<TrainingSession> qw = new LambdaQueryWrapper<>();
        qw.eq(TrainingSession::getEmpNo, empNo);
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
    public List<Map<String, Object>> getContributionWall(String empNo, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        LambdaQueryWrapper<TrainingSession> qw = new LambdaQueryWrapper<>();
        qw.eq(TrainingSession::getEmpNo, empNo)
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
            userDays.computeIfAbsent(s.getEmpNo(), k -> new HashSet<>()).add(s.getSessionDate());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        userDays.forEach((empNo, daySet) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("empNo", empNo);
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
            String empNo = sessionDateMap.get(d.getSessionId()) != null ?
                    sessions.stream().filter(s -> s.getId().equals(d.getSessionId())).findFirst().map(TrainingSession::getEmpNo).orElse("") : "";
            if (empNo.isEmpty()) continue;
            userActionDetails.computeIfAbsent(empNo, k -> new HashMap<>())
                    .computeIfAbsent(d.getActionId(), k -> new ArrayList<>()).add(d);
        }

        // 三大项动作名称关键字（深蹲、卧推、硬拉）
        List<GymAction> allActions = actionMapper.selectList(null);
        Map<String, String> actionNameMap = allActions.stream()
                .collect(Collectors.toMap(GymAction::getId, GymAction::getName, (a, b) -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        userActionDetails.forEach((empNo, actionMap) -> {
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
                item.put("empNo", empNo);
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
    // 平台期检测
    // ═══════════════════════════════════════════════════════════

    @Override
    public List<Map<String, Object>> detectPlateau(String empNo, int weeks) {
        LocalDate since = LocalDate.now().minusWeeks(weeks);

        // 获取该用户所有训练详情
        LambdaQueryWrapper<TrainingSession> sessionQw = new LambdaQueryWrapper<>();
        sessionQw.eq(TrainingSession::getEmpNo, empNo)
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
    public boolean checkIsPr(String empNo, String actionId, BigDecimal weight, Integer reps) {
        BigDecimal currentOrm = calculateOneRepMax(weight, reps);
        BigDecimal bestOrm = getBestOneRepMax(empNo, actionId);
        // 如果当前1RM大于历史最佳（排除当前记录），则为PR
        // 注意：这里 getBestOneRepMax 会包含当前刚保存的记录，所以需要比较的是"之前的最佳"
        // 简化处理：如果当前1RM >= 历史最佳，就认为是PR
        return currentOrm.compareTo(BigDecimal.ZERO) > 0 && currentOrm.compareTo(bestOrm) >= 0;
    }

    // ═══════════════════════════════════════════════════════════
    // 力量对比
    // ═══════════════════════════════════════════════════════════

    @Override
    public Map<String, Object> getStrengthComparison(String empNo1, String empNo2) {
        Map<String, Object> result = new HashMap<>();
        result.put("empNo1", empNo1);
        result.put("empNo2", empNo2);

        // 总搬运吨位
        result.put("totalVolume1", getTotalVolume(empNo1));
        result.put("totalVolume2", getTotalVolume(empNo2));

        // 三大项最高重量
        Map<String, String> bigThreeActions = findBigThreeActions();
        List<Map<String, Object>> comparison = new ArrayList<>();
        bigThreeActions.forEach((name, actionId) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("actionName", name);
            item.put("best1", getBestOneRepMax(empNo1, actionId));
            item.put("best2", getBestOneRepMax(empNo2, actionId));
            comparison.add(item);
        });
        result.put("bigThree", comparison);

        // 力量平衡雷达图（胸、背、腿、肩、核心 五个部位的训练总容量比重）
        result.put("radar1", getStrengthRadar(empNo1));
        result.put("radar2", getStrengthRadar(empNo2));

        return result;
    }

    // ═══════════════════════════════════════════════════════════
    // 私有辅助方法
    // ═══════════════════════════════════════════════════════════

    private List<TrainingSessionDetail> getCompletedDetailsByAction(String empNo, String actionId) {
        // 先获取该用户所有session
        LambdaQueryWrapper<TrainingSession> sessionQw = new LambdaQueryWrapper<>();
        sessionQw.eq(TrainingSession::getEmpNo, empNo);
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

    private BigDecimal getTotalVolume(String empNo) {
        LambdaQueryWrapper<TrainingSession> qw = new LambdaQueryWrapper<>();
        qw.eq(TrainingSession::getEmpNo, empNo);
        List<TrainingSession> sessions = sessionMapper.selectList(qw);
        return sessions.stream()
                .map(s -> s.getTotalVolume() != null ? s.getTotalVolume() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> getStrengthRadar(String empNo) {
        String[] groups = {"CHEST", "BACK", "LEG", "SHOULDER", "CORE"};
        String[] groupNames = {"胸", "背", "腿", "肩", "核心"};
        Map<String, BigDecimal> radar = new LinkedHashMap<>();

        // 获取所有训练详情
        LambdaQueryWrapper<TrainingSession> sessionQw = new LambdaQueryWrapper<>();
        sessionQw.eq(TrainingSession::getEmpNo, empNo);
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
