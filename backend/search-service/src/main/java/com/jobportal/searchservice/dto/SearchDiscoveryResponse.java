package com.jobportal.searchservice.dto;

import java.util.ArrayList;
import java.util.List;

public class SearchDiscoveryResponse {

    private List<String> relatedSearches = new ArrayList<>();
    private List<FacetValueCount> suggestedLocations = new ArrayList<>();
    private List<FacetValueCount> suggestedCompanies = new ArrayList<>();
    private List<FacetValueCount> suggestedEmploymentTypes = new ArrayList<>();

    public List<String> getRelatedSearches() {
        return relatedSearches;
    }

    public void setRelatedSearches(List<String> relatedSearches) {
        this.relatedSearches = relatedSearches;
    }

    public List<FacetValueCount> getSuggestedLocations() {
        return suggestedLocations;
    }

    public void setSuggestedLocations(List<FacetValueCount> suggestedLocations) {
        this.suggestedLocations = suggestedLocations;
    }

    public List<FacetValueCount> getSuggestedCompanies() {
        return suggestedCompanies;
    }

    public void setSuggestedCompanies(List<FacetValueCount> suggestedCompanies) {
        this.suggestedCompanies = suggestedCompanies;
    }

    public List<FacetValueCount> getSuggestedEmploymentTypes() {
        return suggestedEmploymentTypes;
    }

    public void setSuggestedEmploymentTypes(List<FacetValueCount> suggestedEmploymentTypes) {
        this.suggestedEmploymentTypes = suggestedEmploymentTypes;
    }
}
