package com.bookorder.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bookorder.entity.SysUser;

public interface SysUserService extends IService<SysUser> {

    SysUser getByUsername(String username);

    String login(String username, String password);

    void register(String username, String password, String nickname);

    SysUser getUserInfo(Long userId);
}
