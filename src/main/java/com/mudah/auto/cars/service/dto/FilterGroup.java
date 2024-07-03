package com.mudah.auto.cars.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class FilterGroup {

    private String groupOperator;
    private List<Filter> group;

    @JsonProperty("group_operator")
    public String getGroupOperator() {
        return groupOperator;
    }

    public void setGroupOperator(String groupOperator) {
        this.groupOperator = groupOperator;
    }

    @JsonProperty("group")
    public List<Filter> getGroup() {
        return group;
    }

    public void setGroup(List<Filter> group) {
        this.group = group;
    }
}
