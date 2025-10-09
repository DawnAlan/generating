package com.hust.generatingcapacity.model.generation.dispatch;


import java.util.HashMap;
import java.util.Map;

public class ConstraintEnvBuilder {

    private final Map<String, Object> env = new HashMap<>();

    public Map<String, Object> conditionBuild(double t, double h, double l, double dl, double qin) {
        env.put("T", t);
        env.put("H", h);
        env.put("L", l);
        env.put("dL", dl);
        env.put("Qin", qin);
        return env;
    }

    public Map<String, Object> paramBuild(double h, double dh, double qp, double qo, double c,double gen) {
        env.put("H", h);
        env.put("dH", dh);
        env.put("Qp", qp);
        env.put("Qo", qo);
        env.put("C", c);
        env.put("P",gen);
        return env;
    }

}

