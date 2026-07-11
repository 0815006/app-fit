package com.fit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fit.entity.LoginRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LoginRecordMapper extends BaseMapper<LoginRecord> {

    @Select("SELECT COUNT(*) FROM login_record WHERE emp_no = #{empNo}")
    long countByEmpNo(@Param("empNo") String empNo);

    @Select("SELECT COUNT(*) FROM login_record WHERE login_type = #{loginType}")
    long countByLoginType(@Param("loginType") String loginType);

    @Select("SELECT COUNT(*) FROM login_record WHERE user_id = #{userId} AND login_type = #{loginType}")
    long countByUserIdAndLoginType(@Param("userId") String userId, @Param("loginType") String loginType);

    @Select("SELECT COUNT(*) FROM login_record")
    long countAll();
}
