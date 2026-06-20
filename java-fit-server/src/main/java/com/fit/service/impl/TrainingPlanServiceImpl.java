package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.TrainingPlan;
import com.fit.mapper.TrainingPlanMapper;
import com.fit.service.TrainingPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingPlanServiceImpl implements TrainingPlanService {

    private final TrainingPlanMapper mapper;

    @Override
    public Page<TrainingPlan> queryPage(int page, int size, String empNo, String planName, String muscleGroup) {
        LambdaQueryWrapper<TrainingPlan> qw = new LambdaQueryWrapper<>();
        if (empNo != null && !empNo.isBlank()) {
            qw.eq(TrainingPlan::getEmpNo, empNo);
        }
        if (planName != null && !planName.isBlank()) {
            qw.like(TrainingPlan::getPlanName, planName);
        }
        if (muscleGroup != null && !muscleGroup.isBlank()) {
            qw.eq(TrainingPlan::getMuscleGroup, muscleGroup);
        }
        qw.orderByAsc(TrainingPlan::getSortNo)
          .orderByDesc(TrainingPlan::getCreateTime);
        return mapper.selectPage(new Page<>(page, size), qw);
    }

    @Override
    public List<TrainingPlan> listByEmpNo(String empNo) {
        return mapper.selectList(new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getEmpNo, empNo)
                .eq(TrainingPlan::getStatus, 1)
                .orderByAsc(TrainingPlan::getSortNo)
                .orderByDesc(TrainingPlan::getCreateTime));
    }

    @Override
    public TrainingPlan getById(String id) {
        return mapper.selectById(id);
    }

    @Override
    public TrainingPlan save(TrainingPlan plan) {
        mapper.insert(plan);
        log.info("Created training plan: id={}, name={}", plan.getId(), plan.getPlanName());
        return plan;
    }

    @Override
    public TrainingPlan update(TrainingPlan plan) {
        mapper.updateById(plan);
        log.info("Updated training plan: id={}, name={}", plan.getId(), plan.getPlanName());
        return plan;
    }

    @Override
    public void delete(String id) {
        mapper.deleteById(id);
        log.info("Deleted training plan: id={}", id);
    }
}
