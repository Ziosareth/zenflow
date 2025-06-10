package it.zenflow.controller.admin;

import it.zenflow.model.rbac.Role;
import it.zenflow.service.rbac.PermissionService;
import it.zenflow.service.rbac.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/roles")
@PreAuthorize("hasAuthority('READ_ROLE')")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final PermissionService permissionService;
    private final MessageSource messageSource;

    @GetMapping("")
    public String listRoles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        Page<Role> rolePage = roleService.findAll(pageable);

        model.addAttribute("roles", rolePage.getContent());
        model.addAttribute("currentPage", rolePage.getNumber());
        model.addAttribute("totalPages", rolePage.getTotalPages());
        model.addAttribute("totalItems", rolePage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("sortField", sort);

        return "admin/roles";
    }

    @GetMapping("/{id}")
    public String viewRole(@PathVariable Long id, Model model) {
        return roleService.findById(id)
                .map(role -> {
                    model.addAttribute("role", role);
                    return "admin/role-detail";
                })
                .orElse("redirect:/admin/roles");
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('UPDATE_ROLE')")
    public String newRoleForm(Model model) {
        model.addAttribute("role", new Role());
        model.addAttribute("allPermissions", permissionService.findAll());
        return "admin/role-edit";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('UPDATE_ROLE')")
    public String editRoleForm(@PathVariable Long id, Model model) {
        return roleService.findById(id)
                .map(role -> {
                    model.addAttribute("role", role);
                    model.addAttribute("allPermissions", permissionService.findAll());
                    return "admin/role-edit";
                })
                .orElse("redirect:/admin/roles");
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('UPDATE_ROLE')")
    public String createRole(@ModelAttribute Role role, RedirectAttributes redirectAttributes) {
        Role savedRole = roleService.save(role);
        String message = messageSource.getMessage("admin.roles.created", null, LocaleContextHolder.getLocale());
        redirectAttributes.addFlashAttribute("message", message);
        return "redirect:/admin/roles/" + savedRole.getId();
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAuthority('UPDATE_ROLE')")
    public String updateRole(@PathVariable Long id, @ModelAttribute Role role, RedirectAttributes redirectAttributes) {
        return roleService.findById(id)
                .map(existingRole -> {
                    existingRole.setName(role.getName());
                    existingRole.setPermissions(role.getPermissions());

                    roleService.save(existingRole);
                    String message = messageSource.getMessage("admin.roles.updated", null, LocaleContextHolder.getLocale());
                    redirectAttributes.addFlashAttribute("message", message);
                    return "redirect:/admin/roles/" + id;
                })
                .orElse("redirect:/admin/roles");
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('UPDATE_ROLE')")
    public String deleteRole(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        roleService.deleteById(id);
        String message = messageSource.getMessage("admin.roles.deleted", null, LocaleContextHolder.getLocale());
        redirectAttributes.addFlashAttribute("message", message);
        return "redirect:/admin/roles";
    }
}
