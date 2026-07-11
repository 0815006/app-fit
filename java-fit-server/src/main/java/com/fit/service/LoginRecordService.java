package com.fit.service;

import com.fit.entity.LoginRecord;

import java.time.LocalDateTime;

public interface LoginRecordService {

    /**
     * Record a login event with userId and return the created record.
     *
     * @param userId    user表主键ID
     * @param empNo     employee number
     * @param loginType WEB or MINI_PROGRAM
     * @return the saved login record
     */
    LoginRecord record(String userId, String empNo, String loginType);

    /**
     * Get total login count for a given employee (by empNo).
     *
     * @param empNo employee number
     * @return login count
     */
    long count(String empNo);

    /**
     * Get total login count for a given login type (e.g. MINI_PROGRAM).
     *
     * @param loginType login type
     * @return total count for this login type
     */
    long countByLoginType(String loginType);

    /**
     * Get login count for a given user ID and login type.
     *
     * @param userId    user表主键ID
     * @param loginType WEB or MINI_PROGRAM
     * @return login count for this user + type
     */
    long countByUserIdAndLoginType(String userId, String loginType);

    /**
     * Get total count of all login records.
     *
     * @return total count
     */
    long countAll();

    /**
     * Get total login count for a given user ID (all types).
     *
     * @param userId user表主键ID
     * @return total login count for this user
     */
    long countByUserId(String userId);

    /**
     * Get the latest login time for a given user ID.
     *
     * @param userId user表主键ID
     * @return latest login time, or null if never logged in
     */
    LocalDateTime getLatestLoginTimeByUserId(String userId);
}
