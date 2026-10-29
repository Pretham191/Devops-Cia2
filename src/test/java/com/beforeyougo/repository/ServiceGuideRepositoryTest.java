package com.beforeyougo.repository;

import com.beforeyougo.domain.Category;
import com.beforeyougo.domain.Institution;
import com.beforeyougo.domain.ServiceGuide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ServiceGuideRepositoryTest {

    @Autowired
    private InstitutionRepository institutions;

    @Autowired
    private ServiceGuideRepository guides;

    @BeforeEach
    void setUp() {
        Institution psk = institutions.save(new Institution("Passport Seva Kendra", Category.GOVERNMENT, "Chennai"));
        Institution bank = institutions.save(new Institution("City Bank Branch", Category.BANKING, "Madurai"));
        guides.save(new ServiceGuide(psk, "Fresh passport application"));
        guides.save(new ServiceGuide(bank, "Savings account opening"));
        guides.save(new ServiceGuide(bank, "Fixed deposit"));
    }

    private static List<String> titles(List<ServiceGuide> result) {
        return result.stream().map(ServiceGuide::getTitle).toList();
    }

    @Test
    void matchesInstitutionNameCaseInsensitively() {
        assertThat(titles(guides.search("PASSPORT seva", null))).containsExactly("Fresh passport application");
    }

    @Test
    void matchesGuideTitle() {
        assertThat(titles(guides.search("savings", null))).containsExactly("Savings account opening");
    }

    @Test
    void matchesCity() {
        assertThat(titles(guides.search("madurai", null))).containsExactly("Fixed deposit", "Savings account opening");
    }

    @Test
    void categoryFilterRestrictsResults() {
        assertThat(titles(guides.search("", Category.BANKING))).containsExactly("Fixed deposit", "Savings account opening");
        assertThat(titles(guides.search("passport", Category.BANKING))).isEmpty();
    }

    @Test
    void blankQueryReturnsEverything() {
        assertThat(guides.search(null, null)).hasSize(3);
        assertThat(guides.search("   ", null)).hasSize(3);
    }
}
