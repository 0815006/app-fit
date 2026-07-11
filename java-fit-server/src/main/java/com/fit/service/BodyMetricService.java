package com.fit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.BodyMetric;

import java.time.LocalDate;
import java.util.List;

public interface BodyMetricService {
    Page<BodyMetric> queryPage(int page, int size, String userId, LocalDate startDate, LocalDate endDate);
    List<BodyMetric> listByUserId(String userId, LocalDate startDate, LocalDate endDate);
    BodyMetric getById(String id);
    BodyMetric save(BodyMetric metric);
    BodyMetric update(BodyMetric metric);
    void delete(String id);
}
