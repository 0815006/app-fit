package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.GymAction;
import com.fit.entity.GymActionMuscleRel;
import com.fit.entity.GymMuscle;
import com.fit.mapper.GymActionMapper;
import com.fit.mapper.GymActionMuscleRelMapper;
import com.fit.mapper.GymMuscleMapper;
import com.fit.service.GymActionMuscleRelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GymActionMuscleRelServiceImpl implements GymActionMuscleRelService {

    private final GymActionMuscleRelMapper mapper;
    private final GymActionMapper actionMapper;
    private final GymMuscleMapper muscleMapper;

    @Override
    public Page<GymActionMuscleRel> queryPage(int page, int size, String actionId, String muscleId) {
        LambdaQueryWrapper<GymActionMuscleRel> qw = new LambdaQueryWrapper<>();
        if (actionId != null && !actionId.isBlank()) {
            qw.eq(GymActionMuscleRel::getActionId, actionId);
        }
        if (muscleId != null && !muscleId.isBlank()) {
            qw.eq(GymActionMuscleRel::getMuscleId, muscleId);
        }
        qw.orderByAsc(GymActionMuscleRel::getActionId)
          .orderByDesc(GymActionMuscleRel::getIsPrimary);
        Page<GymActionMuscleRel> result = mapper.selectPage(new Page<>(page, size), qw);
        enrichNames(result.getRecords());
        return result;
    }

    @Override
    public List<GymActionMuscleRel> listByActionId(String actionId) {
        LambdaQueryWrapper<GymActionMuscleRel> qw = new LambdaQueryWrapper<>();
        qw.eq(GymActionMuscleRel::getActionId, actionId)
          .orderByDesc(GymActionMuscleRel::getIsPrimary);
        List<GymActionMuscleRel> list = mapper.selectList(qw);
        enrichNames(list);
        return list;
    }

    @Override
    public List<GymActionMuscleRel> listByMuscleGroup(String muscleGroup) {
        // 1. 查出该肌群大类下所有 muscleId
        List<GymMuscle> muscles = muscleMapper.selectList(
                new LambdaQueryWrapper<GymMuscle>()
                        .eq(GymMuscle::getMuscleGroup, muscleGroup)
                        .select(GymMuscle::getId));
        if (muscles.isEmpty()) {
            return List.of();
        }
        List<String> muscleIds = muscles.stream().map(GymMuscle::getId).toList();

        // 2. 用 muscleId IN (...) 查出所有 rel
        LambdaQueryWrapper<GymActionMuscleRel> qw = new LambdaQueryWrapper<>();
        qw.in(GymActionMuscleRel::getMuscleId, muscleIds)
          .orderByDesc(GymActionMuscleRel::getIsPrimary);
        List<GymActionMuscleRel> list = mapper.selectList(qw);
        enrichNames(list);
        return list;
    }

    @Override
    public GymActionMuscleRel save(GymActionMuscleRel rel) {
        mapper.insert(rel);
        log.info("Created action-muscle rel: id={}, action={}, muscle={}", rel.getId(), rel.getActionId(), rel.getMuscleId());
        return rel;
    }

    @Override
    public void delete(String id) {
        mapper.deleteById(id);
        log.info("Deleted action-muscle rel: id={}", id);
    }

    @Override
    public void deleteByActionId(String actionId) {
        LambdaQueryWrapper<GymActionMuscleRel> qw = new LambdaQueryWrapper<>();
        qw.eq(GymActionMuscleRel::getActionId, actionId);
        mapper.delete(qw);
    }

    private void enrichNames(List<GymActionMuscleRel> rels) {
        if (rels.isEmpty()) return;

        Map<String, String> actionNameMap = actionMapper.selectList(
                new LambdaQueryWrapper<GymAction>().select(GymAction::getId, GymAction::getName)
        ).stream().collect(Collectors.toMap(GymAction::getId, GymAction::getName));

        Map<String, String> muscleNameMap = muscleMapper.selectList(
                new LambdaQueryWrapper<GymMuscle>().select(GymMuscle::getId, GymMuscle::getMuscleName)
        ).stream().collect(Collectors.toMap(GymMuscle::getId, GymMuscle::getMuscleName));

        for (GymActionMuscleRel rel : rels) {
            rel.setActionName(actionNameMap.getOrDefault(rel.getActionId(), rel.getActionId()));
            rel.setMuscleName(muscleNameMap.getOrDefault(rel.getMuscleId(), rel.getMuscleId()));
        }
    }
}
