package com.dp.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.dp.dto.LoginFormDTO;
import com.dp.dto.Result;
import com.dp.entity.User;

import javax.servlet.http.HttpSession;

public interface IUserService extends IService<User> {
    public Result sendCode(String phone);
    public Result login(LoginFormDTO loginFormDTO);
}
