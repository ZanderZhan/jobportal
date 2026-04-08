package com.jobportal.searchservice.dto;

public class FacetValueCount {

    private String value;
    private long count;

    public FacetValueCount() {
    }

    public FacetValueCount(String value, long count) {
        this.value = value;
        this.count = count;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
