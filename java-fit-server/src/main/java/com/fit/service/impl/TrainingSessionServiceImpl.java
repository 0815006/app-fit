package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.TrainingSession;
import com.fit.mapper.TrainingSessionMapper;
import com.fit.service.TrainingSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingSessionServiceImpl implements TrainingSessionService {

    private final TrainingSessionMapper mapper;

    @Override
    public Page<TrainingSession> queryPage(int page, int size, String userId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<TrainingSession> qw = new LambdaQueryWrapper<>();
        if (userId != null && !userId.isBlank()) {
            qw.eq(TrainingSession::getUserId, userId);
        }
        if (startDate != null) {
            qw.ge(TrainingSession::getSessionDate, startDate);
        }
        if (endDate != null) {
            qw.le(TrainingSession::getSessionDate, endDate);
        }
        qw.orderByDesc(TrainingSession::getSessionDate)
          .orderByDesc(TrainingSession::getCreateTime);
        return mapper.selectPage(new Page<>(page, size), qw);
    }

    @Override
    public TrainingSession getById(String id) {
        return mapper.selectById(id);
    }

    @Override
    public TrainingSession save(TrainingSession session) {
        mapper.insert(session);
        log.info("Created training session: id={}, userId={}, date={}", session.getId(), session.getUserId(), session.getSessionDate());
        return session;
    }

    @Override
    public TrainingSession update(TrainingSession session) {
        mapper.updateById(session);
        log.info("Updated training session: id={}", session.getId());
        return session;
    }

    @Override
    public void delete(String id) {
        mapper.deleteById(id);
        log.info("Deleted training session: id={}", id);
    }
}
