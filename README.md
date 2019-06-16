# Seconds-Kill

本项目是一个模拟高并发环境下基于 SpringBoot 的秒杀购物平台。为了减少对数据库的直接访问，通过 Redis 实现了缓存优化；并通过 RabbitMQ 消息中间件来接收大量的并发请求，实现异步和削峰，然后再慢慢地更新数据库。最后通过 Jmeter 压测工具，可以很好地对比优化前后的 QPS...

## 目录

- [项目的基本配置及背景](#项目的基本配置及背景)
- [明文密码两次 MD5 加密](#明文密码两次-MD5-加密)
- [分布式 Session](#分布式-Session)
- [秒杀功能开发](#秒杀功能开发)
- [第一次压测](#第一次压测)
	- [Jmeter 快速入门](#Jmeter-快速入门)
	- [自定义变量模拟多用户](#自定义变量模拟多用户) 
	- [命令行压测](#命令行压测)
- [页面级高并发秒杀优化](#页面级高并发秒杀优化)
	- [商品列表页缓存实现](#商品列表页缓存实现) 
	- [热点数据对象缓存](#热点数据对象缓存)
	- [商品详情静态化](#商品详情静态化)
	- [秒杀接口前后端分离](#秒杀接口前后端分离)
	- [解决超卖问题](#解决超卖问题)
- [服务级高并发秒杀优化](#服务级高并发秒杀优化)
	- [集成 RabbitMQ](#集成-RabbitMQ)
	- [Redis 预减库存](#Redis-预减库存)
	- [RabbitMQ 异步下单](#RabbitMQ-异步下单)
- [第二次压测](#第二次压测)
- [写在最后](#写在最后)

## 项目的基本配置及背景

项目用的是 SpringBoot 2.1.5、thymeleaf 2.0.4、MyBatis 1.3.2、MySQL 8.0、最新版本的 Redis、Jmeter 压测工具和 RabbitMQ 消息中间件，其中 Redis、Jmeter 和 RabbitMQ 的相关配置是部署在虚拟机上，最后将项目打成 jar 包在虚拟机上运行。具体的环境配置过程稍微有点复杂，网上有具体博客可作参考，这里不加详述。

本项目是一个秒杀系统，秒杀与其他业务最大的区别在于秒杀的瞬间：

1. 系统的并发量会非常的大

2. 并发量大的同时，网络的流量也会瞬间变大

关于第一点，核心问题在于如何在大并发的情况下能保证 DB 能扛得住压力，因为大并发的瓶颈就在于 DB。如果说请求直接从前端透传到 DB，显然，DB 是无法承受几十万上百万甚至上千万的并发量的。所以，我们能做的只能是减少对 DB 的访问，比如前端发出了1000万个请求，通过我们的处理，最终只有100个会访问 DB，这样就可以了！针对秒杀这种场景，因为秒杀商品的数量是有限的，这种做法刚好适用！

关于第二点，最常用的办法就是做页面静态化，也就是常说的前后端分离：把静态页面直接缓存到用户的浏览器端，所需要的数据从服务端接口动态获取。这样会大大节省网络的流量，再加上 CDN，一般不会有大问题。

**如何减少DB的访问？**

假如某个商品可秒杀的数量是10，那么在秒杀活动开始之前，把商品的 goodsId 和数量加载到 Redis 缓存。服务端收到请求的时候，首先预减一下 Redis 里面的数量，如果数量减到0随后的访问直接返回秒杀失败。也就是说，只有10个请求最终会去实际地请数据库。

当然，如果我们的商品数比较多，10000件商品参与秒杀，10000 * 10 = 100000个并发去请求 DB，DB 的压力还是会很大，这里就用到另一个非常重要的组件：消息队列。我们不是把请求直接去访问 DB，而是先把请求写到消息队列，做一个缓存，然后再去缓慢地更新数据库。这样做以后，前端用户的请求可能不会立即得到响应是成功还是失败，很可能得到的是一个排队中的返回值。这个时候，需要客户端再去服务端轮询，因为我们不能保证一定就秒杀成功了。当服务端出队，生成订单以后，把用户 id 和商品 goodsId 写到缓存中，来应对客户端的轮询就可以了。

这样处理以后，我们的应用是可以很简单的进行分布式横向扩展的，以应对更大的并发。

## 明文密码两次 MD5 加密

通过两次 MD5 加密提高数据校验的安全性。第一次 MD5 是防止用户的明文密码在网络上传输，第二次 MD5 是防止网上相关的 MD5 解密反查。

第一次 MD5 加密：用户端：password = MD5(明文 + 固定 salt -> "1a2b3c4d")

第二次 MD5 加密：password = MD5(用户端输入 + 随机 salt)

数据库中 user 表里插入的即为第二次加密的密码和随机 salt。

## 分布式 Session

每次登录都会生成一个 token 并把它加入到 Cookie 中，在跳转不同页面时，会把 token 对应的 user 从 Redis 中取出。

改进：通过 UserArgumentResolver 封装之前加入 Cookie、由 token 得到 user 等一系列操作。

    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        webRequest.getNativeRequest();
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
    
        String paramToken = request.getParameter(MiaoshaUserService.COOKIE_NAME_TOKEN);
        String cookieToken = getCookieValue(request, MiaoshaUserService.COOKIE_NAME_TOKEN);
        if (StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
            return null;
        }
        String token = StringUtils.isEmpty(paramToken) ? cookieToken : paramToken;
        return userService.getByToken(response, token);
    }

## 秒杀功能开发

**主要思路：**

1. 首先判断秒杀商品的库存，如果小于等于0，则直接返回秒杀失败
2. 判断是否已经秒杀到了商品，即从数据库中查看该用户是否已经存在对应商品的订单，如果有则重复秒杀
3. 减库存
4. 生成订单(order_info + miaosha_order)

但存在一些问题，比如高并发下存在的超卖问题，以及怎样有效地减少大量对数据库访问的请求...

## 第一次压测

待更...
