package com.hzc.secKill.Controller;

import com.hzc.secKill.Domain.GoodsVo;
import com.hzc.secKill.Domain.MiaoshaUser;
import com.hzc.secKill.Domain.OrderDetailVo;
import com.hzc.secKill.Domain.OrderInfo;
import com.hzc.secKill.Redis.RedisService;
import com.hzc.secKill.Result.CodeMsg;
import com.hzc.secKill.Result.ResultUtil;
import com.hzc.secKill.Service.GoodsService;
import com.hzc.secKill.Service.MiaoshaUserService;
import com.hzc.secKill.Service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    MiaoshaUserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    OrderService orderService;

    @Autowired
    GoodsService goodsService;

    @RequestMapping("/detail")
    @ResponseBody
    public ResultUtil<OrderDetailVo> info(Model model, MiaoshaUser user,
                                          @RequestParam("orderId") long orderId) {
        if (null == user) {
            return ResultUtil.error(CodeMsg.SESSION_ERROR);
        }
        OrderInfo order = orderService.getOrderById(orderId);
        if (null == order) {
            return ResultUtil.error(CodeMsg.ORDER_NOT_EXIST);
        }
        long goodsId = order.getGoodsId();
        GoodsVo goods = goodsService.getGoodVoByGoodsId(goodsId);
        OrderDetailVo orderDetailVo = new OrderDetailVo();
        orderDetailVo.setGoods(goods);
        orderDetailVo.setOrder(order);
        return ResultUtil.success(orderDetailVo);
    }

}
