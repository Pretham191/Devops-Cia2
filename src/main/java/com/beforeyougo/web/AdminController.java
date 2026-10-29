package com.beforeyougo.web;

import com.beforeyougo.domain.Category;
import com.beforeyougo.domain.Institution;
import com.beforeyougo.domain.ServiceGuide;
import com.beforeyougo.repository.InstitutionRepository;
import com.beforeyougo.service.ContributionService;
import com.beforeyougo.service.GuideService;
import com.beforeyougo.web.form.GuideForm;
import com.beforeyougo.web.form.InstitutionForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.NoSuchElementException;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final GuideService guideService;
    private final ContributionService contributionService;
    private final InstitutionRepository institutions;

    public AdminController(GuideService guideService, ContributionService contributionService, InstitutionRepository institutions) {
        this.guideService = guideService;
        this.contributionService = contributionService;
        this.institutions = institutions;
    }

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        model.addAttribute("pending", contributionService.pending());
        model.addAttribute("needsReview", guideService.needsReview());
        model.addAttribute("guideCount", guideService.findAll().size());
        model.addAttribute("institutionCount", institutions.count());
        model.addAttribute("pendingCount", contributionService.countPending());
        return "admin/dashboard";
    }

    // ---- Moderation ----

    @PostMapping("/contributions/{id}/approve")
    public String approve(@PathVariable("id") Long id, RedirectAttributes redirect) {
        return moderate(() -> contributionService.approve(id), "Contribution approved.", redirect);
    }

    @PostMapping("/contributions/{id}/reject")
    public String reject(@PathVariable("id") Long id, RedirectAttributes redirect) {
        return moderate(() -> contributionService.reject(id), "Contribution rejected.", redirect);
    }

    private String moderate(Runnable action, String success, RedirectAttributes redirect) {
        try {
            action.run();
            redirect.addFlashAttribute("message", success);
        } catch (NoSuchElementException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }

    // ---- Guides ----

    @GetMapping("/guides")
    public String guides(Model model) {
        model.addAttribute("guides", guideService.findAll());
        return "admin/guides";
    }

    @GetMapping("/guides/new")
    public String newGuide(Model model) {
        return guideForm(model, new GuideForm(), null);
    }

    @PostMapping("/guides")
    public String createGuide(@Valid @ModelAttribute("guideForm") GuideForm form, BindingResult errors,
                              Model model, RedirectAttributes redirect) {
        checkGuideForm(form, errors);
        if (errors.hasErrors()) {
            return guideForm(model, form, null);
        }
        ServiceGuide guide = guideService.create(form);
        redirect.addFlashAttribute("message", "Guide created.");
        return "redirect:/guides/" + guide.getId();
    }

    @GetMapping("/guides/{id}/edit")
    public String editGuide(@PathVariable("id") Long id, Model model) {
        return guideForm(model, guideService.toForm(id), id);
    }

    @PostMapping("/guides/{id}")
    public String updateGuide(@PathVariable("id") Long id, @Valid @ModelAttribute("guideForm") GuideForm form,
                              BindingResult errors, Model model, RedirectAttributes redirect) {
        checkGuideForm(form, errors);
        if (errors.hasErrors()) {
            return guideForm(model, form, id);
        }
        guideService.update(id, form);
        redirect.addFlashAttribute("message", "Guide updated.");
        return "redirect:/guides/" + id;
    }

    @PostMapping("/guides/{id}/verify")
    public String markVerified(@PathVariable("id") Long id, RedirectAttributes redirect) {
        ServiceGuide guide = guideService.markVerified(id);
        redirect.addFlashAttribute("message", "\"" + guide.getTitle() + "\" marked verified today.");
        return "redirect:/admin";
    }

    private String guideForm(Model model, GuideForm form, Long guideId) {
        model.addAttribute("guideForm", form);
        model.addAttribute("guideId", guideId);
        model.addAttribute("institutions", institutions.findAllByOrderByNameAsc());
        return "admin/guide-form";
    }

    private void checkGuideForm(GuideForm form, BindingResult errors) {
        if (form.getInstitutionId() != null && !institutions.existsById(form.getInstitutionId())) {
            errors.rejectValue("institutionId", "unknown", "Choose an existing institution");
        }
        String tooLong = "Each line can be at most " + ServiceGuide.MAX_ITEM_LENGTH + " characters";
        if (GuideService.hasLineLongerThan(form.getDocumentsText(), ServiceGuide.MAX_ITEM_LENGTH)) {
            errors.rejectValue("documentsText", "tooLong", tooLong);
        }
        if (GuideService.hasLineLongerThan(form.getPrerequisitesText(), ServiceGuide.MAX_ITEM_LENGTH)) {
            errors.rejectValue("prerequisitesText", "tooLong", tooLong);
        }
        if (GuideService.hasLineLongerThan(form.getStepsText(), ServiceGuide.MAX_ITEM_LENGTH)) {
            errors.rejectValue("stepsText", "tooLong", tooLong);
        }
        if (GuideService.hasLineLongerThan(form.getCommonMistakesText(), ServiceGuide.MAX_ITEM_LENGTH)) {
            errors.rejectValue("commonMistakesText", "tooLong", tooLong);
        }
    }

    // ---- Institutions ----

    @GetMapping("/institutions/new")
    public String newInstitution(Model model) {
        model.addAttribute("institutionForm", new InstitutionForm());
        model.addAttribute("categories", Category.values());
        return "admin/institution-form";
    }

    @PostMapping("/institutions")
    public String createInstitution(@Valid @ModelAttribute("institutionForm") InstitutionForm form, BindingResult errors,
                                    Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            model.addAttribute("categories", Category.values());
            return "admin/institution-form";
        }
        Institution institution = new Institution(form.getName().trim(), form.getCategory(), trimToNull(form.getCity()));
        institution.setAddress(trimToNull(form.getAddress()));
        institution.setContact(trimToNull(form.getContact()));
        institution.setDescription(trimToNull(form.getDescription()));
        institutions.save(institution);
        redirect.addFlashAttribute("message", "Institution \"" + institution.getName() + "\" created. You can now add guides for it.");
        return "redirect:/admin/guides/new";
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
