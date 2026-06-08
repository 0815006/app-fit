package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.GymEquipment;
import com.fit.mapper.GymEquipmentMapper;
import com.fit.service.GymEquipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GymEquipmentServiceImpl implements GymEquipmentService {

    private final GymEquipmentMapper mapper;

    @Override
    public Page<GymEquipment> queryPage(int page, int size, String equipmentName, String equipmentType) {
        LambdaQueryWrapper<GymEquipment> qw = new LambdaQueryWrapper<>();
        if (equipmentName != null && !equipmentName.isBlank()) {
            qw.like(GymEquipment::getEquipmentName, equipmentName);
        }
        if (equipmentType != null && !equipmentType.isBlank()) {
            qw.eq(GymEquipment::getEquipmentType, equipmentType);
        }
        qw.orderByAsc(GymEquipment::getEquipmentType)
          .orderByAsc(GymEquipment::getEquipmentName);
        return mapper.selectPage(new Page<>(page, size), qw);
    }

    @Override
    public List<GymEquipment> listAll() {
        LambdaQueryWrapper<GymEquipment> qw = new LambdaQueryWrapper<>();
        qw.orderByAsc(GymEquipment::getEquipmentType)
          .orderByAsc(GymEquipment::getEquipmentName);
        return mapper.selectList(qw);
    }

    @Override
    public GymEquipment getById(String id) {
        return mapper.selectById(id);
    }

    @Override
    public GymEquipment save(GymEquipment equipment) {
        mapper.insert(equipment);
        log.info("Created gym equipment: id={}, name={}", equipment.getId(), equipment.getEquipmentName());
        return equipment;
    }

    @Override
    public GymEquipment update(GymEquipment equipment) {
        mapper.updateById(equipment);
        log.info("Updated gym equipment: id={}, name={}", equipment.getId(), equipment.getEquipmentName());
        return equipment;
    }

    @Override
    public void delete(String id) {
        mapper.deleteById(id);
        log.info("Deleted gym equipment: id={}", id);
    }
}
