package com.dp.utils;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        /************** 基于session *****************
        HttpSession session = request.getSession();
        Object user = session.getAttribute("user");
        if(user == null){
            //不存在拦截返回401状态码
            response.setStatus(401);
            return false;
        }
        // 存在，保存用户信息到ThreadLocal
        UserHolder.saveUser((UserDTO) user);
        **********************************************/

        // 1. 判断是否需要拦截 （ThreadLocal中是否有用户）
        if (UserHolder.getUser() == null) {
            // 没有需要拦截，设置状态码
            response.setStatus(401);
            // 拦截
            return false;
        }
        //放行
        return true;
    }
}
