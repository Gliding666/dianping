package com.dp.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.dp.dto.LoginFormDTO;
import com.dp.dto.Result;
import com.dp.entity.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public interface IUserService extends IService<User> {
    Result sendCode(String phone);
    Result login(LoginFormDTO loginFormDTO);

    Result logout(HttpServletRequest request);

    Result sign();

    Result signCount();
}
