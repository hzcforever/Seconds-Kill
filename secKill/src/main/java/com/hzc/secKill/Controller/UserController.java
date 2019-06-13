package com.hzc.secKill.Controller;

import com.hzc.secKill.Domain.MiaoshaUser;
import com.hzc.secKill.Result.ResultUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/user")
public class UserController {

    @RequestMapping("/info")
    @ResponseBody
    public ResultUtil<MiaoshaUser> information(Model model, MiaoshaUser user) {
        return ResultUtil.success(user);
    }

}
