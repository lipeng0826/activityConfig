package com.bookorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bookorder.common.BusinessException;
import com.bookorder.entity.SysRole;
import com.bookorder.entity.SysUser;
import com.bookorder.entity.SysUserRole;
import com.bookorder.mapper.SysRoleMapper;
import com.bookorder.mapper.SysUserMapper;
import com.bookorder.mapper.SysUserRoleMapper;
import com.bookorder.security.JwtUtil;
import com.bookorder.security.SysUserDetails;
import com.bookorder.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Override
    public SysUser getByUsername(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }

    @Override
    public String login(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        SysUserDetails userDetails = (SysUserDetails) authentication.getPrincipal();
        return jwtUtil.generateToken(userDetails.getId(), username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(String username, String password, String nickname) {
        if (getByUsername(username) != null) {
            throw new BusinessException(400, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setStatus(1);
        userMapper.insert(user);

        // 默认绑定 READER 角色
        SysRole role = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "READER"));
        if (role != null) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(role.getId());
            userRoleMapper.insert(userRole);
        }
    }

    @Override
    public SysUser getUserInfo(Long userId) {
        return userMapper.selectById(userId);
    }
}
