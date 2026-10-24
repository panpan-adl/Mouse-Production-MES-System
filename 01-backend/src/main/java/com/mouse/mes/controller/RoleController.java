package com.mouse.mes.controller;

import com.mouse.mes.entity.Role;
import com.mouse.mes.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping
    public List<Role> getAllRoles() {
        return roleService.findAll();
    }

    @GetMapping("/{id}")
    public Role getRoleById(@PathVariable Integer id) {
        return roleService.findById(id);
    }

    @GetMapping("/rolecode/{roleCode}")
    public Role getRoleByRoleCode(@PathVariable String roleCode) {
        return roleService.findByRoleCode(roleCode);
    }

    @PostMapping
    public Role createRole(@RequestBody Role role) {
        return roleService.create(role);
    }

    @PutMapping("/{id}")
    public Role updateRole(@PathVariable Integer id, @RequestBody Role role) {
        role.setId(id);
        return roleService.update(role);
    }

    @DeleteMapping("/{id}")
    public void deleteRole(@PathVariable Integer id) {
        roleService.delete(id);
    }
}