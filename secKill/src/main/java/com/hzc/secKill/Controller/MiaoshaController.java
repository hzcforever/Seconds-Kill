package com.hzc.secKill.Controller;

import com.hzc.secKill.Domain.GoodsVo;
import com.hzc.secKill.Domain.MiaoshaOrder;
import com.hzc.secKill.Domain.MiaoshaUser;
import com.hzc.secKill.Domain.OrderInfo;
import com.hzc.secKill.Redis.RedisService;
import com.hzc.secKill.Result.CodeMsg;
import com.hzc.secKill.Service.GoodsService;
import com.hzc.secKill.Service.MiaoshaService;
import com.hzc.secKill.Service.MiaoshaUserService;
import com.hzc.secKill.Service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/miaosha")
public class MiaoshaController {

    @Autowired
    MiaoshaUserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    MiaoshaService miaoshaService;

    @RequestMapping("/do_miaosha")
    public String miaosha(Model model, MiaoshaUser user,
                          @RequestParam("goodsId") long goodsId) {
        model.addAttribute("user", user);
        if (null == user) {
            return "login";
        }
        // 判断库存
        GoodsVo goods = goodsService.getGoodVoByGoodsId(goodsId);
        int stock = goods.getStockCount();
        if (stock <= 0) {
            model.addAttribute("errmsg", CodeMsg.MIAO_SHA_OVER);
            return "miaosha_fail";
        }
        // 判断是否已经秒杀到
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
        if (order != null) {
            model.addAttribute("errmsg", CodeMsg.REPEATE_MIAOSHA);
            return "miaosha_fail";
        }
        // 减库存->下订单->写入秒杀订单
        OrderInfo orderInfo = miaoshaService.miaosha(user, goods); // 通过事务保证原子性操作
        model.addAttribute("orderInfo", orderInfo);
        model.addAttribute("goods", goods);
        return "order_detail";
    }

}
