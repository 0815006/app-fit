package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fit.entity.TripPlanDetail;
import com.fit.mapper.TripPlanDetailMapper;
import com.fit.service.TripPlanDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripPlanDetailServiceImpl implements TripPlanDetailService {

    private final TripPlanDetailMapper mapper;

    @Override
    public List<TripPlanDetail> listByPlanId(String planId) {
        LambdaQueryWrapper<TripPlanDetail> qw = new LambdaQueryWrapper<>();
        qw.eq(TripPlanDetail::getPlanId, planId);
        qw.orderByAsc(TripPlanDetail::getContainer)
          .orderByDesc(TripPlanDetail::getImportanceLevel)
          .orderByAsc(TripPlanDetail::getItemName);
        return mapper.selectList(qw);
    }

    @Override
    public TripPlanDetail getById(String id) {
        return mapper.selectById(id);
    }

    @Override
    public TripPlanDetail save(TripPlanDetail detail) {
        mapper.insert(detail);
        log.info("Created trip plan detail: id={}, planId={}, itemName={}", detail.getId(), detail.getPlanId(), detail.getItemName());
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatch(List<TripPlanDetail> details) {
        for (TripPlanDetail detail : details) {
            mapper.insert(detail);
        }
        log.info("Batch created {} trip plan details", details.size());
    }

    @Override
    public TripPlanDetail update(TripPlanDetail detail) {
        mapper.updateById(detail);
        log.info("Updated trip plan detail: id={}", detail.getId());
        return detail;
    }

    @Override
    public void updateChecked(String id, Integer isChecked) {
        LambdaUpdateWrapper<TripPlanDetail> uw = new LambdaUpdateWrapper<>();
        uw.eq(TripPlanDetail::getId, id)
          .set(TripPlanDetail::getIsChecked, isChecked);
        mapper.update(null, uw);
        log.info("Updated trip plan detail checked: id={}, isChecked={}", id, isChecked);
    }

    @Override
    public void updateExcludeFlag(String id, Integer excludeFlag) {
        LambdaUpdateWrapper<TripPlanDetail> uw = new LambdaUpdateWrapper<>();
        uw.eq(TripPlanDetail::getId, id)
          .set(TripPlanDetail::getExcludeFlag, excludeFlag);
        mapper.update(null, uw);
        log.info("Updated trip plan detail exclude flag: id={}, excludeFlag={}", id, excludeFlag);
    }

    @Override
    public void updateQuantity(String id, Integer targetQuantity) {
        LambdaUpdateWrapper<TripPlanDetail> uw = new LambdaUpdateWrapper<>();
        uw.eq(TripPlanDetail::getId, id)
          .set(TripPlanDetail::getTargetQuantity, targetQuantity);
        mapper.update(null, uw);
        log.info("Updated trip plan detail quantity: id={}, targetQuantity={}", id, targetQuantity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByPlanId(String planId) {
        LambdaQueryWrapper<TripPlanDetail> qw = new LambdaQueryWrapper<>();
        qw.eq(TripPlanDetail::getPlanId, planId);
        mapper.delete(qw);
        log.info("Deleted all trip plan details for planId={}", planId);
    }
}
