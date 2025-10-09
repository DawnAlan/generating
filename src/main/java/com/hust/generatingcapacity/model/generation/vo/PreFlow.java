package com.hust.generatingcapacity.model.generation.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Data
@Getter
@Setter
public class PreFlow {

    private Date time;

    private Double inFlow;

    public PreFlow() {
    }

    public PreFlow(Date time, Double inFlow) {
        this.time = time;
        this.inFlow = inFlow;
    }
}
