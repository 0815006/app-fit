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
}
