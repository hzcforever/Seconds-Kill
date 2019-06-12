package com.hzc.secKill.Redis;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Service
public class RedisService {

    @Autowired
    JedisPool jedisPool;

    // 获取对象
    public <T> T get(KeyPrefix prefix, String key, Class<T> clazz) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            // 生成真正的 key
            String realKey = prefix.getPrefix() + key;
            String val = jedis.get(realKey);
            T t = stringToBean(val, clazz);
            return t;
        } finally {
            returnToPool(jedis);
        }
    }

    // 设置对象
    public <T> boolean set(KeyPrefix prefix, String key, T value) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String s = beanToString(value);
            if (null == s || s.length() <= 0) {
                return false;
            }
            // 生成真正的 key
            String realKey = prefix.getPrefix() + key;
            int seconds = prefix.expireSeconds();
            if (seconds <= 0) {
                jedis.set(realKey, s);
            } else {
                jedis.setex(realKey, seconds, s);
            }
            return true;
        } finally {
            returnToPool(jedis);
        }
    }

    // 判断是否存在
    public <T> boolean exists(KeyPrefix prefix, String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            // 生成真正的 key
            String realKey = prefix.getPrefix() + key;
            return jedis.exists(realKey);
        } finally {
            returnToPool(jedis);
        }
    }

    // 增加
    public <T> Long incr(KeyPrefix prefix, String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            // 生成真正的 key
            String realKey = prefix.getPrefix() + key;
            return jedis.incr(realKey);
        } finally {
            returnToPool(jedis);
        }
    }

    // 减少
    public <T> Long decr(KeyPrefix prefix, String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            // 生成真正的 key
            String realKey = prefix.getPrefix() + key;
            return jedis.decr(realKey);
        } finally {
            returnToPool(jedis);
        }
    }

    private <T> String beanToString(T value) {
        if (value == null) {
            return null;
        }
        Class<?> clazz = value.getClass();
        if (clazz == int.class || clazz == Integer.class) {
            return "" + value;
        } else if (clazz == String.class) {
            return (String) value;
        }
        return JSON.toJSONString(value);
    }

    private <T> T stringToBean(String s, Class<T> clazz) {
        if (null == s || s.length() <= 0 || null == clazz) {
            return null;
        }
        if (clazz == int.class || clazz == Integer.class) {
            return (T) Integer.valueOf(s);
        } else if (clazz == String.class) {
            return (T) s;
        }
        return JSON.toJavaObject(JSON.parseObject(s), clazz);
    }

    private void returnToPool(Jedis jedis) {
        if (jedis != null) {
            jedis.close();
        }
    }
}
