package com.hzc.secKill.Service;

import com.hzc.secKill.DAO.MiaoshaUserDAO;
import com.hzc.secKill.Domain.LoginVo;
import com.hzc.secKill.Domain.MiaoshaUser;
import com.hzc.secKill.Exception.GlobalException;
import com.hzc.secKill.Redis.MiaoshaUserKey;
import com.hzc.secKill.Redis.RedisService;
import com.hzc.secKill.Result.CodeMsg;
import com.hzc.secKill.Utils.MD5Util;
import com.hzc.secKill.Utils.UUIDUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

@Service
public class MiaoshaUserService {

    public static final String COOKIE_NAME_TOKEN = "token";

    @Autowired
    MiaoshaUserDAO miaoshaUserDAO;

    @Autowired
    RedisService redisService;

    public MiaoshaUser getById(long id) {
        return miaoshaUserDAO.getById(id);
    }

    public boolean login(HttpServletResponse response, LoginVo loginVo) {
        if (null == loginVo) {
            throw new GlobalException(CodeMsg.SERVER_ERROR);
        }
        String mobile = loginVo.getMobile();
        String formPass = loginVo.getPassword();
        // 判断手机号是否存在
        MiaoshaUser user = getById(Long.parseLong(mobile));
        if (null == user) {
            throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
        }
        // 验证密码
        String dbPass = user.getPassword();
        String dbSalt = user.getSalt();
        String calculatePass = MD5Util.formPassFromDBPass(formPass, dbSalt);
        if (!calculatePass.equals(dbPass)) {
            throw new GlobalException(CodeMsg.PASSWORD_ERROR);
        }
        // 生成 cookie
        String token = UUIDUtil.uuid();
        addCookie(response, token, user);
        return true;
    }

    public MiaoshaUser getByToken(HttpServletResponse response, String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        MiaoshaUser user = redisService.get(MiaoshaUserKey.token, token, MiaoshaUser.class);
        // 延长有效期
        if (user != null) {
            addCookie(response, token, user);
        }
        return user;
    }

    private void addCookie(HttpServletResponse response, String token, MiaoshaUser user) {
        redisService.set(MiaoshaUserKey.token, token, user); // redis 生成 token 的缓存
        Cookie cookie = new Cookie(COOKIE_NAME_TOKEN, token);
        cookie.setMaxAge(MiaoshaUserKey.token.expireSeconds());
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}
