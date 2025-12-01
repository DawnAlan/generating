package com.hust.generatingcapacity.model.generation.vo;

import com.hust.generatingcapacity.model.generation.type.ParamBoundType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class BoundPair {

    public ParamBoundType min;
    public double minVal;
    public ParamBoundType max;
    public double maxVal;

    public BoundPair(ParamBoundType min, double minVal, ParamBoundType max, double maxVal) {
        this.min = min;
        this.minVal = minVal;
        this.max = max;
        this.maxVal = maxVal;
    }

    public BoundPair() {
    }

    @Override
    public String toString() {
        return Math.round(minVal * 100.0) / 100.0 + "<= " + this.max.variable() + " <=" + Math.round(maxVal * 100.0) / 100.0;
    }

    public String toParamMinString(int period) {
        if (this.min.variable().equals("P")) {
            return this.max.variable() + ">= " + Math.round(minVal * period / 3600.0 * 100.0) / 100.0;
        }else {
            return this.max.variable() + ">= " + Math.round(minVal * 100.0) / 100.0;
        }

    }

    public String toParamMaxString(int period) {
        if (this.max.variable().equals("P")) {
            return this.max.variable() + " <=" + Math.round(maxVal * period / 3600.0 * 100.0) / 100.0;
        }else {
            return this.max.variable() + " <=" + Math.round(maxVal * 100.0) / 100.0;
        }
    }
}



