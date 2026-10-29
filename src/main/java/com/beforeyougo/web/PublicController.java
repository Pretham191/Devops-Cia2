package com.beforeyougo.web;

import com.beforeyougo.domain.Category;
import com.beforeyougo.domain.ContributionType;
import com.beforeyougo.domain.ServiceGuide;
import com.beforeyougo.service.ContributionService;
import com.beforeyougo.service.FreshnessCalculator;
import com.beforeyougo.service.GuideService;
import com.beforeyougo.web.form.ContributionForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.Locale;

@Controller
public class PublicController {

    static final String SUBMITTED_MESSAGE = "Thanks — your submission will appear after review.";

    private final GuideService guideService;
    private final ContributionService contributionService;
    private final FreshnessCalculator freshness;

    public PublicController(GuideService guideService, ContributionService contributionService, FreshnessCalculator freshness) {
        this.guideService = guideService;
        this.contributionService = contributionService;
        this.freshness = freshness;
    }

    @GetMapping("/")
    public String home(@RequestParam(name = "q", required = false) String q,
                       @RequestParam(name = "category", required = false) String category,
                       Model model) {
        Category selected = parseCategory(category);
        model.addAttribute("q", q == null ? "" : q.trim());
        model.addAttribute("selectedCategory", selected);
        model.addAttribute("categories", Category.values());
        model.addAttribute("guides", guideService.search(q, selected));
        return "index";
    }

    @GetMapping("/guides/{id}")
    public String guide(@PathVariable("id") Long id, Model model) {
        populateGuide(id, model);
        model.addAttribute("contributionForm", new ContributionForm());
        return "guide";
    }

    @PostMapping("/guides/{id}/contributions")
    public String contribute(@PathVariable("id") Long id,
                             @Valid @ModelAttribute("contributionForm") ContributionForm form,
                             BindingResult errors,
                             Model model,
                             RedirectAttributes redirect) {
        if (form.getVisitDate() != null && form.getVisitDate().isAfter(freshness.today())) {
            errors.rejectValue("visitDate", "future", "Visit date can't be in the future");
        }
        if (errors.hasErrors()) {
            populateGuide(id, model);
            return "guide";
        }
        contributionService.submit(id, form);
        redirect.addFlashAttribute("message", SUBMITTED_MESSAGE);
        return "redirect:/guides/" + id;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    private void populateGuide(Long id, Model model) {
        ServiceGuide guide = guideService.getDetail(id);
        model.addAttribute("guide", guide);
        model.addAttribute("badge", freshness.badgeFor(guide.getLastVerifiedAt()));
        model.addAttribute("experiences", contributionService.approvedFor(id));
        model.addAttribute("contributionTypes", ContributionType.values());
        model.addAttribute("today", freshness.today());
    }

    private static Category parseCategory(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String upper = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(Category.values()).filter(c -> c.name().equals(upper)).findFirst().orElse(null);
    }
}
