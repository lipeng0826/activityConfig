package com.bookorder.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookorder.entity.SysUser;
import com.bookorder.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动时初始化管理员密码（确保 BCrypt 哈希正确）
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        SysUser admin = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "admin"));

        if (admin != null) {
            // 用正确的 BCrypt 哈希更新密码
            admin.setPassword(passwordEncoder.encode("admin123"));
            userMapper.updateById(admin);
            log.info("管理员密码已重置为: admin123");
        } else {
            log.warn("未找到 admin 用户，请检查 init.sql 是否已执行");
        }
    }
}
