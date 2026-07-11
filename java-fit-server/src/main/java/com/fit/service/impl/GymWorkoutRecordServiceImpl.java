package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fit.entity.*;
import com.fit.mapper.*;
import com.fit.service.GymWorkoutRecordService;
import com.fit.vo.DashboardVO;
import com.fit.vo.MuscleGroupStatusVO;
import com.fit.vo.TimeoutRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

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
    public void endWorkout(String recordId, BigDecimal exhaustionScore) {
        GymWorkoutRecord record = getRecordOrThrow(recordId);
        if (record.getStatus() != 0) {
            throw new IllegalStateException("该训练记录已结束，不可重复操作");
        }
        record.setEndTime(LocalDateTime.now());
        record.setExhaustionScore(exhaustionScore);
        record.setStatus(1);
        recordMapper.updateById(record);
        log.info("结束训练：recordId={}, exhaustionScore={}", recordId, exhaustionScore);
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

            groupStatuses.add(MuscleGroupStatusVO.builder()
                    .muscleGroup(groupCode)
                    .muscleGroupName(groupName)
                    .weeklyCount(weeklyCount)
                    .status(status)
                    .remainingSeconds(remainingSeconds)
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

    @Override
    public void makeupWorkout(String userId, String actionId, LocalDateTime startTime, BigDecimal exhaustionScore) {
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
        record.setExhaustionScore(exhaustionScore);
        record.setStatus(1);

        recordMapper.insert(record);
        log.info("自由补打卡：recordId={}, userId={}, actionId={}, startTime={}", record.getId(), userId, actionId, startTime);
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
}
