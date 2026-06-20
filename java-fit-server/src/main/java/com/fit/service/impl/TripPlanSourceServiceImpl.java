package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fit.entity.TripPlanSource;
import com.fit.mapper.TripPlanSourceMapper;
import com.fit.service.TripPlanSourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripPlanSourceServiceImpl implements TripPlanSourceService {

    private final TripPlanSourceMapper mapper;

    @Override
    public List<TripPlanSource> listByPlanDetailId(String planDetailId) {
        LambdaQueryWrapper<TripPlanSource> qw = new LambdaQueryWrapper<>();
        qw.eq(TripPlanSource::getPlanDetailId, planDetailId);
        return mapper.selectList(qw);
    }

    @Override
    public TripPlanSource save(TripPlanSource source) {
        mapper.insert(source);
        log.info("Created trip plan source: id={}, planDetailId={}, tagId={}", source.getId(), source.getPlanDetailId(), source.getTagId());
        return source;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatch(List<TripPlanSource> sources) {
        for (TripPlanSource source : sources) {
            mapper.insert(source);
        }
        log.info("Batch created {} trip plan sources", sources.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByPlanDetailId(String planDetailId) {
        LambdaQueryWrapper<TripPlanSource> qw = new LambdaQueryWrapper<>();
        qw.eq(TripPlanSource::getPlanDetailId, planDetailId);
        mapper.delete(qw);
        log.info("Deleted all trip plan sources for planDetailId={}", planDetailId);
    }
}
