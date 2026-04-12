package com.jobportal.searchservice.dto;

import java.util.ArrayList;
import java.util.List;

public class JobSearchFacetsResponse {

    private List<FacetValueCount> locations = new ArrayList<>();
    private List<FacetValueCount> companies = new ArrayList<>();
    private List<FacetValueCount> employmentTypes = new ArrayList<>();

    public List<FacetValueCount> getLocations() {
        return locations;
    }

    public void setLocations(List<FacetValueCount> locations) {
        this.locations = locations;
    }

    public List<FacetValueCount> getCompanies() {
        return companies;
    }

    public void setCompanies(List<FacetValueCount> companies) {
        this.companies = companies;
    }

    public List<FacetValueCount> getEmploymentTypes() {
        return employmentTypes;
    }

    public void setEmploymentTypes(List<FacetValueCount> employmentTypes) {
        this.employmentTypes = employmentTypes;
    }
}
