package com.jobportal.searchservice.dto;

import java.util.ArrayList;
import java.util.List;

public class JobAutocompleteResponse {

    private List<AutocompleteSuggestion> suggestions = new ArrayList<>();

    public List<AutocompleteSuggestion> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<AutocompleteSuggestion> suggestions) {
        this.suggestions = suggestions;
    }
}
