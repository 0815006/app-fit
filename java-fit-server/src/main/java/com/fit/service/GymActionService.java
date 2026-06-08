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
}
