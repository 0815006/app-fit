package com.fit.service;

import com.fit.entity.TrainingSessionDetail;

import java.util.List;

public interface TrainingSessionDetailService {
    List<TrainingSessionDetail> listBySessionId(String sessionId);
    TrainingSessionDetail save(TrainingSessionDetail detail);
    TrainingSessionDetail update(TrainingSessionDetail detail);
    void delete(String id);
    void deleteBySessionId(String sessionId);
}
