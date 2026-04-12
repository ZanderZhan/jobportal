package com.jobportal.searchservice.dto;

public class AutocompleteSuggestion {

    private String value;
    private String type;
    private long count;

    public AutocompleteSuggestion() {
    }

    public AutocompleteSuggestion(String value, String type, long count) {
        this.value = value;
        this.type = type;
        this.count = count;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
