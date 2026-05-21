package com.cmt.NexusAi.modules.user.service.imp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmt.NexusAi.common.BaseResponse;
import com.cmt.NexusAi.common.ResultUtils;
import com.cmt.NexusAi.modules.security.constant.UserConstant;
import com.cmt.NexusAi.modules.user.mapper.RoleMapper;
import com.cmt.NexusAi.modules.user.mapper.UserMapper;
import com.cmt.NexusAi.modules.user.mapper.UserRoleMapper;
import com.cmt.NexusAi.modules.user.model.dto.LoginUserDTO;
import com.cmt.NexusAi.modules.security.model.entity.Role;
import com.cmt.NexusAi.modules.user.model.entity.User;
import com.cmt.NexusAi.modules.user.model.entity.UserRole;
import com.cmt.NexusAi.modules.user.service.UserService;
import com.cmt.NexusAi.modules.security.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper,User>
        implements UserService, UserDetailsService {

    private static final String USER_ROLE_KEY = "user:role:";

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    @Lazy
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RoleMapper roleMapper;


    @Override
    public boolean isNewUser(Long userId) {
        User user = this.getById(userId);
        if (user == null || user.getRegisterTime() == null) {
            return false;
        }
        // LocalDateTime 可以直接比较
        return user.getRegisterTime().isAfter(LocalDateTime.now().minusDays(7));
    }

    @Override
    public String getUserNameById(Long senderId) {
        return this.getById(senderId).getUsername();
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 根据手机号查询用户
        LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
        userQuery.eq(User::getPhone, username);
        User user = this.getOne(userQuery);

        if (user == null) {
            log.info("用户不存在，手机号：{}", username);
            throw new UsernameNotFoundException("用户不存在");
        }

        // 2. 通过用户ID，在user_role表查出绑定的角色ID
        LambdaQueryWrapper<UserRole> urQuery = new LambdaQueryWrapper<>();
        urQuery.eq(UserRole::getUid, user.getId());
        UserRole userRole = userRoleMapper.selectOne(urQuery);

        // 3. 根据rid去role表，查出真实的角色编码rcode
        Role role = roleMapper.selectById(userRole.getRid());

        // 4. 把真实角色编码（USER/CREATOR等）装入集合
        List<String> roleCodes = List.of(role.getRcode());

        // 5. 传入角色编码，构建登录用户对象
        return new LoginUserDTO(user, roleCodes);
    }

    @Override
    public BaseResponse<String> registerByPhone(String name, String phone, String password) {
        if (phone == null || phone.length() != 11) {
            return ResultUtils.error(400, "手机格式出错(11位) 请重试");
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        User existUser = this.getOne(queryWrapper);

        if (existUser != null) {
            return ResultUtils.error(400, "手机号码已存在 请重新输入");
        }

        User user = new User();
        user.setUsername(name);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(password));
        // 邮箱给空字符串，避免数据库报错
        user.setEmail("");
        user.setRegisterTime(LocalDateTime.now());
        boolean saved = this.save(user);
        if (!saved) {
            return ResultUtils.error(500, "注册失败");
        }

        // =============== 注册成功 → 自动赋予【普通用户 USER 角色】 ===============
        UserRole userRole = new UserRole();
        userRole.setUid(user.getId()); // 刚注册的用户ID
        userRole.setRid(1L);
        // 角色ID：1 = 普通用户（你数据库里的）
        userRoleMapper.insert(userRole);

        return ResultUtils.success("手机号码注册成功");
    }

    // TODO 后期改成真实邮箱验证
    @Override
    public BaseResponse<String> registerByEmail(String name, String email, String password) {
        if (!StringUtils.hasText(email)) {
            return ResultUtils.error(400, "邮箱不能为空");
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, email);
        User existUser = this.getOne(queryWrapper);

        if (existUser != null) {
            return ResultUtils.error(400, "邮箱已存在 请重新输入");
        }

        User user = new User();
        user.setUsername(email);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhone("");

        boolean saved = this.save(user);
        user.setRegisterTime(LocalDateTime.now()); // 增加注册时间
        if (!saved) {
            return ResultUtils.error(500, "注册失败");
        }

        // ====================== 自动赋予默认角色 USER(rid=1) ======================
        UserRole userRole = new UserRole();
        userRole.setUid(user.getId());
        userRole.setRid(1L);
        userRoleMapper.insert(userRole);

        return ResultUtils.success("邮箱注册成功");
    }

    @Override
    public BaseResponse<String> loginUserByEmail(String email, String password) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, email);
        User user = this.getOne(queryWrapper);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResultUtils.error(400, "邮箱不存在或密码错误");
        }

        HashMap<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("id", user.getId());
        String jwt = JwtUtil.getJwt(claims);
        
        return ResultUtils.success(jwt);
    }

    @Override
    public BaseResponse<String> loginUserByPhone(String phone, String password) {
        System.out.println("进入登录手机号方法 "+ "phone"+"--->"+"password");
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        User user = this.getOne(queryWrapper);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResultUtils.error(400, "手机号码不存在或密码错误");
        }

        HashMap<String, Object> claims = new HashMap<>();
        claims.put("phone", phone);
        claims.put("id", user.getId());
        String jwt = JwtUtil.getJwt(claims);
        
        return ResultUtils.success(jwt);
    }

    @Override
    public BaseResponse<String> updateByPhone(String phone, String password, String newPassword) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        User user = this.getOne(queryWrapper);

        if (user == null) {
            return ResultUtils.error(400, "手机号码不存在");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResultUtils.error(400, "原密码错误");
        }

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getPhone, phone)
                .set(User::getPassword, passwordEncoder.encode(newPassword));
        
        boolean updated = this.update(updateWrapper);
        if (updated) {
            return ResultUtils.success("修改密码成功");
        } else {
            return ResultUtils.error(500, "修改密码失败");
        }
    }

    @Override
    public BaseResponse<String> updateByEmail(String email, String password, String newPassword) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, email);
        User user = this.getOne(queryWrapper);

        if (user == null) {
            return ResultUtils.error(400, "邮箱不存在");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResultUtils.error(400, "原密码错误");
        }

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getUsername, email)
                .set(User::getPassword, passwordEncoder.encode(newPassword));
        
        boolean updated = this.update(updateWrapper);
        if (updated) {
            return ResultUtils.success("修改密码成功");
        } else {
            return ResultUtils.error(500, "修改密码失败");
        }
    }

    @Override
    public BaseResponse<Map<String, Object>> loginUser(User user) {
        try {
            UsernamePasswordAuthenticationToken authenticationToken = 
                    new UsernamePasswordAuthenticationToken(user.getPhone(), user.getPassword());

            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            LoginUserDTO loginUser = (LoginUserDTO) authentication.getPrincipal();


            Collection<? extends GrantedAuthority> authorities = loginUser.getAuthorities();

            String roles = "";
            if (authorities != null && !authorities.isEmpty()) {
                roles = authorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                        .collect(Collectors.joining(","));
            }

            Map<String, Object> claims = new HashMap<>();
            claims.put("id", loginUser.getUserId());
            claims.put("roles", roles);

            String token = JwtUtil.getJwt(claims);

            log.info("用户【{}】，真实角色列表：{}", user.getUsername(), roles);

            if (StringUtils.hasText(roles)) {
                redisTemplate.opsForValue().set(USER_ROLE_KEY + loginUser.getUserId(), roles, 30, TimeUnit.MINUTES);
            }

            LinkedHashMap<String, Object> data = new LinkedHashMap<>();
            data.put("token", token);
            data.put("id", loginUser.getUserId());
            data.put("phone", loginUser.getPhone());
            data.put("name", loginUser.getUsername());

            return ResultUtils.success(data);

        } catch (BadCredentialsException e) {
            log.error("用户登录失败: 用户名或密码错误");
            return ResultUtils.error(400, "用户名或密码错误");
        } catch (Exception e) {
            log.error("用户登录异常: {}", e.getMessage());
            return ResultUtils.error(500, "用户名或密码错误");
        }
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        return (User) request.getSession().getAttribute(UserConstant.LOGIN_USER);
    }

}
