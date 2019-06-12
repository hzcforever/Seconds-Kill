package com.hzc.secKill.DAO;

import com.hzc.secKill.Domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface UserDAO {

    @Select("select * from user where id = #{id}")
    public User getById(@Param("id") int id);
}
