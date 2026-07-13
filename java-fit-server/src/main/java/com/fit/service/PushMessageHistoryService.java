package com.fit.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fit.entity.PushMessageHistory;

public interface PushMessageHistoryService {

    void save(PushMessageHistory history);

    IPage<PushMessageHistory> page(int page, int size);
}
