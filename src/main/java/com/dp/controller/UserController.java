package com.dp.controller;

import cn.hutool.core.bean.BeanUtil;
import com.dp.dto.LoginFormDTO;
import com.dp.dto.Result;
import com.dp.dto.UserDTO;
import com.dp.entity.User;
import com.dp.entity.UserInfo;
import com.dp.service.IUserInfoService;
import com.dp.service.IUserService;
import com.dp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private IUserService userService;

    @Autowired
    private IUserInfoService userInfoService;

    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone) {
        return userService.sendCode(phone);
    }
    @PostMapping("login")
    public Result login(@RequestBody LoginFormDTO loginFormDTO){
        return userService.login(loginFormDTO);
    }
    @GetMapping("me")
    public Result me(){
        return Result.ok(UserHolder.getUser());
    }
    @GetMapping("{id}")
    public Result getUserById(@PathVariable("id") Long id) {
        User user = userService.getById(id);
        if(user == null) return Result.ok();
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        return Result.ok(userDTO);
    }
    @GetMapping("test")
    public Result test(){
        return Result.ok("ok");
    }

    @PostMapping("logout")
    public Result logout(HttpServletRequest request) {
        return userService.logout(request);
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }

}
