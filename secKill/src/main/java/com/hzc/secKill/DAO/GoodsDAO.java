package com.hzc.secKill.DAO;

import com.hzc.secKill.Domain.GoodsVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface GoodsDAO {

    @Select("select g.*, mg.stock_count, mg.start_date, mg.end_date, mg.miaosha_price" +
            " from miaosha_goods mg left join goods g on mg.goods_id = g.id")
    public List<GoodsVo> listGoodsVo();

    @Select("select g.*, mg.stock_count, mg.start_date, mg.end_date, mg.miaosha_price" +
            " from miaosha_goods mg left join goods g on mg.goods_id = g.id " +
            " where g.id = #{goodsId}")
    GoodsVo getGoodVoByGoodsId(@Param("goodsId") long goodsId);
}
