package com.hzc.secKill.DAO;

import com.hzc.secKill.Domain.MiaoshaUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface MiaoshaUserDAO {

    @Select("select * from miaosha_user where id = #{id}")
    public MiaoshaUser getById(@Param("id")long id);

    @Update("update miaosha_user set password = #{password} where id = #{id}")
    public void update(MiaoshaUser toBeUpdate);
}
