package com.jobportal.profileservice.dto;

import com.jobportal.profileservice.entity.StudentProfilePortfolioLink;

public record PortfolioLinkResponse(
        Long id,
        String label,
        String url
) {

    public static PortfolioLinkResponse fromEntity(StudentProfilePortfolioLink portfolioLink) {
        return new PortfolioLinkResponse(
                portfolioLink.getId(),
                portfolioLink.getLabel(),
                portfolioLink.getUrl()
        );
    }
}
