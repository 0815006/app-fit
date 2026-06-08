package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.GymAction;
import com.fit.entity.GymActionRecommendation;
import com.fit.mapper.GymActionMapper;
import com.fit.mapper.GymActionRecommendationMapper;
import com.fit.service.GymActionRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GymActionRecommendationServiceImpl implements GymActionRecommendationService {

    private final GymActionRecommendationMapper mapper;
    private final GymActionMapper actionMapper;

    @Override
    public Page<GymActionRecommendation> queryPage(int page, int size, String actionId, String trainingGoal) {
        LambdaQueryWrapper<GymActionRecommendation> qw = new LambdaQueryWrapper<>();
        if (actionId != null && !actionId.isBlank()) {
            qw.eq(GymActionRecommendation::getActionId, actionId);
        }
        if (trainingGoal != null && !trainingGoal.isBlank()) {
            qw.eq(GymActionRecommendation::getTrainingGoal, trainingGoal);
        }
        qw.orderByAsc(GymActionRecommendation::getActionId)
          .orderByAsc(GymActionRecommendation::getTrainingGoal);
        Page<GymActionRecommendation> result = mapper.selectPage(new Page<>(page, size), qw);
        fillDisplayNames(result.getRecords());
        return result;
    }

    @Override
    public List<GymActionRecommendation> listByActionId(String actionId) {
        LambdaQueryWrapper<GymActionRecommendation> qw = new LambdaQueryWrapper<>();
        qw.eq(GymActionRecommendation::getActionId, actionId);
        List<GymActionRecommendation> list = mapper.selectList(qw);
        fillDisplayNames(list);
        return list;
    }

    @Override
    public GymActionRecommendation getById(String id) {
        GymActionRecommendation rec = mapper.selectById(id);
        if (rec != null) {
            GymAction action = actionMapper.selectById(rec.getActionId());
            rec.setActionName(action != null ? action.getName() : rec.getActionId());
        }
        return rec;
    }

    @Override
    public GymActionRecommendation save(GymActionRecommendation rec) {
        mapper.insert(rec);
        log.info("Created action recommendation: id={}, action={}, goal={}", rec.getId(), rec.getActionId(), rec.getTrainingGoal());
        return rec;
    }

    @Override
    public GymActionRecommendation update(GymActionRecommendation rec) {
        mapper.updateById(rec);
        log.info("Updated action recommendation: id={}", rec.getId());
        return rec;
    }

    @Override
    public void delete(String id) {
        mapper.deleteById(id);
        log.info("Deleted action recommendation: id={}", id);
    }

    private void fillDisplayNames(List<GymActionRecommendation> list) {
        if (list.isEmpty()) return;
        Map<String, String> actionNames = actionMapper.selectList(null).stream()
                .collect(Collectors.toMap(GymAction::getId, GymAction::getName, (a, b) -> a));
        for (GymActionRecommendation rec : list) {
            rec.setActionName(actionNames.getOrDefault(rec.getActionId(), rec.getActionId()));
        }
    }
}
