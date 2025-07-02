package it.zenflow.master.controller;

import it.zenflow.master.dto.CreateTenantDTO;
import it.zenflow.master.dto.UpdateTenantDTO;
import it.zenflow.master.service.MasterTenantService;
import it.zenflow.master.model.Tenant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/master/tenants")
@RequiredArgsConstructor
public class MasterTenantController {

    private final MasterTenantService tenantService;

    @GetMapping("")
    public String listTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sort,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        Page<Tenant> tenantPage = tenantService.findAll(pageable);

        model.addAttribute("tenants", tenantPage.getContent());
        model.addAttribute("currentPage", tenantPage.getNumber());
        model.addAttribute("totalPages", tenantPage.getTotalPages());
        model.addAttribute("totalItems", tenantPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("sortField", sort);

        return "master/tenants/list";
    }

    @GetMapping("/{name}")
    public String viewTenant(@PathVariable String name, Model model) {
        return tenantService.findByName(name)
                .map(tenant -> {
                    model.addAttribute("tenant", tenant);
                    return "master/tenants/detail";
                })
                .orElse("redirect:/master/tenants");
    }

    @GetMapping("/new")
    public String newTenantForm(Model model) {
        model.addAttribute("tenant", new CreateTenantDTO());
        return "master/tenants/new";
    }

    @PostMapping("")
    public String createTenant(@Valid @ModelAttribute("tenant") CreateTenantDTO dto, 
                              BindingResult result, 
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "master/tenants/new";
        }

        try {
            Tenant tenant = tenantService.createTenant(dto);
            redirectAttributes.addFlashAttribute("message", "Tenant creato con successo");
            return "redirect:/master/tenants/" + tenant.getName();
        } catch (Exception e) {
            result.rejectValue("name", "error.tenant", e.getMessage());
            return "master/tenants/new";
        }
    }

    @GetMapping("/{name}/edit")
    public String editTenantForm(@PathVariable String name, Model model) {
        return tenantService.findByName(name)
                .map(tenant -> {
                    UpdateTenantDTO dto = new UpdateTenantDTO();
                    dto.setUrl(tenant.getUrl());
                    dto.setUsername(tenant.getUsername());
                    dto.setDriver(tenant.getDriver());
                    dto.setEnabled(tenant.isEnabled());
                    
                    model.addAttribute("tenant", dto);
                    model.addAttribute("tenantName", name);
                    return "master/tenants/edit";
                })
                .orElse("redirect:/master/tenants");
    }

    @PostMapping("/{name}")
    public String updateTenant(@PathVariable String name, 
                              @Valid @ModelAttribute("tenant") UpdateTenantDTO dto, 
                              BindingResult result, 
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "master/tenants/edit";
        }

        try {
            tenantService.updateTenant(name, dto);
            redirectAttributes.addFlashAttribute("message", "Tenant aggiornato con successo");
            return "redirect:/master/tenants/" + name;
        } catch (Exception e) {
            result.rejectValue("url", "error.tenant", e.getMessage());
            return "master/tenants/edit";
        }
    }

    @PostMapping("/{name}/initialize")
    public String initializeTenant(@PathVariable String name, RedirectAttributes redirectAttributes) {
        try {
            tenantService.initializeTenantDatabase(name);
            redirectAttributes.addFlashAttribute("message", "Database del tenant inizializzato con successo");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/master/tenants/" + name;
    }

    @PostMapping("/{name}/toggle-status")
    public String toggleTenantStatus(@PathVariable String name, RedirectAttributes redirectAttributes) {
        return tenantService.findByName(name)
                .map(tenant -> {
                    tenant.setEnabled(!tenant.isEnabled());
                    tenantService.save(tenant);
                    String message = tenant.isEnabled() ? "Tenant abilitato con successo" : "Tenant disabilitato con successo";
                    redirectAttributes.addFlashAttribute("message", message);
                    return "redirect:/master/tenants/" + name;
                })
                .orElse("redirect:/master/tenants");
    }
}