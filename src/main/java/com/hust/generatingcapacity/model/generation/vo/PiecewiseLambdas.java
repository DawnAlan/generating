package com.hust.generatingcapacity.model.generation.vo;

import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 保存分段线性化用到的一套 λ 和段选择 z，以及该套网格 H。
 */
public class PiecewiseLambdas {
    public List<Variable> lambdas;   // λ[i]，i=0..n-1
    public List<Variable> segZ;      // z[s]，s=0..n-2（二进制，选择段）
    public double[] Hgrid;           // 网格 H[i]（递增）

    public PiecewiseLambdas(List<Variable> lambdas, List<Variable> segZ, double[] h) {
        this.lambdas = lambdas;
        this.segZ = segZ;
        this.Hgrid = h;
    }

    public PiecewiseLambdas() {
    }

    public int size() {
        return lambdas.size();
    }
}
