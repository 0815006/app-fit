package com.fit.service.impl;

import com.fit.entity.LoginRecord;
import com.fit.mapper.LoginRecordMapper;
import com.fit.service.LoginRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginRecordServiceImpl implements LoginRecordService {

    private final LoginRecordMapper loginRecordMapper;

    @Override
    public LoginRecord record(String userId, String empNo, String loginType) {
        LoginRecord record = new LoginRecord();
        record.setUserId(userId);
        record.setEmpNo(empNo);
        record.setLoginType(loginType);
        loginRecordMapper.insert(record);
        log.info("Login recorded: userId={}, empNo={}, type={}, id={}", userId, empNo, loginType, record.getId());
        return record;
    }

    @Override
    public long count(String empNo) {
        return loginRecordMapper.countByEmpNo(empNo);
    }

    @Override
    public long countByLoginType(String loginType) {
        return loginRecordMapper.countByLoginType(loginType);
    }

    @Override
    public long countByUserIdAndLoginType(String userId, String loginType) {
        return loginRecordMapper.countByUserIdAndLoginType(userId, loginType);
    }

    @Override
    public long countAll() {
        return loginRecordMapper.countAll();
    }
}
