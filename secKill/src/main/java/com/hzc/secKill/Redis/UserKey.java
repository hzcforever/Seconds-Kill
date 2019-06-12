package com.hzc.secKill.Redis;

public class UserKey extends BasePrefix {

    private UserKey(String prefix) {
        super(prefix);
    }

    public static UserKey getById = new UserKey("id");

}
