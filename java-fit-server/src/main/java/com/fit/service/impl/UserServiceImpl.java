package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fit.entity.User;
import com.fit.mapper.UserMapper;
import com.fit.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 * 启动时自动检测并创建管理员账号
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Override
    public User getByUsername(String username) {
        return this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
    }

    @Override
    public User getByOpenid(String openid) {
        return this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getWxOpenid, openid));
    }

    /**
     * 启动时检查并创建初始管理员账号
     * 用户名: admin  密码: admin123  工号: 2036377
     */
    @PostConstruct
    public void initAdminUser() {
        User existing = getByUsername("admin");
        if (existing != null) {
            log.info("管理员账号已存在, username=admin");
            return;
        }

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(ENCODER.encode("admin123"));
        admin.setEmpNo("2036377");
        admin.setNickname("Admin");
        admin.setStatus(1);

        this.save(admin);
        log.info("初始管理员账号已创建: username=admin, empNo=2036377");
    }
}
