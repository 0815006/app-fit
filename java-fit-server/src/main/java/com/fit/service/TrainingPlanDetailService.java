package com.fit.service;

import com.fit.entity.TrainingPlanDetail;

import java.util.List;

public interface TrainingPlanDetailService {
    List<TrainingPlanDetail> listByPlanId(String planId);
    TrainingPlanDetail save(TrainingPlanDetail detail);
    TrainingPlanDetail update(TrainingPlanDetail detail);
    void delete(String id);
    void deleteByPlanId(String planId);
}
