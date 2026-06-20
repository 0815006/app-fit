package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fit.entity.TripTagItem;
import com.fit.mapper.TripTagItemMapper;
import com.fit.service.TripTagItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripTagItemServiceImpl implements TripTagItemService {

    private final TripTagItemMapper mapper;

    @Override
    public List<TripTagItem> listByTagId(String tagId) {
        LambdaQueryWrapper<TripTagItem> qw = new LambdaQueryWrapper<>();
        qw.eq(TripTagItem::getTagId, tagId);
        return mapper.selectList(qw);
    }

    @Override
    public List<TripTagItem> listByTagIds(List<String> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<TripTagItem> qw = new LambdaQueryWrapper<>();
        qw.in(TripTagItem::getTagId, tagIds);
        return mapper.selectList(qw);
    }

    @Override
    public TripTagItem save(TripTagItem tagItem) {
        mapper.insert(tagItem);
        log.info("Created trip tag item: id={}, tagId={}, itemId={}", tagItem.getId(), tagItem.getTagId(), tagItem.getItemId());
        return tagItem;
    }

    @Override
    public void delete(String id) {
        mapper.deleteById(id);
        log.info("Deleted trip tag item: id={}", id);
    }
}
