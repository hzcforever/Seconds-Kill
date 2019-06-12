package com.hzc.secKill.Controller;

import com.hzc.secKill.Domain.LoginVo;
import com.hzc.secKill.Result.ResultUtil;
import com.hzc.secKill.Service.MiaoshaUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@Controller
@RequestMapping("/login")
public class LoginController {

    private static Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    MiaoshaUserService miaoshaUserService;

    @RequestMapping("/to_login")
    public String toLogin() {
        return "login";
    }

    @RequestMapping("/do_login")
    @ResponseBody
    public ResultUtil<Boolean> doLogin(HttpServletResponse response, @Valid LoginVo loginVo) {
        logger.info(loginVo.toString());
        // 参数校验
//        String passInput = loginVo.getPassword();
//        String mobile = loginVo.getMobile();
//        if (StringUtils.isEmpty(passInput)) {
//            return ResultUtil.error(CodeMsg.PASSWORD_EMPTY);
//        }
//        if (StringUtils.isEmpty(mobile)) {
//            return ResultUtil.error(CodeMsg.MOBILE_EMPTY);
//        }
//        if (!ValidatorUtil.isMobile(mobile)) {
//            return ResultUtil.error(CodeMsg.MOBILE_ERROR);
//        }
        // 登录
        miaoshaUserService.login(response, loginVo);
        return ResultUtil.success(true);
    }

}
