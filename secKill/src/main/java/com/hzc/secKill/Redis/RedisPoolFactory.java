package com.hzc.secKill.Redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPool;

@Service
public class RedisPoolFactory {

    @Autowired
    RedisConfig redisConfig;

    @Bean
    public JedisPool JedisFactory() {
        JedisPool jedisPool = new JedisPool("redis://127.0.0.1:6379/2");
        return jedisPool;
    }
}
