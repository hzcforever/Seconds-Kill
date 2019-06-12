package com.hzc.secKill.Controller;


import com.hzc.secKill.Domain.User;
import com.hzc.secKill.Redis.RedisService;
import com.hzc.secKill.Redis.UserKey;
import com.hzc.secKill.Result.CodeMsg;
import com.hzc.secKill.Result.ResultUtil;
import com.hzc.secKill.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class DemoController {

    @RequestMapping("/hello")
    @ResponseBody
    public String home() {
        return "hello world!";
    }

    // 1. rest api json 2. 页面
    @RequestMapping("/success")
    @ResponseBody
    public ResultUtil<String> success() {
        return ResultUtil.success("hello");
    }

    @RequestMapping("/hzc")
    @ResponseBody
    public ResultUtil<String> error() {
        return ResultUtil.error(CodeMsg.SERVER_ERROR);
    }

    @RequestMapping("/thymeleaf")
    public String thymeleaf(Model model) {
        model.addAttribute("name", "huangzichen");
        return "hello";
    }

    @Autowired
    UserService userService;

    @RequestMapping("/db")
    @ResponseBody
    public ResultUtil<User> dbGet() {
        User user = userService.getById(2);
        return ResultUtil.success(user);
    }

    @Autowired
    RedisService redisService;

    @RequestMapping("/redis/get")
    @ResponseBody
    public ResultUtil<User> redisGet() {
        User user = redisService.get(UserKey.getById, "" + 111, User.class);
        return ResultUtil.success(user);
    }
    @RequestMapping("/redis/set")
    @ResponseBody
    public ResultUtil<Boolean> redisSet() {
        User user = new User();
        user.setId(111);
        user.setName("HuangZiChen");
        boolean val = redisService.set(UserKey.getById, "" + 111, user);
        return ResultUtil.success(val);
    }

}
