package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fit.entity.TrainingPlanDetail;
import com.fit.mapper.TrainingPlanDetailMapper;
import com.fit.service.TrainingPlanDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingPlanDetailServiceImpl implements TrainingPlanDetailService {

    private final TrainingPlanDetailMapper mapper;

    @Override
    public List<TrainingPlanDetail> listByPlanId(String planId) {
        return mapper.selectList(new LambdaQueryWrapper<TrainingPlanDetail>()
                .eq(TrainingPlanDetail::getPlanId, planId)
                .orderByAsc(TrainingPlanDetail::getSortNo));
    }

    @Override
    public TrainingPlanDetail save(TrainingPlanDetail detail) {
        mapper.insert(detail);
        log.info("Created training plan detail: id={}, planId={}", detail.getId(), detail.getPlanId());
        return detail;
    }

    @Override
    public TrainingPlanDetail update(TrainingPlanDetail detail) {
        mapper.updateById(detail);
        log.info("Updated training plan detail: id={}", detail.getId());
        return detail;
    }

    @Override
    public void delete(String id) {
        mapper.deleteById(id);
        log.info("Deleted training plan detail: id={}", id);
    }

    @Override
    public void deleteByPlanId(String planId) {
        mapper.delete(new LambdaQueryWrapper<TrainingPlanDetail>()
                .eq(TrainingPlanDetail::getPlanId, planId));
        log.info("Deleted all training plan details for planId={}", planId);
    }
}
