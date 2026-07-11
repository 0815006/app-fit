package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.GymAction;
import com.fit.entity.GymActionMuscleRel;
import com.fit.entity.GymMuscle;
import com.fit.mapper.GymActionMapper;
import com.fit.mapper.GymActionMuscleRelMapper;
import com.fit.mapper.GymMuscleMapper;
import com.fit.service.GymActionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GymActionServiceImpl implements GymActionService {

    private final GymActionMapper mapper;
    private final GymMuscleMapper muscleMapper;
    private final GymActionMuscleRelMapper actionMuscleRelMapper;

    @Override
    public Page<GymAction> queryPage(int page, int size, String name, String actionType, String movementPattern, Integer difficultyLevel) {
        LambdaQueryWrapper<GymAction> qw = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank()) {
            qw.like(GymAction::getName, name);
        }
        if (actionType != null && !actionType.isBlank()) {
            qw.eq(GymAction::getActionType, actionType);
        }
        if (movementPattern != null && !movementPattern.isBlank()) {
            qw.eq(GymAction::getMovementPattern, movementPattern);
        }
        if (difficultyLevel != null) {
            qw.eq(GymAction::getDifficultyLevel, difficultyLevel);
        }
        qw.orderByAsc(GymAction::getActionType)
          .orderByAsc(GymAction::getName);
        return mapper.selectPage(new Page<>(page, size), qw);
    }

    @Override
    public List<GymAction> listAll() {
        return mapper.selectList(new LambdaQueryWrapper<GymAction>()
                .eq(GymAction::getStatus, 1)
                .orderByAsc(GymAction::getName));
    }

    @Override
    public GymAction getById(String id) {
        return mapper.selectById(id);
    }

    @Override
    public GymAction save(GymAction action) {
        mapper.insert(action);
        log.info("Created gym action: id={}, name={}", action.getId(), action.getName());
        return action;
    }

    @Override
    public GymAction update(GymAction action) {
        mapper.updateById(action);
        log.info("Updated gym action: id={}, name={}", action.getId(), action.getName());
        return action;
    }

    @Override
    public void delete(String id) {
        mapper.deleteById(id);
        log.info("Deleted gym action: id={}", id);
    }

    @Override
    public List<GymAction> listByMuscleGroup(String muscleGroup) {
        // 1. 查出该肌群大类下所有具体肌肉的 muscleId
        List<GymMuscle> muscles = muscleMapper.selectList(
                new LambdaQueryWrapper<GymMuscle>()
                        .select(GymMuscle::getId)
                        .eq(GymMuscle::getMuscleGroup, muscleGroup));
        List<String> muscleIds = muscles.stream().map(GymMuscle::getId).toList();
        if (muscleIds.isEmpty()) {
            return List.of();
        }

        // 2. 查出关联的动作 ID（去重）
        List<GymActionMuscleRel> rels = actionMuscleRelMapper.selectList(
                new LambdaQueryWrapper<GymActionMuscleRel>()
                        .select(GymActionMuscleRel::getActionId)
                        .in(GymActionMuscleRel::getMuscleId, muscleIds));
        List<String> actionIds = rels.stream()
                .map(GymActionMuscleRel::getActionId)
                .distinct()
                .toList();
        if (actionIds.isEmpty()) {
            return List.of();
        }

        // 3. 查出动作实体
        return mapper.selectList(
                new LambdaQueryWrapper<GymAction>()
                        .in(GymAction::getId, actionIds)
                        .eq(GymAction::getStatus, 1)
                        .orderByAsc(GymAction::getName));
    }
}
