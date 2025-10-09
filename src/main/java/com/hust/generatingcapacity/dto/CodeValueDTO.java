package com.hust.generatingcapacity.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Data
@Getter
@Setter
public class CodeValueDTO {

    private double code;
    private double value;

    public CodeValueDTO(double code, double value) {
        this.code = code;
        this.value = value;
    }

    public CodeValueDTO() {
    }


}
