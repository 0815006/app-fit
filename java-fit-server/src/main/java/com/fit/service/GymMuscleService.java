package com.fit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.GymMuscle;

import java.util.List;

public interface GymMuscleService {
    Page<GymMuscle> queryPage(int page, int size, String muscleName, String muscleGroup);
    List<GymMuscle> listAll();
    GymMuscle getById(String id);
    GymMuscle save(GymMuscle muscle);
    GymMuscle update(GymMuscle muscle);
    void delete(String id);
}
