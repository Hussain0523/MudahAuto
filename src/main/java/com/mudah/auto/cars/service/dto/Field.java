package com.mudah.auto.cars.service.dto;

public class Field {
    private String api_name;
    private String id;

    public Field(String api_name, String id) {
        this.api_name = api_name;
        this.id = id;
    }

    public String getApi_name() {
        return api_name;
    }

    public void setApi_name(String api_name) {
        this.api_name = api_name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}