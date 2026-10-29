package com.beforeyougo.repository;

import com.beforeyougo.domain.Institution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    List<Institution> findAllByOrderByNameAsc();
}
