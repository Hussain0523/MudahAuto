package com.mudah.auto.cars.service.dto;

import java.util.List;

public class Filter {
    private String comparator;
    private Field field;
    private Object value;

    public Filter(String comparator, Field field, Object value) {
        this.comparator = comparator;
        this.field = field;
        this.value = value;
    }

    public String getComparator() {
        return comparator;
    }

    public void setComparator(String comparator) {
        this.comparator = comparator;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}