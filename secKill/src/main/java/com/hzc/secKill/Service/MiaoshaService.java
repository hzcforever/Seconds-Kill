package com.hzc.secKill.Service;

import com.hzc.secKill.DAO.GoodsDAO;
import com.hzc.secKill.Domain.Goods;
import com.hzc.secKill.Domain.GoodsVo;
import com.hzc.secKill.Domain.MiaoshaUser;
import com.hzc.secKill.Domain.OrderInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MiaoshaService {

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Transactional
    public OrderInfo miaosha(MiaoshaUser user, GoodsVo goods) {
        // 减库存->下订单->写入秒杀订单
        goodsService.reduceStock(goods);
        return orderService.createOrder(user, goods);
    }
}
