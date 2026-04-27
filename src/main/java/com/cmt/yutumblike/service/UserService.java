package com.cmt.yutumblike.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cmt.yutumblike.common.BaseResponse;
import com.cmt.yutumblike.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Map;

public interface UserService extends IService<User>, UserDetailsService {

    User getLoginUser(HttpServletRequest request);

    BaseResponse<String> registerByPhone(String name, String phone, String password);

    BaseResponse<String> registerByEmail(String name, String email, String password);

    BaseResponse<String> loginUserByEmail(String email, String password);

    BaseResponse<String> loginUserByPhone(String phone, String password);

    BaseResponse<String> updateByPhone(String phone, String password, String newPassword);

    BaseResponse<String> updateByEmail(String email, String password, String newPassword);

    BaseResponse<Map<String, Object>> loginUser(User user);

}
