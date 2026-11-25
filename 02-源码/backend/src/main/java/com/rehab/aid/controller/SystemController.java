package com.rehab.aid.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rehab.aid.common.BizException;
import com.rehab.aid.common.R;
import com.rehab.aid.dto.PageDTO;
import com.rehab.aid.entity.*;
import com.rehab.aid.mapper.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/** 用户、角色、菜单、消息 */
@RestController
@RequestMapping("/api")
public class SystemController {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMessageMapper msgMapper;
    private final PasswordEncoder encoder;

    public SystemController(SysUserMapper u, SysRoleMapper r, SysMenuMapper m, SysRoleMenuMapper rm,
                            SysMessageMapper ms, PasswordEncoder e) {
        this.userMapper = u; this.roleMapper = r; this.menuMapper = m;
        this.roleMenuMapper = rm; this.msgMapper = ms; this.encoder = e;
    }

    @GetMapping("/user/page")
    public R<Page<SysUser>> page(PageDTO q) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        if (q.getKeyword() != null && !q.getKeyword().isBlank())
            w.and(x -> x.like(SysUser::getUsername, q.getKeyword().trim()).or().like(SysUser::getRealName, q.getKeyword().trim()));
        if (q.getStatus() != null && !q.getStatus().isBlank() && !"ALL".equals(q.getStatus()))
            w.eq(SysUser::getRoleCode, q.getStatus());
        w.orderByAsc(SysUser::getId);
        Page<SysUser> p = userMapper.selectPage(new Page<>(q.getPageNum(), q.getPageSize()), w);
        p.getRecords().forEach(u -> u.setPassword(null));
        return R.ok(p);
    }

    @PostMapping("/user")
    public R<Void> create(@RequestBody SysUser body) {
        if (body.getUsername() == null || body.getUsername().isBlank()) throw new BizException("账号必填");
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, body.getUsername().trim())) > 0)
            throw new BizException("账号已存在");
        if (body.getRoleCode() == null || roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, body.getRoleCode())) == 0)
            throw new BizException("请选择已定义的角色");
        body.setId(null);
        body.setPassword(encoder.encode("123456"));
        body.setStatus(1);
        body.setCreateTime(LocalDateTime.now());
        userMapper.insert(body);
        return R.ok();
    }

    @PutMapping("/user/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody SysUser body) {
        SysUser db = userMapper.selectById(id);
        if (db == null) throw new BizException("用户不存在");
        body.setId(id);
        body.setUsername(db.getUsername());
        body.setPassword(null);
        userMapper.updateById(body);
        return R.ok();
    }

    @PostMapping("/user/{id}/reset")
    public R<Void> reset(@PathVariable Long id) {
        SysUser db = userMapper.selectById(id);
        if (db == null) throw new BizException("用户不存在");
        db.setPassword(encoder.encode("123456"));
        userMapper.updateById(db);
        return R.ok();
    }

    @DeleteMapping("/user/{id}")
    public R<Void> delete(@PathVariable Long id) {
        if (id == 1L) throw new BizException("默认管理员不可删除");
        userMapper.deleteById(id);
        return R.ok();
    }

    @GetMapping("/role/list")
    public R<List<SysRole>> roles() {
        return R.ok(roleMapper.selectList(null));
    }

    @GetMapping("/role/{id}/menus")
    public R<List<Long>> roleMenus(@PathVariable Long id) {
        return R.ok(roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id))
                .stream().map(SysRoleMenu::getMenuId).toList());
    }

    @PostMapping("/role/{id}/menus")
    public R<Void> assign(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        for (Long mid : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(id);
            rm.setMenuId(mid);
            roleMenuMapper.insert(rm);
        }
        return R.ok();
    }

    /** 菜单树：分配菜单时隐藏「菜单管理」本身，避免菜单被误删导致无法进入 */
    @GetMapping("/menu/tree")
    public R<List<Map<String, Object>>> menuTree() {
        List<SysMenu> all = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .ne(SysMenu::getName, "菜单管理").orderByAsc(SysMenu::getSort));
        List<Map<String, Object>> roots = new ArrayList<>();
        for (SysMenu m : all) {
            if (m.getParentId() == null || m.getParentId() == 0) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", m.getId());
                node.put("name", m.getName());
                node.put("path", m.getPath());
                List<Map<String, Object>> ch = new ArrayList<>();
                for (SysMenu c : all) if (m.getId().equals(c.getParentId())) {
                    Map<String, Object> cn = new HashMap<>();
                    cn.put("id", c.getId()); cn.put("name", c.getName()); cn.put("path", c.getPath());
                    ch.add(cn);
                }
                node.put("children", ch);
                roots.add(node);
            }
        }
        return R.ok(roots);
    }

    @PostMapping("/menu")
    public R<Void> saveMenu(@RequestBody SysMenu body) {
        if (body.getName() == null || body.getName().isBlank()) throw new BizException("菜单名称必填");
        if (body.getId() == null) {
            if (body.getSort() == null) body.setSort(99);
            if (body.getParentId() == null) body.setParentId(0L);
            menuMapper.insert(body);
        } else {
            menuMapper.updateById(body);
        }
        return R.ok();
    }

    @GetMapping("/message/page")
    public R<Page<SysMessage>> messages(PageDTO q) {
        LambdaQueryWrapper<SysMessage> w = new LambdaQueryWrapper<>();
        w.orderByDesc(SysMessage::getCreateTime);
        return R.ok(msgMapper.selectPage(new Page<>(q.getPageNum(), q.getPageSize()), w));
    }

    @PostMapping("/message/{id}/read")
    public R<Void> read(@PathVariable Long id) {
        SysMessage m = msgMapper.selectById(id);
        if (m != null) { m.setIsRead(1); msgMapper.updateById(m); }
        return R.ok();
    }
}
