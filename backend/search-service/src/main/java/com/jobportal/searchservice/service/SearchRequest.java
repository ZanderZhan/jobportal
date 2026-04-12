package com.jobportal.searchservice.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public record SearchRequest(
    String title,
    String company,
    String location,
    List<String> employmentTypes,
    BigDecimal salaryMin,
    BigDecimal salaryMax,
    String salaryCurrency,
    String workMode,
    Integer postedWithinDays,
    String status,
    int page,
    int size,
    String sort
) {
    public static final String DEFAULT_SORT = "createdAt,desc";

    public SearchRequest {
        employmentTypes = employmentTypes == null ? List.of() : List.copyOf(employmentTypes);
    }

    public int offset() {
        return page * size;
    }

    public boolean usesDefaultSort() {
        return DEFAULT_SORT.equals(sort);
    }

    public String cacheKey() {
        return String.join("|",
            Optional.ofNullable(title).orElse(""),
            Optional.ofNullable(company).orElse(""),
            Optional.ofNullable(location).orElse(""),
            String.join(",", employmentTypes),
            Optional.ofNullable(salaryMin).map(BigDecimal::toPlainString).orElse(""),
            Optional.ofNullable(salaryMax).map(BigDecimal::toPlainString).orElse(""),
            Optional.ofNullable(salaryCurrency).orElse(""),
            Optional.ofNullable(workMode).orElse(""),
            Optional.ofNullable(postedWithinDays).map(String::valueOf).orElse(""),
            status,
            Integer.toString(page),
            Integer.toString(size),
            sort
        );
    }

    public String facetsCacheKey() {
        return String.join("|",
            Optional.ofNullable(title).orElse(""),
            Optional.ofNullable(company).orElse(""),
            Optional.ofNullable(location).orElse(""),
            String.join(",", employmentTypes),
            Optional.ofNullable(salaryMin).map(BigDecimal::toPlainString).orElse(""),
            Optional.ofNullable(salaryMax).map(BigDecimal::toPlainString).orElse(""),
            Optional.ofNullable(salaryCurrency).orElse(""),
            Optional.ofNullable(workMode).orElse(""),
            Optional.ofNullable(postedWithinDays).map(String::valueOf).orElse(""),
            status
        );
    }

    public String primaryEmploymentType() {
        return employmentTypes.size() == 1 ? employmentTypes.get(0) : null;
    }

    public boolean hasEmploymentTypeFilter() {
        return !employmentTypes.isEmpty();
    }

    public LocalDateTime postedAfter() {
        if (postedWithinDays == null) {
            return null;
        }
        return LocalDateTime.now().minusDays(postedWithinDays);
    }

    public SearchRequest withPageAndSize(int page, int size) {
        return new SearchRequest(
            title,
            company,
            location,
            employmentTypes,
            salaryMin,
            salaryMax,
            salaryCurrency,
            workMode,
            postedWithinDays,
            status,
            page,
            size,
            sort
        );
    }
}
