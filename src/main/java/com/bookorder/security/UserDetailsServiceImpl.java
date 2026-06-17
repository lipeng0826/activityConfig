package com.bookorder.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookorder.entity.SysUser;
import com.bookorder.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private SysUserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        if (user.getStatus() != 1) {
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        List<String> roleCodes = userMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissionCodes = userMapper.selectPermissionCodesByUserId(user.getId());

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String code : roleCodes) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + code));
        }
        for (String code : permissionCodes) {
            authorities.add(new SimpleGrantedAuthority(code));
        }

        return new SysUserDetails(user, authorities);
    }
}
