package com.fit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.GymActionEquipmentRel;

import java.util.List;

public interface GymActionEquipmentRelService {
    Page<GymActionEquipmentRel> queryPage(int page, int size, String actionId, String equipmentId);
    List<GymActionEquipmentRel> listByActionId(String actionId);
    GymActionEquipmentRel save(GymActionEquipmentRel rel);
    void delete(String id);
    void deleteByActionId(String actionId);
}
