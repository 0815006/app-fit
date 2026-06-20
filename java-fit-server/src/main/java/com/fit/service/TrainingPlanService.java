package com.fit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.TrainingPlan;

import java.util.List;

public interface TrainingPlanService {
    Page<TrainingPlan> queryPage(int page, int size, String empNo, String planName, String muscleGroup);
    List<TrainingPlan> listByEmpNo(String empNo);
    TrainingPlan getById(String id);
    TrainingPlan save(TrainingPlan plan);
    TrainingPlan update(TrainingPlan plan);
    void delete(String id);
}
