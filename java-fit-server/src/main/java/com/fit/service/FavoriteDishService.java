package com.fit.service;

import com.fit.entity.UserFavoriteDish;

import java.util.List;
import java.util.Map;

public interface FavoriteDishService {

    /**
     * 收藏/取消收藏（Toggle）
     * @param empNo   用户工号
     * @param dishName 菜品名称
     * @return Map { "favorited": true/false, "dishName": "菜品名" }
     */
    Map<String, Object> toggle(String empNo, String dishName);

    /**
     * 批量查询收藏状态
     * @param empNo     用户工号
     * @param dishNames 菜品名称列表
     * @return 已收藏的菜品名称列表
     */
    List<String> check(String empNo, List<String> dishNames);

    /**
     * 获取收藏列表
     * @param empNo 用户工号
     * @return 收藏列表
     */
    List<UserFavoriteDish> list(String empNo);

    /**
     * 删除单个收藏
     * @param empNo    用户工号
     * @param dishName 菜品名称
     */
    void delete(String empNo, String dishName);
}
