package com.bookorder.controller;

import com.bookorder.common.Result;
import com.bookorder.dto.LoginRequest;
import com.bookorder.dto.RegisterRequest;
import com.bookorder.dto.UserInfoVO;
import com.bookorder.entity.SysUser;
import com.bookorder.mapper.SysUserMapper;
import com.bookorder.security.SysUserDetails;
import com.bookorder.service.SysUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private SysUserService userService;

    @Autowired
    private SysUserMapper userMapper;

    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginRequest request) {
        String token = userService.login(request.getUsername(), request.getPassword());
        return Result.success(token);
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request.getUsername(), request.getPassword(), request.getNickname());
        return Result.success();
    }

    @GetMapping("/me")
    public Result<UserInfoVO> me(@AuthenticationPrincipal SysUserDetails userDetails) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(userDetails.getId());
        vo.setUsername(userDetails.getUsername());
        vo.setNickname(userDetails.getNickname());

        SysUser user = userService.getUserInfo(userDetails.getId());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());

        List<String> roles = userMapper.selectRoleCodesByUserId(userDetails.getId());
        List<String> permissions = userMapper.selectPermissionCodesByUserId(userDetails.getId());
        vo.setRoles(roles);
        vo.setPermissions(permissions);

        return Result.success(vo);
    }
}
