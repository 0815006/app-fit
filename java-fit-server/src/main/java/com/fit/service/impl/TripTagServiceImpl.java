package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fit.entity.TripTag;
import com.fit.mapper.TripTagMapper;
import com.fit.service.TripTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripTagServiceImpl implements TripTagService {

    private final TripTagMapper mapper;

    @Override
    public List<TripTag> listAll() {
        LambdaQueryWrapper<TripTag> qw = new LambdaQueryWrapper<>();
        qw.orderByAsc(TripTag::getType)
          .orderByAsc(TripTag::getName);
        return mapper.selectList(qw);
    }

    @Override
    public List<TripTag> listByType(String type) {
        LambdaQueryWrapper<TripTag> qw = new LambdaQueryWrapper<>();
        qw.eq(TripTag::getType, type);
        qw.orderByAsc(TripTag::getName);
        return mapper.selectList(qw);
    }

    @Override
    public TripTag getById(String id) {
        return mapper.selectById(id);
    }

    @Override
    public TripTag save(TripTag tag) {
        mapper.insert(tag);
        log.info("Created trip tag: id={}, name={}", tag.getId(), tag.getName());
        return tag;
    }

    @Override
    public TripTag update(TripTag tag) {
        mapper.updateById(tag);
        log.info("Updated trip tag: id={}, name={}", tag.getId(), tag.getName());
        return tag;
    }

    @Override
    public void delete(String id) {
        mapper.deleteById(id);
        log.info("Deleted trip tag: id={}", id);
    }
}
