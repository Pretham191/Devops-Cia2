package com.beforeyougo.repository;

import com.beforeyougo.domain.Contribution;
import com.beforeyougo.domain.ContributionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContributionRepository extends JpaRepository<Contribution, Long> {

    List<Contribution> findByGuide_IdAndStatusOrderByVisitDateDescCreatedAtDesc(Long guideId, ContributionStatus status);

    List<Contribution> findByStatusOrderByCreatedAtAsc(ContributionStatus status);

    long countByStatus(ContributionStatus status);
}
