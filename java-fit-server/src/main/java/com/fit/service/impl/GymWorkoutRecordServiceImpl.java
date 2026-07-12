package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fit.entity.*;
import com.fit.mapper.*;
import com.fit.service.GymWorkoutRecordService;
import com.fit.vo.DashboardVO;
import com.fit.vo.MuscleGroupStatusVO;
import com.fit.vo.TimeoutRecordVO;
import com.fit.vo.WeeklyWorkoutVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GymWorkoutRecordServiceImpl implements GymWorkoutRecordService {

    private final GymWorkoutRecordMapper recordMapper;
    private final GymActionMuscleRelMapper actionMuscleRelMapper;
    private final GymMuscleMapper muscleMapper;
    private final GymActionMapper actionMapper;

    /** muscleGroup 编码 → 中文名 映射 */
    private static final Map<String, String> GROUP_NAME_MAP = new LinkedHashMap<>();
    static {
        GROUP_NAME_MAP.put("CHEST", "胸部");
        GROUP_NAME_MAP.put("BACK", "背部");
        GROUP_NAME_MAP.put("SHOULDER", "肩部");
        GROUP_NAME_MAP.put("ARM", "手臂");
        GROUP_NAME_MAP.put("LEG", "腿部");
        GROUP_NAME_MAP.put("GLUTE", "臀部");
        GROUP_NAME_MAP.put("CORE", "核心");
        GROUP_NAME_MAP.put("CARDIO", "有氧");
        GROUP_NAME_MAP.put("FULL_BODY", "全身");
    }

    @Override
    public String startWorkout(String userId, String actionId) {
        // 1. 通过 gym_action_muscle_rel（取 is_primary=1）找到 muscle_id
        LambdaQueryWrapper<GymActionMuscleRel> relQw = new LambdaQueryWrapper<>();
        relQw.eq(GymActionMuscleRel::getActionId, actionId)
             .eq(GymActionMuscleRel::getIsPrimary, 1)
             .last("LIMIT 1");
        GymActionMuscleRel rel = actionMuscleRelMapper.selectOne(relQw);

        String muscleGroup;
        if (rel != null) {
            // 2. 通过 gym_muscle 查到 muscle_group
            GymMuscle muscle = muscleMapper.selectById(rel.getMuscleId());
            if (muscle == null) {
                throw new IllegalArgumentException("肌群不存在：muscleId=" + rel.getMuscleId());
            }
            muscleGroup = muscle.getMuscleGroup();
        } else {
            // 降级：取任意一条关联
            relQw = new LambdaQueryWrapper<>();
            relQw.eq(GymActionMuscleRel::getActionId, actionId).last("LIMIT 1");
            rel = actionMuscleRelMapper.selectOne(relQw);
            if (rel == null) {
                throw new IllegalArgumentException("动作未关联任何肌群：actionId=" + actionId);
            }
            GymMuscle muscle = muscleMapper.selectById(rel.getMuscleId());
            muscleGroup = muscle.getMuscleGroup();
        }

        // 3. 创建记录
        GymWorkoutRecord record = new GymWorkoutRecord();
        record.setUserId(userId);
        record.setActionId(actionId);
        record.setMuscleGroup(muscleGroup);
        record.setStartTime(LocalDateTime.now());
        record.setStatus(0);

        recordMapper.insert(record);
        log.info("开始训练：recordId={}, userId={}, actionId={}, muscleGroup={}", record.getId(), userId, actionId, muscleGroup);
        return record.getId();
    }

    @Override
    public void endWorkout(String recordId, BigDecimal weight, Integer reps, Integer setCount, BigDecimal exhaustionScore) {
        GymWorkoutRecord record = getRecordOrThrow(recordId);
        if (record.getStatus() != 0) {
            throw new IllegalStateException("该训练记录已结束，不可重复操作");
        }

        // 1. 写入训练数据（允许全部为 null）
        record.setWeight(weight);
        record.setReps(reps);
        record.setSetCount(setCount);

        // 2. 仅当 weight 和 reps 均非 null 时才计算 1RM 和 PR
        if (weight != null && reps != null && weight.compareTo(BigDecimal.ZERO) > 0 && reps > 0) {
            // Epley 公式：1RM = W × (1 + R/30)
            BigDecimal rm = weight.multiply(BigDecimal.ONE.add(
                BigDecimal.valueOf(reps).divide(BigDecimal.valueOf(30), 4, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);
            record.setRmEstimate(rm);

            // 查该用户该动作历史最佳 1RM，判断是否 PR
            BigDecimal bestRm = findBestRmEstimate(record.getUserId(), record.getActionId(), recordId);
            record.setIsPr(bestRm != null && rm.compareTo(bestRm) > 0 ? 1 : 0);
        }
        // 如果 weight/reps 任一为 null，rmEstimate 保持 null，isPr 保持 0

        // 3. 写入 endTime + exhaustionScore + status=1
        record.setEndTime(LocalDateTime.now());
        record.setExhaustionScore(exhaustionScore);
        record.setStatus(1);
        recordMapper.updateById(record);
        log.info("结束训练：recordId={}, weight={}, reps={}, setCount={}, exhaustionScore={}, rmEstimate={}, isPr={}",
                recordId, weight, reps, setCount, exhaustionScore, record.getRmEstimate(), record.getIsPr());
    }

    /**
     * 查找该用户该动作的历史最佳 rmEstimate（排除当前记录自身）
     */
    private BigDecimal findBestRmEstimate(String userId, String actionId, String excludeRecordId) {
        LambdaQueryWrapper<GymWorkoutRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(GymWorkoutRecord::getUserId, userId)
          .eq(GymWorkoutRecord::getActionId, actionId)
          .ne(GymWorkoutRecord::getId, excludeRecordId)
          .isNotNull(GymWorkoutRecord::getRmEstimate)
          .orderByDesc(GymWorkoutRecord::getRmEstimate)
          .last("LIMIT 1");
        GymWorkoutRecord best = recordMapper.selectOne(qw);
        return best != null ? best.getRmEstimate() : null;
    }

    @Override
    public void correctTimeout(String recordId, int actualMinutes, BigDecimal exhaustionScore) {
        GymWorkoutRecord record = getRecordOrThrow(recordId);
        if (record.getStatus() != 0) {
            throw new IllegalStateException("该训练记录已结束，不可重复操作");
        }
        record.setEndTime(record.getStartTime().plusMinutes(actualMinutes));
        record.setExhaustionScore(exhaustionScore);
        record.setStatus(2);
        recordMapper.updateById(record);
        log.info("超时修正：recordId={}, actualMinutes={}, exhaustionScore={}", recordId, actualMinutes, exhaustionScore);
    }

    private GymWorkoutRecord getRecordOrThrow(String recordId) {
        GymWorkoutRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("训练记录不存在：" + recordId);
        }
        return record;
    }

    @Override
    public DashboardVO getDashboard(String userId) {
        // 1. 查所有肌群，按 muscle_group 分组
        List<GymMuscle> allMuscles = muscleMapper.selectList(
                new LambdaQueryWrapper<GymMuscle>().orderByAsc(GymMuscle::getSortNo));

        // 按 muscle_group 去重，保留每个 group 的第一条（取 baseRecoveryHours）
        Map<String, GymMuscle> groupMap = new LinkedHashMap<>();
        for (GymMuscle m : allMuscles) {
            groupMap.putIfAbsent(m.getMuscleGroup(), m);
        }

        // 本周一 00:00
        LocalDateTime weekStart = LocalDateTime.now()
                .with(DayOfWeek.MONDAY)
                .with(LocalTime.MIN);

        List<MuscleGroupStatusVO> groupStatuses = new ArrayList<>();
        List<String> readyNames = new ArrayList<>();

        for (Map.Entry<String, GymMuscle> entry : groupMap.entrySet()) {
            String groupCode = entry.getKey();
            GymMuscle muscle = entry.getValue();
            String groupName = GROUP_NAME_MAP.getOrDefault(groupCode, groupCode);

            // 2a. 本周打卡次数：DISTINCT action_id 去重
            LambdaQueryWrapper<GymWorkoutRecord> countQw = new LambdaQueryWrapper<>();
            countQw.select(GymWorkoutRecord::getActionId)
                   .eq(GymWorkoutRecord::getUserId, userId)
                   .eq(GymWorkoutRecord::getMuscleGroup, groupCode)
                   .ge(GymWorkoutRecord::getStartTime, weekStart)
                   .groupBy(GymWorkoutRecord::getActionId);
            List<GymWorkoutRecord> countList = recordMapper.selectList(countQw);
            int weeklyCount = countList.size();

            // 2b. 最新一条已结束的训练记录
            LambdaQueryWrapper<GymWorkoutRecord> latestQw = new LambdaQueryWrapper<>();
            latestQw.eq(GymWorkoutRecord::getUserId, userId)
                    .eq(GymWorkoutRecord::getMuscleGroup, groupCode)
                    .in(GymWorkoutRecord::getStatus, 1, 2)
                    .orderByDesc(GymWorkoutRecord::getEndTime)
                    .last("LIMIT 1");
            GymWorkoutRecord latest = recordMapper.selectOne(latestQw);

            String status;
            long remainingSeconds = 0;

            if (latest == null || latest.getEndTime() == null) {
                // 无训练记录 → 已恢复
                status = "READY";
            } else {
                // 3. 计算恢复状态
                int baseHours = muscle.getBaseRecoveryHours() != null ? muscle.getBaseRecoveryHours() : 48;
                BigDecimal score = latest.getExhaustionScore() != null ? latest.getExhaustionScore() : BigDecimal.ONE;
                // T_real = base_recovery_hours × exhaustion_score（小时）
                BigDecimal realHours = BigDecimal.valueOf(baseHours).multiply(score);
                long realSeconds = realHours.multiply(BigDecimal.valueOf(3600)).longValue();
                LocalDateTime targetTime = latest.getEndTime().plusSeconds(realSeconds);

                if (LocalDateTime.now().isBefore(targetTime)) {
                    status = "RECOVERING";
                    remainingSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), targetTime);
                } else {
                    status = "READY";
                }
            }

            if ("READY".equals(status)) {
                readyNames.add(groupName);
            }

            // 4. 统计二级肌肉本周训练痕迹
            List<MuscleGroupStatusVO.SubMuscleStatus> subMuscles = buildSubMuscleStatuses(
                    userId, groupCode, weekStart);

            groupStatuses.add(MuscleGroupStatusVO.builder()
                    .muscleGroup(groupCode)
                    .muscleGroupName(groupName)
                    .weeklyCount(weeklyCount)
                    .status(status)
                    .remainingSeconds(remainingSeconds)
                    .subMuscles(subMuscles)
                    .build());
        }

        // 超时检查
        TimeoutRecordVO timeoutRecord = checkTimeoutInternal(userId);

        return DashboardVO.builder()
                .readyMuscleNames(readyNames)
                .muscleGroups(groupStatuses)
                .timeoutRecord(timeoutRecord)
                .build();
    }

    /**
     * 构建某个肌群大类下所有二级肌肉的本周训练痕迹
     */
    private List<MuscleGroupStatusVO.SubMuscleStatus> buildSubMuscleStatuses(
            String userId, String muscleGroup, LocalDateTime weekStart) {
        // 查该肌群大类下所有二级肌肉
        List<GymMuscle> subMuscles = muscleMapper.selectList(
                new LambdaQueryWrapper<GymMuscle>()
                        .eq(GymMuscle::getMuscleGroup, muscleGroup)
                        .orderByAsc(GymMuscle::getSortNo));

        if (subMuscles.isEmpty()) {
            return List.of();
        }

        // 收集所有肌肉ID
        List<String> muscleIds = subMuscles.stream()
                .map(GymMuscle::getId)
                .toList();

        // 批量查这些肌肉关联的所有 actionId
        List<GymActionMuscleRel> allRels = actionMuscleRelMapper.selectList(
                new LambdaQueryWrapper<GymActionMuscleRel>()
                        .in(GymActionMuscleRel::getMuscleId, muscleIds)
                        .select(GymActionMuscleRel::getActionId, GymActionMuscleRel::getMuscleId));

        // muscleId → 关联的 actionId 集合
        Map<String, Set<String>> muscleActionMap = new HashMap<>();
        for (GymActionMuscleRel rel : allRels) {
            muscleActionMap.computeIfAbsent(rel.getMuscleId(), k -> new HashSet<>())
                    .add(rel.getActionId());
        }

        // 批量查本周有训练记录的 actionId（该肌群下全部）
        Set<String> trainedActionIds = recordMapper.selectList(
                new LambdaQueryWrapper<GymWorkoutRecord>()
                        .select(GymWorkoutRecord::getActionId)
                        .eq(GymWorkoutRecord::getUserId, userId)
                        .eq(GymWorkoutRecord::getMuscleGroup, muscleGroup)
                        .ge(GymWorkoutRecord::getStartTime, weekStart))
                .stream()
                .map(GymWorkoutRecord::getActionId)
                .collect(Collectors.toSet());

        // 组装结果
        List<MuscleGroupStatusVO.SubMuscleStatus> result = new ArrayList<>();
        for (GymMuscle sub : subMuscles) {
            Set<String> actionIds = muscleActionMap.getOrDefault(sub.getId(), Set.of());
            boolean trained = actionIds.stream().anyMatch(trainedActionIds::contains);
            result.add(MuscleGroupStatusVO.SubMuscleStatus.builder()
                    .muscleCode(sub.getMuscleCode())
                    .muscleName(sub.getMuscleName())
                    .trainedThisWeek(trained)
                    .build());
        }
        return result;
    }

    @Override
    public void makeupWorkout(String userId, String actionId, LocalDateTime startTime,
                              BigDecimal weight, Integer reps, Integer setCount, BigDecimal exhaustionScore) {
        // 推导 muscleGroup
        LambdaQueryWrapper<GymActionMuscleRel> relQw = new LambdaQueryWrapper<>();
        relQw.eq(GymActionMuscleRel::getActionId, actionId)
             .eq(GymActionMuscleRel::getIsPrimary, 1)
             .last("LIMIT 1");
        GymActionMuscleRel rel = actionMuscleRelMapper.selectOne(relQw);

        String muscleGroup;
        if (rel != null) {
            GymMuscle muscle = muscleMapper.selectById(rel.getMuscleId());
            muscleGroup = muscle.getMuscleGroup();
        } else {
            relQw = new LambdaQueryWrapper<>();
            relQw.eq(GymActionMuscleRel::getActionId, actionId).last("LIMIT 1");
            rel = actionMuscleRelMapper.selectOne(relQw);
            if (rel == null) {
                throw new IllegalArgumentException("动作未关联任何肌群：actionId=" + actionId);
            }
            GymMuscle muscle = muscleMapper.selectById(rel.getMuscleId());
            muscleGroup = muscle.getMuscleGroup();
        }

        GymWorkoutRecord record = new GymWorkoutRecord();
        record.setUserId(userId);
        record.setActionId(actionId);
        record.setMuscleGroup(muscleGroup);
        record.setStartTime(startTime);
        record.setEndTime(startTime); // 瞬间打卡
        record.setWeight(weight);
        record.setReps(reps);
        record.setSetCount(setCount);
        record.setExhaustionScore(exhaustionScore);
        record.setStatus(1);

        recordMapper.insert(record);

        // 计算 1RM 和 PR（与 endWorkout 逻辑一致）
        if (weight != null && reps != null && weight.compareTo(BigDecimal.ZERO) > 0 && reps > 0) {
            // Epley 公式：1RM = W × (1 + R/30)
            BigDecimal rm = weight.multiply(BigDecimal.ONE.add(
                BigDecimal.valueOf(reps).divide(BigDecimal.valueOf(30), 4, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);
            record.setRmEstimate(rm);

            // 查该用户该动作历史最佳 1RM，判断是否 PR
            BigDecimal bestRm = findBestRmEstimate(userId, actionId, record.getId());
            record.setIsPr(bestRm != null && rm.compareTo(bestRm) > 0 ? 1 : 0);

            recordMapper.updateById(record);
        }

        log.info("自由补打卡：recordId={}, userId={}, actionId={}, startTime={}, weight={}, reps={}, setCount={}, rmEstimate={}, isPr={}",
                record.getId(), userId, actionId, startTime, weight, reps, setCount, record.getRmEstimate(), record.getIsPr());
    }

    @Override
    public TimeoutRecordVO checkTimeout(String userId) {
        return checkTimeoutInternal(userId);
    }

    private TimeoutRecordVO checkTimeoutInternal(String userId) {
        // status=0 且 start_time < now - 2小时
        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
        LambdaQueryWrapper<GymWorkoutRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(GymWorkoutRecord::getUserId, userId)
          .eq(GymWorkoutRecord::getStatus, 0)
          .le(GymWorkoutRecord::getStartTime, twoHoursAgo)
          .orderByAsc(GymWorkoutRecord::getStartTime)
          .last("LIMIT 1");
        GymWorkoutRecord record = recordMapper.selectOne(qw);

        if (record == null) {
            return null;
        }

        // 获取动作名称
        GymAction action = actionMapper.selectById(record.getActionId());
        String actionName = action != null ? action.getName() : "未知动作";

        // 生成人性化时间标签
        String startTimeLabel = formatTimeLabel(record.getStartTime());

        return TimeoutRecordVO.builder()
                .recordId(record.getId())
                .actionName(actionName)
                .startTime(record.getStartTime())
                .startTimeLabel(startTimeLabel)
                .build();
    }

    /**
     * 生成人性化时间标签，如"今天 14:30"、"昨天 18:30"、"3天前 10:00"
     */
    private String formatTimeLabel(LocalDateTime time) {
        if (time == null) return "";
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        String timeStr = time.format(tf);
        LocalDate today = LocalDate.now();
        LocalDate date = time.toLocalDate();

        if (date.equals(today)) {
            return "今天 " + timeStr;
        } else if (date.equals(today.minusDays(1))) {
            return "昨天 " + timeStr;
        } else {
            long days = ChronoUnit.DAYS.between(date, today);
            return days + "天前 " + timeStr;
        }
    }

    @Override
    public List<WeeklyWorkoutVO> getWeeklySummary(String userId) {
        LocalDateTime weekStart = LocalDateTime.now()
                .with(DayOfWeek.MONDAY)
                .with(LocalTime.MIN);
        LocalDateTime weekEnd = weekStart.plusDays(7);

        LambdaQueryWrapper<GymWorkoutRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(GymWorkoutRecord::getUserId, userId)
          .in(GymWorkoutRecord::getStatus, 1, 2)
          .ge(GymWorkoutRecord::getStartTime, weekStart)
          .lt(GymWorkoutRecord::getStartTime, weekEnd)
          .orderByAsc(GymWorkoutRecord::getStartTime);

        List<GymWorkoutRecord> records = recordMapper.selectList(qw);

        return records.stream().map(r -> {
            GymAction action = actionMapper.selectById(r.getActionId());
            String actionName = action != null ? action.getName() : "未知动作";
            String groupName = GROUP_NAME_MAP.getOrDefault(r.getMuscleGroup(), r.getMuscleGroup());

            return WeeklyWorkoutVO.builder()
                    .actionName(actionName)
                    .muscleGroup(r.getMuscleGroup())
                    .muscleGroupName(groupName)
                    .startTime(r.getStartTime())
                    .weight(r.getWeight())
                    .reps(r.getReps())
                    .setCount(r.getSetCount())
                    .rmEstimate(r.getRmEstimate())
                    .isPr(r.getIsPr() != null && r.getIsPr() == 1)
                    .exhaustionScore(r.getExhaustionScore())
                    .dayOfWeek(r.getStartTime().getDayOfWeek().getValue())
                    .build();
        }).toList();
    }

    @Override
    public List<String> getCheckinDates(String userId) {
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);

        LambdaQueryWrapper<GymWorkoutRecord> qw = new LambdaQueryWrapper<>();
        qw.select(GymWorkoutRecord::getStartTime)
          .eq(GymWorkoutRecord::getUserId, userId)
          .in(GymWorkoutRecord::getStatus, 1, 2)
          .ge(GymWorkoutRecord::getStartTime, sixMonthsAgo)
          .orderByAsc(GymWorkoutRecord::getStartTime);

        return recordMapper.selectList(qw).stream()
                .map(r -> r.getStartTime().toLocalDate().toString())
                .distinct()
                .toList();
    }
}
