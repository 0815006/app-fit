package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.TripItem;
import com.fit.mapper.TripItemMapper;
import com.fit.service.TripItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripItemServiceImpl implements TripItemService {

    private final TripItemMapper mapper;

    @Override
    public Page<TripItem> queryPage(int page, int size, String name, String category) {
        LambdaQueryWrapper<TripItem> qw = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank()) {
            qw.like(TripItem::getName, name);
        }
        if (category != null && !category.isBlank()) {
            qw.eq(TripItem::getCategory, category);
        }
        qw.orderByAsc(TripItem::getCategory)
          .orderByAsc(TripItem::getName);
        return mapper.selectPage(new Page<>(page, size), qw);
    }

    @Override
    public List<TripItem> listAll() {
        LambdaQueryWrapper<TripItem> qw = new LambdaQueryWrapper<>();
        qw.orderByAsc(TripItem::getCategory)
          .orderByAsc(TripItem::getName);
        return mapper.selectList(qw);
    }

    @Override
    public TripItem getById(String id) {
        return mapper.selectById(id);
    }

    @Override
    public TripItem save(TripItem item) {
        mapper.insert(item);
        log.info("Created trip item: id={}, name={}", item.getId(), item.getName());
        return item;
    }

    @Override
    public TripItem update(TripItem item) {
        mapper.updateById(item);
        log.info("Updated trip item: id={}, name={}", item.getId(), item.getName());
        return item;
    }

    @Override
    public void delete(String id) {
        mapper.deleteById(id);
        log.info("Deleted trip item: id={}", id);
    }
}
