package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.GymMuscle;
import com.fit.mapper.GymMuscleMapper;
import com.fit.service.GymMuscleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GymMuscleServiceImpl implements GymMuscleService {

    private final GymMuscleMapper mapper;

    @Override
    public Page<GymMuscle> queryPage(int page, int size, String muscleName, String muscleGroup) {
        LambdaQueryWrapper<GymMuscle> qw = new LambdaQueryWrapper<>();
        if (muscleName != null && !muscleName.isBlank()) {
            qw.like(GymMuscle::getMuscleName, muscleName);
        }
        if (muscleGroup != null && !muscleGroup.isBlank()) {
            qw.eq(GymMuscle::getMuscleGroup, muscleGroup);
        }
        qw.orderByAsc(GymMuscle::getSortNo);
        return mapper.selectPage(new Page<>(page, size), qw);
    }

    @Override
    public List<GymMuscle> listAll() {
        return mapper.selectList(new LambdaQueryWrapper<GymMuscle>().orderByAsc(GymMuscle::getSortNo));
    }

    @Override
    public GymMuscle getById(String id) {
        return mapper.selectById(id);
    }

    @Override
    public GymMuscle save(GymMuscle muscle) {
        mapper.insert(muscle);
        log.info("Created gym muscle: id={}, name={}", muscle.getId(), muscle.getMuscleName());
        return muscle;
    }

    @Override
    public GymMuscle update(GymMuscle muscle) {
        mapper.updateById(muscle);
        log.info("Updated gym muscle: id={}, name={}", muscle.getId(), muscle.getMuscleName());
        return muscle;
    }

    @Override
    public void delete(String id) {
        mapper.deleteById(id);
        log.info("Deleted gym muscle: id={}", id);
    }
}
