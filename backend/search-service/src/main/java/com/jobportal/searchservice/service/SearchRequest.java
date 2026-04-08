package com.jobportal.searchservice.service;

import java.math.BigDecimal;
import java.util.Optional;

public record SearchRequest(
    String title,
    String company,
    String location,
    String employmentType,
    BigDecimal salaryMin,
    BigDecimal salaryMax,
    String status,
    int page,
    int size,
    String sort
) {
    public static final String DEFAULT_SORT = "createdAt,desc";

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
            Optional.ofNullable(employmentType).orElse(""),
            Optional.ofNullable(salaryMin).map(BigDecimal::toPlainString).orElse(""),
            Optional.ofNullable(salaryMax).map(BigDecimal::toPlainString).orElse(""),
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
            Optional.ofNullable(employmentType).orElse(""),
            Optional.ofNullable(salaryMin).map(BigDecimal::toPlainString).orElse(""),
            Optional.ofNullable(salaryMax).map(BigDecimal::toPlainString).orElse(""),
            status
        );
    }

    public SearchRequest withPageAndSize(int page, int size) {
        return new SearchRequest(
            title,
            company,
            location,
            employmentType,
            salaryMin,
            salaryMax,
            status,
            page,
            size,
            sort
        );
    }
}
