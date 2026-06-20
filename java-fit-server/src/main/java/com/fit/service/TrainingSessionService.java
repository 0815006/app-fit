package com.fit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.TrainingSession;

import java.time.LocalDate;

public interface TrainingSessionService {
    Page<TrainingSession> queryPage(int page, int size, String empNo, LocalDate startDate, LocalDate endDate);
    TrainingSession getById(String id);
    TrainingSession save(TrainingSession session);
    TrainingSession update(TrainingSession session);
    void delete(String id);
}
