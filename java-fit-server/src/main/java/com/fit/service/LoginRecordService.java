package com.fit.service;

import com.fit.entity.LoginRecord;

public interface LoginRecordService {

    /**
     * Record a login event and return the created record.
     *
     * @param empNo     employee number
     * @param loginType WEB or MINI_PROGRAM
     * @return the saved login record
     */
    LoginRecord record(String empNo, String loginType);

    /**
     * Get total login count for a given employee.
     *
     * @param empNo employee number
     * @return login count
     */
    long count(String empNo);
}
