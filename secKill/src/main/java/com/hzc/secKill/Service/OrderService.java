package com.hzc.secKill.Service;

import com.hzc.secKill.DAO.OrderDAO;
import com.hzc.secKill.Domain.GoodsVo;
import com.hzc.secKill.Domain.MiaoshaOrder;
import com.hzc.secKill.Domain.MiaoshaUser;
import com.hzc.secKill.Domain.OrderInfo;
import com.hzc.secKill.Redis.OrderKey;
import com.hzc.secKill.Redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class OrderService {

    @Autowired
    OrderDAO orderDAO;

    @Autowired
    RedisService redisService;

    public MiaoshaOrder getMiaoshaOrderByUserIdGoodsId(Long userId, long goodsId) {
//        return orderDAO.getMiaoshaOrderByUserIdGoodsId(userId, goodsId);
        return redisService.get(OrderKey.getMiaoshaOrderByUidGid, "" + userId + "_" + goodsId,
                MiaoshaOrder.class);
    }

    @Transactional
    public OrderInfo createOrder(MiaoshaUser user, GoodsVo goods) {
        // 生成订单
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCreateDate(new Date());
        orderInfo.setDeliveryAddrId(0l);
        orderInfo.setGoodsCount(1);
        orderInfo.setGoodsId(goods.getId());
        orderInfo.setGoodsName(goods.getGoodsName());
        orderInfo.setGoodsPrice(goods.getMiaoshaPrice());
        orderInfo.setOrderChannel(1);
        orderInfo.setStatus(0);
        orderInfo.setUserId(user.getId());
        orderDAO.insert(orderInfo);
        // 生成秒杀订单
        MiaoshaOrder miaoshaOrder = new MiaoshaOrder(); // 通过建立唯一索引保证不会卖超，否则回滚
        miaoshaOrder.setGoodsId(goods.getId());
        miaoshaOrder.setOrderId(orderInfo.getId());
        miaoshaOrder.setUserId(user.getId());
        orderDAO.insertMiaoshaOrder(miaoshaOrder);

        redisService.set(OrderKey.getMiaoshaOrderByUidGid, "" + user.getId() + "_" +
                goods.getId(), miaoshaOrder);

        return orderInfo;
    }

    public OrderInfo getOrderById(long orderId) {
        return orderDAO.getOrderById(orderId);
    }

    public void deleteOrders() {
        orderDAO.deleteOrders();
        orderDAO.deleteMiaoshaOrders();
    }
}
