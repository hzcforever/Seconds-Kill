package com.hzc.secKill.Redis;

public interface KeyPrefix {

    int expireSeconds();
    String getPrefix();
}
