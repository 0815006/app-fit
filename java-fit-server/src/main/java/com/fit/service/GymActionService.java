package com.fit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.GymAction;

import java.util.List;

public interface GymActionService {
    Page<GymAction> queryPage(int page, int size, String name, String actionType, String movementPattern, Integer difficultyLevel);
    List<GymAction> listAll();
    GymAction getById(String id);
    GymAction save(GymAction action);
    GymAction update(GymAction action);
    void delete(String id);

    /**
     * 按肌群大类编码查询该肌群下所有动作
     * @param muscleGroup 肌群大类编码（CHEST/BACK/SHOULDER/ARM/LEG/GLUTE/CORE/FULL_BODY）
     * @return 动作列表
     */
    List<GymAction> listByMuscleGroup(String muscleGroup);
}
