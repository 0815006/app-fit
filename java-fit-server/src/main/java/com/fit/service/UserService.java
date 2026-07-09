package com.fit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fit.entity.User;

public interface UserService extends IService<User> {

    /**
     * 按用户名查找用户（Web 登录用）
     */
    User getByUsername(String username);

    /**
     * 按微信 openid 查找用户（小程序登录用）
     */
    User getByOpenid(String openid);
}
