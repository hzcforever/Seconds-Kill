package com.hzc.secKill.Controller;

import com.hzc.secKill.Domain.GoodsVo;
import com.hzc.secKill.Domain.MiaoshaOrder;
import com.hzc.secKill.Domain.MiaoshaUser;
import com.hzc.secKill.RabbitMQ.MQSender;
import com.hzc.secKill.RabbitMQ.MiaoshaMessage;
import com.hzc.secKill.Redis.GoodsKey;
import com.hzc.secKill.Redis.MiaoshaKey;
import com.hzc.secKill.Redis.OrderKey;
import com.hzc.secKill.Redis.RedisService;
import com.hzc.secKill.Result.CodeMsg;
import com.hzc.secKill.Result.ResultUtil;
import com.hzc.secKill.Service.GoodsService;
import com.hzc.secKill.Service.MiaoshaService;
import com.hzc.secKill.Service.MiaoshaUserService;
import com.hzc.secKill.Service.OrderService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/miaosha")
public class MiaoshaController implements InitializingBean {

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

    @Autowired
    MQSender sender;

    private Map<Long, Boolean> localOverMap = new HashMap<>();

    @Override
    public void afterPropertiesSet() throws Exception { // 系统初始化时调用
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        if (null == goodsList) {
            return;
        }
        for (GoodsVo goods : goodsList) {
            redisService.set(GoodsKey.getMiaoshaGoodsStock, "" + goods.getId(), goods.getStockCount());
            localOverMap.put(goods.getId(), false);
        }
    }

    /**
     * GET POST 有什么区别？
     * GET是幂等的，POST 不是幂等的
     */

    /**
     * 秒杀接口优化：减少对数据库的访问
     * 1. 系统初始化，将商品库存数量加载到 Redis
     * 2. 收到请求，Redis 预减库存，库存不足则直接返回，否则进入3
     * 3. 请求入队，立即返回排队中
     * 4. 请求出队，生成订单，减少库存
     * 5. 客户端轮询，是否秒杀成功
     */

    @RequestMapping(value = "/do_miaosha")
    @ResponseBody
    public ResultUtil<Integer> miaosha(Model model, MiaoshaUser user,
                                       @RequestParam("goodsId") long goodsId) {
        model.addAttribute("user", user);
        if (null == user) {
            return ResultUtil.error(CodeMsg.SESSION_ERROR);
        }

        // 内存标记，减少对 Redis 的访问
        boolean over = localOverMap.get(goodsId);
        if (over) {
            return ResultUtil.error(CodeMsg.MIAO_SHA_OVER);
        }

        // 预减库存
        long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock, "" + goodsId);
        if (stock <= 0) {
            localOverMap.put(goodsId, true);
            return ResultUtil.error(CodeMsg.MIAO_SHA_OVER);
        }
        // 判断是否已经秒杀到
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
        if (order != null) {
            return ResultUtil.error(CodeMsg.REPEATE_MIAOSHA);
        }
        // 入队
        MiaoshaMessage mm = new MiaoshaMessage();
        mm.setUser(user);
        mm.setGoodsId(goodsId);
        sender.sendMiaoshMessage(mm);
        return ResultUtil.success(0); // 0 表示排队中

        /**
         // 判断库存
         GoodsVo goods = goodsService.getGoodVoByGoodsId(goodsId);
         int stock = goods.getStockCount();
         if (stock <= 0) {
         return ResultUtil.error(CodeMsg.MIAO_SHA_OVER);
         }
         // 判断是否已经秒杀到
         MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
         if (order != null) {
         return ResultUtil.error(CodeMsg.REPEATE_MIAOSHA);
         }
         // 减库存->下订单->写入秒杀订单
         OrderInfo orderInfo = miaoshaService.miaosha(user, goods); // 通过事务保证业务逻辑的一致
         return ResultUtil.success(orderInfo);
         **/

    }

    /**
     * orderId：成功
     * -1：秒杀失败
     * 0：排队中
     *
     */
    @RequestMapping(value = "/result", method = RequestMethod.GET)
    @ResponseBody
    public ResultUtil<Long> miaoshaResult(Model model, MiaoshaUser user,
                                          @RequestParam("goodsId") long goodsId) {
        model.addAttribute("user", user);
        if (user == null) {
            return ResultUtil.error(CodeMsg.SESSION_ERROR);
        }
        long result = miaoshaService.getMiaoshaResult(user.getId(), goodsId);
        return ResultUtil.success(result);
    }

    @RequestMapping(value = "/reset", method = RequestMethod.GET)
    @ResponseBody
    public ResultUtil<Boolean> reset(Model model) {
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        for (GoodsVo goods : goodsList) {
            goods.setStockCount(10);
            redisService.set(GoodsKey.getMiaoshaGoodsStock, "" + goods.getId(), 10);
            localOverMap.put(goods.getId(), false);
        }
        redisService.delete(OrderKey.getMiaoshaOrderByUidGid);
        redisService.delete(MiaoshaKey.isGoodsOver);
        miaoshaService.reset(goodsList);
        return ResultUtil.success(true);
    }

}
