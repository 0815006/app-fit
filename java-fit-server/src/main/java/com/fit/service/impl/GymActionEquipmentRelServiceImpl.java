package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.GymAction;
import com.fit.entity.GymActionEquipmentRel;
import com.fit.entity.GymEquipment;
import com.fit.mapper.GymActionEquipmentRelMapper;
import com.fit.mapper.GymActionMapper;
import com.fit.mapper.GymEquipmentMapper;
import com.fit.service.GymActionEquipmentRelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GymActionEquipmentRelServiceImpl implements GymActionEquipmentRelService {

    private final GymActionEquipmentRelMapper mapper;
    private final GymActionMapper actionMapper;
    private final GymEquipmentMapper equipmentMapper;

    @Override
    public Page<GymActionEquipmentRel> queryPage(int page, int size, String actionId, String equipmentId) {
        LambdaQueryWrapper<GymActionEquipmentRel> qw = new LambdaQueryWrapper<>();
        if (actionId != null && !actionId.isBlank()) {
            qw.eq(GymActionEquipmentRel::getActionId, actionId);
        }
        if (equipmentId != null && !equipmentId.isBlank()) {
            qw.eq(GymActionEquipmentRel::getEquipmentId, equipmentId);
        }
        qw.orderByAsc(GymActionEquipmentRel::getActionId);
        Page<GymActionEquipmentRel> result = mapper.selectPage(new Page<>(page, size), qw);
        fillDisplayNames(result.getRecords());
        return result;
    }

    @Override
    public List<GymActionEquipmentRel> listByActionId(String actionId) {
        LambdaQueryWrapper<GymActionEquipmentRel> qw = new LambdaQueryWrapper<>();
        qw.eq(GymActionEquipmentRel::getActionId, actionId);
        List<GymActionEquipmentRel> list = mapper.selectList(qw);
        fillDisplayNames(list);
        return list;
    }

    @Override
    public GymActionEquipmentRel save(GymActionEquipmentRel rel) {
        mapper.insert(rel);
        log.info("Created action-equipment rel: id={}, action={}, equipment={}", rel.getId(), rel.getActionId(), rel.getEquipmentId());
        return rel;
    }

    @Override
    public void delete(String id) {
        mapper.deleteById(id);
        log.info("Deleted action-equipment rel: id={}", id);
    }

    @Override
    public void deleteByActionId(String actionId) {
        LambdaQueryWrapper<GymActionEquipmentRel> qw = new LambdaQueryWrapper<>();
        qw.eq(GymActionEquipmentRel::getActionId, actionId);
        mapper.delete(qw);
        log.info("Deleted all action-equipment rels for action: {}", actionId);
    }

    private void fillDisplayNames(List<GymActionEquipmentRel> list) {
        if (list.isEmpty()) return;
        Map<String, String> actionNames = actionMapper.selectList(null).stream()
                .collect(Collectors.toMap(GymAction::getId, GymAction::getName, (a, b) -> a));
        Map<String, String> equipmentNames = equipmentMapper.selectList(null).stream()
                .collect(Collectors.toMap(GymEquipment::getId, GymEquipment::getEquipmentName, (a, b) -> a));
        for (GymActionEquipmentRel rel : list) {
            rel.setActionName(actionNames.getOrDefault(rel.getActionId(), rel.getActionId()));
            rel.setEquipmentName(equipmentNames.getOrDefault(rel.getEquipmentId(), rel.getEquipmentId()));
        }
    }
}
