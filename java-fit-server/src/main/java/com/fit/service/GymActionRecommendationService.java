package com.fit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.GymActionRecommendation;

import java.util.List;

public interface GymActionRecommendationService {
    Page<GymActionRecommendation> queryPage(int page, int size, String actionId, String trainingGoal);
    List<GymActionRecommendation> listByActionId(String actionId);
    GymActionRecommendation getById(String id);
    GymActionRecommendation save(GymActionRecommendation rec);
    GymActionRecommendation update(GymActionRecommendation rec);
    void delete(String id);
}
