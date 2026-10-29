package com.beforeyougo.repository;

import com.beforeyougo.domain.Category;
import com.beforeyougo.domain.ServiceGuide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Locale;

public interface ServiceGuideRepository extends JpaRepository<ServiceGuide, Long> {

    @Query("""
            select g from ServiceGuide g join fetch g.institution i
            where lower(i.name) like :pattern or lower(g.title) like :pattern or lower(i.city) like :pattern
            order by g.title""")
    List<ServiceGuide> searchByPattern(@Param("pattern") String pattern);

    @Query("""
            select g from ServiceGuide g join fetch g.institution i
            where i.category = :category
              and (lower(i.name) like :pattern or lower(g.title) like :pattern or lower(i.city) like :pattern)
            order by g.title""")
    List<ServiceGuide> searchByPatternInCategory(@Param("pattern") String pattern, @Param("category") Category category);

    @Query("select g from ServiceGuide g join fetch g.institution i order by i.name, g.title")
    List<ServiceGuide> findAllWithInstitution();

    /**
     * Case-insensitive search over institution name, guide title and city, optionally
     * restricted to one category. A blank query matches everything.
     */
    default List<ServiceGuide> search(String query, Category category) {
        String pattern = "%" + (query == null ? "" : query.trim().toLowerCase(Locale.ROOT)) + "%";
        return category == null ? searchByPattern(pattern) : searchByPatternInCategory(pattern, category);
    }
}
