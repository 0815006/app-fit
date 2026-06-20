package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.TripPlan;
import com.fit.mapper.TripPlanMapper;
import com.fit.service.TripPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripPlanServiceImpl implements TripPlanService {

    private final TripPlanMapper mapper;

    @Override
    public Page<TripPlan> queryPage(int page, int size, String userId, String status) {
        LambdaQueryWrapper<TripPlan> qw = new LambdaQueryWrapper<>();
        if (userId != null && !userId.isBlank()) {
            qw.eq(TripPlan::getUserId, userId);
        }
        if (status != null && !status.isBlank()) {
            qw.eq(TripPlan::getStatus, status);
        }
        qw.orderByDesc(TripPlan::getCreateTime);
        return mapper.selectPage(new Page<>(page, size), qw);
    }

    @Override
    public List<TripPlan> listByUserId(String userId) {
        LambdaQueryWrapper<TripPlan> qw = new LambdaQueryWrapper<>();
        qw.eq(TripPlan::getUserId, userId);
        qw.orderByDesc(TripPlan::getCreateTime);
        return mapper.selectList(qw);
    }

    @Override
    public TripPlan getById(String id) {
        return mapper.selectById(id);
    }

    @Override
    public TripPlan save(TripPlan plan) {
        mapper.insert(plan);
        log.info("Created trip plan: id={}, title={}", plan.getId(), plan.getTitle());
        return plan;
    }

    @Override
    public TripPlan update(TripPlan plan) {
        mapper.updateById(plan);
        log.info("Updated trip plan: id={}, title={}", plan.getId(), plan.getTitle());
        return plan;
    }

    @Override
    public void delete(String id) {
        mapper.deleteById(id);
        log.info("Deleted trip plan: id={}", id);
    }

    @Override
    public void updateStatus(String id, String status) {
        LambdaUpdateWrapper<TripPlan> uw = new LambdaUpdateWrapper<>();
        uw.eq(TripPlan::getId, id)
          .set(TripPlan::getStatus, status);
        mapper.update(null, uw);
        log.info("Updated trip plan status: id={}, status={}", id, status);
    }
}
