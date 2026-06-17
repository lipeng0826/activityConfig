package com.bookorder.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookorder.annotation.OpLog;
import com.bookorder.common.BusinessException;
import com.bookorder.common.Result;
import com.bookorder.entity.SysRole;
import com.bookorder.entity.SysUser;
import com.bookorder.entity.SysUserRole;
import com.bookorder.mapper.SysRoleMapper;
import com.bookorder.mapper.SysUserMapper;
import com.bookorder.mapper.SysUserRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @GetMapping
    public Result<IPage<SysUser>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getNickname, keyword));
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        IPage<SysUser> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        // 清除密码
        page.getRecords().forEach(u -> u.setPassword(null));
        return Result.success(page);
    }

    @PutMapping("/{id}/disable")
    @OpLog(module = "user", operation = "DISABLE")
    public Result<Void> disable(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(404, "用户不存在");
        user.setStatus(0);
        userMapper.updateById(user);
        return Result.success();
    }

    @PutMapping("/{id}/enable")
    @OpLog(module = "user", operation = "ENABLE")
    public Result<Void> enable(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(404, "用户不存在");
        user.setStatus(1);
        userMapper.updateById(user);
        return Result.success();
    }

    @PutMapping("/{id}/roles")
    @OpLog(module = "user", operation = "ASSIGN_ROLE")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(404, "用户不存在");

        // 删除现有角色
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, id));

        // 分配新角色
        List<Long> roleIds = body.get("roleIds");
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(id);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
        return Result.success();
    }

    @GetMapping("/roles")
    public Result<List<SysRole>> allRoles() {
        return Result.success(roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getStatus, 1)));
    }
}
