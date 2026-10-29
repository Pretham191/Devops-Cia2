package com.beforeyougo.web;

import com.beforeyougo.domain.Contribution;
import com.beforeyougo.domain.ContributionStatus;
import com.beforeyougo.repository.ContributionRepository;
import com.beforeyougo.repository.ServiceGuideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Full application context on H2 with seed data; exercises controllers, templates and security. */
@SpringBootTest(properties = "app.version=test-build")
@AutoConfigureMockMvc
class WebIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ServiceGuideRepository guides;

    @Autowired
    private ContributionRepository contributions;

    private Long guideId;

    @BeforeEach
    void pickSeededGuide() {
        guideId = guides.findAll().getFirst().getId();
    }

    @Test
    void homePageLoadsWithSampleNoticeAndBuildFooter() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("Sample data for demonstration — not official guidance.")))
                .andExpect(content().string(containsString("BeforeYouGo · build test-build")));
    }

    @Test
    void homeSearchFiltersResults() throws Exception {
        mvc.perform(get("/").param("q", "passport").param("category", "GOVERNMENT"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Fresh passport application")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Opening a savings account"))));
    }

    @Test
    void guidePageLoads() throws Exception {
        mvc.perform(get("/guides/{id}", guideId))
                .andExpect(status().isOk())
                .andExpect(view().name("guide"))
                .andExpect(content().string(containsString("Visitor experiences")))
                .andExpect(content().string(containsString("not official requirements")));
    }

    @Test
    void unknownGuideReturns404Page() throws Exception {
        mvc.perform(get("/guides/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    void validContributionRedirectsAndIsSavedPending() throws Exception {
        String text = "They also asked for a photocopy of my ID card " + System.nanoTime();

        mvc.perform(post("/guides/{id}/contributions", guideId).with(csrf())
                        .param("type", "TIP")
                        .param("text", text)
                        .param("visitDate", LocalDate.now().minusDays(1).toString())
                        .param("displayName", "Tester"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/guides/" + guideId))
                .andExpect(flash().attribute("message", PublicController.SUBMITTED_MESSAGE));

        Contribution saved = contributions.findAll().stream()
                .filter(c -> c.getText().equals(text))
                .max(Comparator.comparing(Contribution::getId))
                .orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ContributionStatus.PENDING);
        assertThat(saved.getGuide().getId()).isEqualTo(guideId);
    }

    @Test
    void invalidContributionShowsErrorsAndSavesNothing() throws Exception {
        long before = contributions.count();

        mvc.perform(post("/guides/{id}/contributions", guideId).with(csrf())
                        .param("text", "short")
                        .param("visitDate", LocalDate.now().plusDays(30).toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("guide"))
                .andExpect(model().attributeHasFieldErrors("contributionForm", "type", "text", "visitDate"));

        assertThat(contributions.count()).isEqualTo(before);
    }

    @Test
    void contributionWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/guides/{id}/contributions", guideId)
                        .param("type", "TIP")
                        .param("text", "A perfectly valid tip text")
                        .param("visitDate", LocalDate.now().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRequiresLogin() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", endsWith("/login")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminDashboardLoadsForAdmin() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(content().string(containsString("Moderation queue")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminGuideFormLoads() throws Exception {
        mvc.perform(get("/admin/guides/{id}/edit", guideId))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/guide-form"));
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void healthIsPublicAndUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void livenessAndReadinessProbesArePublic() throws Exception {
        mvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
