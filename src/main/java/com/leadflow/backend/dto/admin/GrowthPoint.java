package com.leadflow.backend.dto.admin;

import java.time.LocalDate;

public class GrowthPoint {

    private final LocalDate date;
    private final long value;

    public GrowthPoint(LocalDate date, long value) {
        this.date = date;
        this.value = value;
    }

    public LocalDate getDate() {
        return date;
    }

    public long getValue() {
        return value;
    }
}
