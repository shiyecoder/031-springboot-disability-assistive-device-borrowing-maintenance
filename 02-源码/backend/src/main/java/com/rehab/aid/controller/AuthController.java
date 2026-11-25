package com.rehab.aid.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rehab.aid.common.BizException;
import com.rehab.aid.common.CurrentUser;
import com.rehab.aid.common.R;
import com.rehab.aid.config.JwtUtil;
import com.rehab.aid.entity.*;
import com.rehab.aid.mapper.*;
import com.rehab.aid.service.UploadService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** 认证、菜单、个人中心与文件上传 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;
    private final UploadService uploadService;

    /** 登录失败计数：连续 5 次锁定 10 分钟 */
    private static final Map<String, int[]> FAIL = new ConcurrentHashMap<>();
    private static final Map<String, Long> LOCK_UNTIL = new ConcurrentHashMap<>();

    public AuthController(SysUserMapper u, SysRoleMapper r, SysMenuMapper m, SysRoleMenuMapper rm,
                          PasswordEncoder e, JwtUtil j, UploadService us) {
        this.userMapper = u; this.roleMapper = r; this.menuMapper = m;
        this.roleMenuMapper = rm; this.encoder = e; this.jwtUtil = j; this.uploadService = us;
    }

    @Data
    public static class LoginReq {
        @NotBlank private String username;
        @NotBlank private String password;
    }

    @PostMapping("/auth/login")
    public R<Map<String, Object>> login(@RequestBody LoginReq req) {
        String uname = req.getUsername().trim();
        Long lock = LOCK_UNTIL.get(uname);
        if (lock != null && lock > System.currentTimeMillis()) {
            throw new BizException("连续登录失败次数过多，请 10 分钟后再试");
        }
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, uname));
        if (user == null || !encoder.matches(req.getPassword(), user.getPassword())) {
            int[] c = FAIL.computeIfAbsent(uname, k -> new int[]{0});
            c[0]++;
            if (c[0] >= 5) {
                LOCK_UNTIL.put(uname, System.currentTimeMillis() + 10 * 60 * 1000L);
                FAIL.remove(uname);
                throw new BizException("连续登录失败 5 次，账号已锁定 10 分钟");
            }
            throw new BizException("账号或密码错误，还可尝试 " + (5 - c[0]) + " 次");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException("账号已停用，请联系机构管理员");
        }
        FAIL.remove(uname);
        user.setLastLogin(LocalDateTime.now());
        userMapper.updateById(user);
        user.setPassword(null);
        String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRoleCode());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        return R.ok(data);
    }

    @GetMapping("/auth/me")
    public R<Map<String, Object>> me() {
        SysUser user = CurrentUser.get();
        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("menus", buildMenuTree(user.getRoleCode()));
        return R.ok(data);
    }

    /** 按角色动态获取菜单树：角色 → 角色菜单 → 菜单 */
    private List<Map<String, Object>> buildMenuTree(String roleCode) {
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
        List<SysMenu> all;
        if (role != null) {
            List<Long> ids = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                    .eq(SysRoleMenu::getRoleId, role.getId())).stream().map(SysRoleMenu::getMenuId).toList();
            all = ids.isEmpty() ? List.of() : menuMapper.selectList(new LambdaQueryWrapper<SysMenu>().in(SysMenu::getId, ids).orderByAsc(SysMenu::getSort));
        } else {
            all = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort));
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (SysMenu m : all) {
            if (m.getParentId() == null || m.getParentId() == 0) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", m.getId());
                node.put("name", m.getName());
                node.put("path", m.getPath());
                node.put("icon", m.getIcon());
                List<Map<String, Object>> children = new ArrayList<>();
                for (SysMenu c : all) {
                    if (m.getId().equals(c.getParentId())) {
                        Map<String, Object> cn = new HashMap<>();
                        cn.put("id", c.getId());
                        cn.put("name", c.getName());
                        cn.put("path", c.getPath());
                        cn.put("icon", c.getIcon());
                        children.add(cn);
                    }
                }
                node.put("children", children);
                roots.add(node);
            }
        }
        return roots;
    }

    @PutMapping("/auth/profile")
    public R<Void> profile(@RequestBody SysUser body) {
        SysUser user = CurrentUser.get();
        if (body.getRealName() != null && !body.getRealName().isBlank()) user.setRealName(body.getRealName().trim());
        if (body.getPhone() != null) user.setPhone(body.getPhone().trim());
        if (body.getAvatar() != null && !body.getAvatar().isBlank()) user.setAvatar(body.getAvatar());
        userMapper.updateById(user);
        return R.ok();
    }

    @Data
    public static class PwdReq {
        private String oldPassword;
        private String newPassword;
    }

    @PostMapping("/auth/password")
    public R<Void> password(@RequestBody PwdReq req) {
        SysUser user = userMapper.selectById(CurrentUser.id());
        if (!encoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BizException("原密码不正确");
        }
        if (req.getNewPassword() == null || req.getNewPassword().length() < 6) {
            throw new BizException("新密码至少 6 位");
        }
        user.setPassword(encoder.encode(req.getNewPassword()));
        userMapper.updateById(user);
        return R.ok();
    }

    /** 文件上传：保存到项目根 uploads/，返回相对路径，禁止外链 */
    @PostMapping("/upload")
    public R<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam(defaultValue = "common") String type) {
        return R.ok(uploadService.save(file, type));
    }

    @GetMapping("/health")
    public R<String> health() {
        return R.ok("ok");
    }
}
