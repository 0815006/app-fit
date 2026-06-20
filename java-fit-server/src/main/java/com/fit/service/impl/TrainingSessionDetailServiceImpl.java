package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fit.entity.TrainingSessionDetail;
import com.fit.mapper.TrainingSessionDetailMapper;
import com.fit.service.TrainingSessionDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingSessionDetailServiceImpl implements TrainingSessionDetailService {

    private final TrainingSessionDetailMapper mapper;

    @Override
    public List<TrainingSessionDetail> listBySessionId(String sessionId) {
        return mapper.selectList(new LambdaQueryWrapper<TrainingSessionDetail>()
                .eq(TrainingSessionDetail::getSessionId, sessionId)
                .orderByAsc(TrainingSessionDetail::getSortNo)
                .orderByAsc(TrainingSessionDetail::getSetNo));
    }

    @Override
    public TrainingSessionDetail save(TrainingSessionDetail detail) {
        mapper.insert(detail);
        log.info("Created training session detail: id={}, sessionId={}", detail.getId(), detail.getSessionId());
        return detail;
    }

    @Override
    public TrainingSessionDetail update(TrainingSessionDetail detail) {
        mapper.updateById(detail);
        log.info("Updated training session detail: id={}", detail.getId());
        return detail;
    }

    @Override
    public void delete(String id) {
        mapper.deleteById(id);
        log.info("Deleted training session detail: id={}", id);
    }

    @Override
    public void deleteBySessionId(String sessionId) {
        mapper.delete(new LambdaQueryWrapper<TrainingSessionDetail>()
                .eq(TrainingSessionDetail::getSessionId, sessionId));
        log.info("Deleted all training session details for sessionId={}", sessionId);
    }
}
