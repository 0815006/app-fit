package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.BodyMetric;
import com.fit.mapper.BodyMetricMapper;
import com.fit.service.BodyMetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BodyMetricServiceImpl implements BodyMetricService {

    private final BodyMetricMapper mapper;

    @Override
    public Page<BodyMetric> queryPage(int page, int size, String empNo, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<BodyMetric> qw = new LambdaQueryWrapper<>();
        if (empNo != null && !empNo.isBlank()) {
            qw.eq(BodyMetric::getEmpNo, empNo);
        }
        if (startDate != null) {
            qw.ge(BodyMetric::getMetricDate, startDate);
        }
        if (endDate != null) {
            qw.le(BodyMetric::getMetricDate, endDate);
        }
        qw.orderByDesc(BodyMetric::getMetricDate);
        return mapper.selectPage(new Page<>(page, size), qw);
    }

    @Override
    public List<BodyMetric> listByEmpNo(String empNo, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<BodyMetric> qw = new LambdaQueryWrapper<>();
        if (empNo != null && !empNo.isBlank()) {
            qw.eq(BodyMetric::getEmpNo, empNo);
        }
        if (startDate != null) {
            qw.ge(BodyMetric::getMetricDate, startDate);
        }
        if (endDate != null) {
            qw.le(BodyMetric::getMetricDate, endDate);
        }
        qw.orderByAsc(BodyMetric::getMetricDate);
        return mapper.selectList(qw);
    }

    @Override
    public BodyMetric getById(String id) {
        return mapper.selectById(id);
    }

    @Override
    public BodyMetric save(BodyMetric metric) {
        mapper.insert(metric);
        log.info("Created body metric: id={}, empNo={}, date={}", metric.getId(), metric.getEmpNo(), metric.getMetricDate());
        return metric;
    }

    @Override
    public BodyMetric update(BodyMetric metric) {
        mapper.updateById(metric);
        log.info("Updated body metric: id={}", metric.getId());
        return metric;
    }

    @Override
    public void delete(String id) {
        mapper.deleteById(id);
        log.info("Deleted body metric: id={}", id);
    }
}
