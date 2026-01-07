package com.beu.result.AcademicAnalytics.controller;

import com.beu.result.AcademicAnalytics.entity.ResultLink;
import com.beu.result.AcademicAnalytics.repository.ResultLinkRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // Import for flash messages

@Controller
@RequestMapping("/admin/links")
public class LinkManagerController {

    private final ResultLinkRepository repository;

    public LinkManagerController(ResultLinkRepository repository) {
        this.repository = repository;
    }

    // 1. Show the Management Page
    @GetMapping
    public String showLinkManager(Model model) {
        model.addAttribute("links", repository.findAll());
        model.addAttribute("newLink", new ResultLink());
        return "admin/link-manager";
    }

    // 2. Add a New Link (Enhanced with Feedback)
    @PostMapping("/save")
    public String saveLink(@ModelAttribute ResultLink link, RedirectAttributes redirectAttributes) {
        try {
            if (link.getId() == null) {
                // New Link
                repository.save(link);
            } else {
                // Update Existing
                ResultLink existing = repository.findById(link.getId()).orElse(new ResultLink());
                existing.setLinkKey(link.getLinkKey());
                existing.setUrlTemplate(link.getUrlTemplate());
                existing.setDescription(link.getDescription());
                existing.setActive(link.isActive());
                repository.save(existing);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Link configuration saved successfully!");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: Link Key '" + link.getLinkKey() + "' already exists.");
        }
        return "redirect:/admin/links";
    }

    // 3. Toggle Active/Inactive
    @GetMapping("/toggle/{id}")
    public String toggleStatus(@PathVariable Long id) {
        repository.findById(id).ifPresent(link -> {
            link.setActive(!link.isActive());
            repository.save(link);
        });
        return "redirect:/admin/links";
    }

    // 4. Delete Link (Enhanced with Feedback)
    @GetMapping("/delete/{id}")
    public String deleteLink(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        repository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Link deleted successfully.");
        return "redirect:/admin/links";
    }
}