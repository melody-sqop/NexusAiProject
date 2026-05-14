package com.cmt.NexusAi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cmt.NexusAi.common.BaseResponse;
import com.cmt.NexusAi.model.entity.User;
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

    boolean isNewUser(Long userId);
}
