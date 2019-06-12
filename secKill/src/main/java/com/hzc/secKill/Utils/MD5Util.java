package com.hzc.secKill.Utils;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * 两次 MD5 加密
 * 1. 用户端：psd = md5(明文 + 固定 salt)
 * 2. 服务端：psd = md5(用户端 + 随机 salt)
 */
public class MD5Util {
    public static String md5(String src) {
        return DigestUtils.md5Hex(src);
    }

    private static final String salt = "1a2b3c4d";

    public static String inputPassFromPass(String inputPass) {
        String str = "" + salt.charAt(0) + salt.charAt(2) + inputPass + salt.charAt(5) + salt.charAt(4);
        return md5(str);
    }

    public static String formPassFromDBPass(String formPass, String salt) {
        String str = "" + salt.charAt(0) + salt.charAt(2) + formPass + salt.charAt(5) + salt.charAt(4);
        return md5(str);
    }

    public static String inputPassToDBPass(String input, String saltDB) {
        String formPass = inputPassFromPass(input);
        String dbPass = formPassFromDBPass(formPass, saltDB);
        return dbPass;
    }

    public static void main(String[] args) {
        System.out.println(inputPassToDBPass("123456", salt));
    }

}
