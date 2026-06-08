package com.fit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.GymActionMuscleRel;

import java.util.List;

public interface GymActionMuscleRelService {
    Page<GymActionMuscleRel> queryPage(int page, int size, String actionId, String muscleId);
    List<GymActionMuscleRel> listByActionId(String actionId);
    GymActionMuscleRel save(GymActionMuscleRel rel);
    void delete(String id);
    void deleteByActionId(String actionId);
}
