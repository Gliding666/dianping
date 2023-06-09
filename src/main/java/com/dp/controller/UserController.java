package com.dp.controller;

import com.dp.dto.LoginFormDTO;
import com.dp.dto.Result;
import com.dp.entity.User;
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

    @GetMapping("test")
    public Result test(){
        return Result.ok("ok");
    }

    @PostMapping("logout")
    public Result logout(HttpServletRequest request) {
        return userService.logout(request);
    }

}
