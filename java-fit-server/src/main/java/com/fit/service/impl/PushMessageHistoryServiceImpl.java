package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.PushMessageHistory;
import com.fit.mapper.PushMessageHistoryMapper;
import com.fit.service.PushMessageHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushMessageHistoryServiceImpl implements PushMessageHistoryService {

    private final PushMessageHistoryMapper mapper;

    @Override
    public void save(PushMessageHistory history) {
        mapper.insert(history);
    }

    @Override
    public IPage<PushMessageHistory> page(int page, int size) {
        LambdaQueryWrapper<PushMessageHistory> qw = new LambdaQueryWrapper<>();
        qw.orderByDesc(PushMessageHistory::getSendTime);
        return mapper.selectPage(new Page<>(page, size), qw);
    }
}
