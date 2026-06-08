package com.fit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.GymEquipment;

import java.util.List;

public interface GymEquipmentService {

    /**
     * Query all equipment records (paginated), with optional filters.
     */
    Page<GymEquipment> queryPage(int page, int size, String equipmentName, String equipmentType);

    /**
     * Get all equipment (no pagination, for dropdowns etc.).
     */
    List<GymEquipment> listAll();

    /**
     * Get a single equipment by id.
     */
    GymEquipment getById(String id);

    /**
     * Create / update equipment.
     */
    GymEquipment save(GymEquipment equipment);

    /**
     * Update equipment.
     */
    GymEquipment update(GymEquipment equipment);

    /**
     * Delete equipment by id.
     */
    void delete(String id);
}
