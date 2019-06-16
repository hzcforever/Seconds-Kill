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
	- [商品查询与秒杀下单压测](#商品查询与秒杀下单压测)
- [页面级高并发秒杀优化](#页面级高并发秒杀优化)
	- [商品列表页缓存实现](#商品列表页缓存实现) 
	- [热点数据对象缓存](#热点数据对象缓存)
	- [解决超卖问题](#解决超卖问题)
- [服务级高并发秒杀优化](#服务级高并发秒杀优化)
	- [集成 RabbitMQ](#集成-RabbitMQ)
	- [Redis 预减库存和RabbitMQ 异步下单](#Redis-预减库存和RabbitMQ-异步下单)
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

### Jmeter 快速入门

在 [jmeter 官网](https://jmeter.apache.org/download_jmeter.cgi) 下载“.tgz”结尾的安装包在虚拟机 CentOS 7(带桌面系统) 安装，下载“.zip”结尾的压缩包在本机 windows 10 环境下安装，具体安装过程可自行百度，

在虚拟机上安装的 jmeter 通过在 bin 文件夹下输入命令行 ./jmeter.sh 运行 jmeter；在 windows 上安装的 jmeter 通过在打开 bin 文件夹下的 jmeter.bat 文件运行 jmeter。

**线程组**

测试计划 -> 添加 -> Threads -> 线程组

本项目设置线程数为5000，循环次数为10，即总共50000个并发。

**HTTP 请求默认值**

线程组 -> 配置元件 -> HTTP 请求默认值

协议：http

服务器名称或IP：虚拟机的 IP

端口号：8080

**HTTP请求**

线程组 -> Sample -> HTTP请求

方法：GET

路径：本项目中用到了 /goods/to_list(商品查询) 和 /miaosha/do_miaosha(秒杀下单) 两个路径

**聚合报告**

线程组 -> 监听器 -> 聚合报告

显示压测的具体情况，包括并发量、Error、QPS 等信息。

### 自定义变量模拟多用户

**Jmeter 自定义变量**

线程组 -> 配置元件 -> CSV Data Set Config

通过配置文件模拟多用户 token 访问页面，实现秒杀订单。

配置文件格式为：userId,token

在 HTTP 请求页中添加参数 token(参数名称)，参数值用 ${token} 作为变量的引用，最后开始压测。

### 商品查询与秒杀下单压测

压测环境是 VMware 虚拟机，系统为 CentOS 7，2G 内存，CPU 两个 processor。

商品查询压测结果：/goods/to_list

<div align="center"><img src="/img//first_to_list.png" width=""/></div>

秒杀下单压测结果：/miaosha/do_miaosha

<div align="center"><img src="/img//first_do_miaosha.png" width=""/></div>

## 页面级高并发秒杀优化

这一节主要讨论使用页面优化技术来提升秒杀系统性能，即利用缓存最大程度地减少对用户数据库的直接访问，并解决超卖现象。

### 商品列表页缓存实现

最开始对于商品的查询优化是将 user 和 goodsList 直接加入到 model 中，然后通过动态渲染模板在浏览器端展示出来，接下来考虑如何做页面缓存。

先看修改后的代码：

    @RequestMapping(value = "/to_list", produces = "text/html")
    @ResponseBody
    public String toList(HttpServletRequest request, HttpServletResponse response,
     Model model, MiaoshaUser user) {
        model.addAttribute("user", user);
        // 取缓存
        String html = redisService.get(GoodsKey.getGoodsList, "", String.class);
            if (!StringUtils.isEmpty(html)) {
            return html;
        }
        // 查询商品列表
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        model.addAttribute("goodsList", goodsList);
    
        // 手动渲染
        IWebContext ctx = new WebContext(request, response, request.getServletContext(),
        request.getLocale(), model.asMap());
        html = thymeleafViewResolver.getTemplateEngine().process("goods_list", ctx);
        if (!StringUtils.isEmpty(html)) {
            redisService.set(GoodsKey.getGoodsList, "", html);
        }
        return html;
    }

首先，查看 Redis 缓存中是否存在以 GoodsKey 为 key 的 String 类型的值，如果有且不为空则直接返回；否则通过 listGoodsVo() 查询商品列表，并放进 model 中，这个时候通过 ThymeleafViewResolver 手动渲染模板，如果得到的 html 不为空，则存入缓存(缓存的有效期可设为一分钟)。

其它相关页面的缓存实现以此类推，具体细节见源代码。

### 热点数据对象缓存

原先根据 id 取 user 的方法实现如下：

    public MiaoshaUser getById(long id) {
        return miaoshaUserDAO.getById(id);
    }

显然每次取 user 都要通过 DAO 直接访问数据库，这里对该方法进行改进：

    public MiaoshaUser getById(long id) {
        // 取缓存
        MiaoshaUser user = redisService.get(MiaoshaUserKey.getById, "" + id, MiaoshaUser.class);
        if (user != null) {
            return user;
        }
        // 取数据库
        user = miaoshaUserDAO.getById(id);
        if (user != null) {
            redisService.set(MiaoshaUserKey.getById, "" + id, user);
        }
        return user;
    }

还有更新密码的方法优化：

    public boolean updatePassword(String token, long id, String formPass) {
        // 取user
        MiaoshaUser user = getById(id);
        if(user == null) {
            throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
        }
        // 更新数据库
        MiaoshaUser toBeUpdate = new MiaoshaUser();
        toBeUpdate.setId(id);
        toBeUpdate.setPassword(MD5Util.formPassFromDBPass(formPass, user.getSalt()));
        miaoshaUserDAO.update(toBeUpdate);
        // 删除和更新缓存
        redisService.delete(MiaoshaUserKey.getById, "" + id);
        user.setPassword(toBeUpdate.getPassword());
        redisService.set(MiaoshaUserKey.token, token, user);
        return true;
    }

当更新密码的时候，首先通过 getById(id) 取 user，如果为空则抛出异常；这里对于处理缓存与写库的顺序是先更新数据库再删除缓存(在网上有很多博客都讲的是先删缓存再写库，原因是如果先写库再删缓存，万一删除失败，这时会出现数据库与缓存数据的不一致)，但是经过我的测试，也咨询了一些人，觉得处理缓存失败的概率要远远小于写库失败的概率，因此这里暂且使用先写库再删缓存的次序。

### 解决超卖问题

在高并发环境下，对于某一个共享变量的更新，很容易造成线程安全问题，在这里具体表现为超卖现象。

**超卖现象的解决思路：**

在 SQL 语句中，加入条件判断语句，判断剩余库存是否大于0再去更新。

    @Update("update miaosha_goods set stock_count = stock_count - 1 where goods_id = #{goodsId} " +
    "and stock_count > 0")
    int reduceStock(MiaoshaGoods g);

由于数据库在每次更新的时候会对 miaosha_goods 加锁，因此更新其实是串行执行的，不会出现多个线程同时更新一条记录的情况，所以在这里是通过数据库来保证不会出现超卖现象。

在这里虽然解决了超卖现象，但仍然有一个问题，那就是同一个用户可能发出多个请求，也就是同一个用户秒杀到了多个相同商品。

**同一个用户秒杀多个相同商品的解决思路：**

1. 通过验证码防止出现相关庆幸
2. 在 miaosha_order 表中创建 user_id 与 goods_id 的唯一索引，并且通过 @Transactional 注解的 createOrder 方法中，先生成订单，后生成秒杀订单，如果出现了同一个用户发出的多个请求，则第二次及之后的秒杀请求会导致生成秒杀订单失败，从而引起事务的回滚，这里就能保证同一个用户只能秒杀一种商品一次。

## 服务级高并发秒杀优化

在前面的缓存优化中，我们考虑了如何最大程度地减少对数据库的访问，并解决了秒杀过程中可能出现的相关问题，在本节中我们进一步考虑如何减少对缓存的访问。

1. 通过 Redis 预减库存更进一步减少对数据库的访问
2. 通过内存标记减少对 Redis 的访问
3. 通过 RabbitMQ 将用户请求入队缓冲，实现异步下单，增强用户体验
4. 第二次压测

### 集成 Rabbit MQ

RabbitMQ 是采用 Erlang 语言实现 AMQP（Advanced Message Queuing Protocol，高级消息队列协议）的消息中间件，它最初起源于金融系统，用于在分布式系统中存储转发消息。

RabbitMQ 整体上是一个生产者与消费者模型，主要负责接收、存储和转发消息。可以把消息传递的过程想象成：当你将一个包裹送到邮局，邮局会暂存并最终将邮件通过邮递员送到收件人的手上，RabbitMQ 就好比由邮局、邮箱和邮递员组成的一个系统。从计算机术语层面来说，RabbitMQ 模型更像是一种交换机模型。

在虚拟机上安装并配置 erlang 和 RabbitMQ，在 pom 文件中添加相关依赖，在本机浏览器中输入虚拟机 ip:15672 即可打开 RabbitMQ 的界面。

RabbitMQ 交换机有以下几种类型：fanout、direct、topic、headers 这四种，但 headers 类型的交换器性能比较差，一般不推荐。

### Redis 预减库存和RabbitMQ 异步下单

1. 在系统初始化的时候，把商品库存的数量预加载到 Redis 中(在 afterPropertiesSet 方法中实现预加载过程)
2. 在收到用户秒杀请求后，通过 Redis 预减库存，若库存不足则直接返回秒杀失败，并标记该 goods 已经秒杀完毕；如果库存大于0则入队，并返回正在排队中
3. 请求出队，生成订单，减少库存
4. 客户端轮询，判断是否秒杀成功

秒杀订单：
    
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
        return ResultUtil.success(0); // 0 表示排队
    }

## 第二次压测

为了尽可能保证两次压测结果对比的公平性，内存、CPU配置保持不变，多变量配置文件与第一次压测完全相同。

### 商品查询与秒杀下单压测

加了页面缓存和对象缓存后，商品查询压测结果：/goods/to_list

<div align="center"><img src="/img//second_to_list.png" width=""/></div>

通过 RabbitMQ 实现异步下单后，秒杀下单压测结果：/miaosha/do_miaosha

<div align="center"><img src="/img//second_do_miaosha.png" width=""/></div>

很明显两者的 QPS 有明显的提高，如果将 Redis 和 RabbitMQ 部署在不同的服务器上，提升效果可能会更加明显。

## 写在最后

通过这个秒杀项目对缓存有了更加深刻的理解，通过压测前后的对比更加直观地感受到了缓存的好处。通过 RabbitMQ 将业务逻辑异步化，并在高并发环境下有效地削峰，也能够大幅度地提升系统性能。

在后续的功能优化与扩展中，可以有以下几个思路进行深入思考：

1. 通过隐藏秒杀地址防止恶意刷新
2. 通过数学公式和图片验证码防止恶意刷新
3. 限制同一个用户在固定时间内访问秒杀接口的次数
3. 通过 Ngnix 配置负载均衡，给服务器分流
4. 通过 Redis 集群保证高可用
5. 开启 RabbitMQ 持久化机制保证消息队列的可靠性传输

