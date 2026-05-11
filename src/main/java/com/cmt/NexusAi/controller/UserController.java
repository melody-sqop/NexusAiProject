package com.cmt.NexusAi.controller;

import com.cmt.NexusAi.common.BaseResponse;
import com.cmt.NexusAi.common.ResultUtils;
import com.cmt.NexusAi.constant.UserConstant;
import com.cmt.NexusAi.model.dto.UserPasswordUpdateDTO;
import com.cmt.NexusAi.model.entity.User;
import com.cmt.NexusAi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/user")
@Tag(name = "用户管理")
public class UserController {
    
    @Resource
    private UserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册（手机号/邮箱）")
    public BaseResponse<String> register(@RequestBody User user) {
        String phone = user.getPhone();
        String password = user.getPassword();
        String username = user.getUsername();
        String email = user.getEmail();

        if (StringUtils.hasText(phone)) {
            return userService.registerByPhone(username, phone, password);
        }
        if (StringUtils.hasText(email)) {
            return userService.registerByEmail(username, email, password);
        }
        return ResultUtils.error(400, "请输入手机号或者邮箱");
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public BaseResponse<?> login(@RequestBody User user) {
        return userService.loginUser(user);
    }

    @PostMapping("/updatePassword")
    @Operation(summary = "修改密码（手机号/邮箱）")
    public BaseResponse<String> updatePassword(@RequestBody UserPasswordUpdateDTO dto) {
        String password = dto.getPassword();
        String newPassword = dto.getNewpassword();
        String phone = dto.getPhone();
        String email = dto.getEmail();

        if (!StringUtils.hasText(password) || !StringUtils.hasText(newPassword)) {
            return ResultUtils.error(400, "请输入原密码以及要修改的密码");
        }

        if (StringUtils.hasText(phone)) {
            return userService.updateByPhone(phone, password, newPassword);
        } else if (StringUtils.hasText(email)) {
            return userService.updateByEmail(email, password, newPassword);
        }

        return ResultUtils.error(400, "请输入正确的手机号码或邮箱");
    }

    @GetMapping("/get/login")
    @Operation(summary = "获取当前登录用户信息")
    public BaseResponse<User> getLoginUser(HttpServletRequest request) {
        User loginUser = (User) request.getSession().getAttribute(UserConstant.LOGIN_USER);
        return ResultUtils.success(loginUser);
    }
}
