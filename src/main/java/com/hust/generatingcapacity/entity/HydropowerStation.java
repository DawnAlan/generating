package com.hust.generatingcapacity.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.util.List;

@Data
@Entity
@Table(name = "ddh_hystation")
@Comment("水电站信息")
public class HydropowerStation {// 水电站信息
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "station_name", unique = true, columnDefinition = "VARCHAR(255) COMMENT '电站名称'")
    private String stationName;
    @Column(name = "station_code", columnDefinition = "DOUBLE COMMENT '电站代码'")
    private Double stationCode;
    @Column(name = "basin", columnDefinition = "TEXT COMMENT '流域'")
    private String basin;
    @Column(name = "longitude", columnDefinition = "DOUBLE COMMENT '经度'")
    private Double longitude;
    @Column(name = "latitude", columnDefinition = "DOUBLE COMMENT '纬度'")
    private Double latitude;
    @Column(name = "status", columnDefinition = "INT COMMENT '建设状态（1-投产、2-在建、3-拟建）'")
    private Integer status;
    @Column(name = "installed_capacity", columnDefinition = "DOUBLE COMMENT '装机容量（MW）'")
    private Double installedCapacity;
    @Column(name = "regulation_performance", columnDefinition = "TEXT COMMENT '调节性能(无、日调节、季调节、年调节)'")
    private String regulationPerformance;
    @Column(name = "transmission_section", columnDefinition = "TEXT COMMENT '输电断面'")
    private String transmissionSection;
    @Column(name = "upper_station", columnDefinition = "TEXT COMMENT '上游电站'")
    private String upperStation;
    @Column(name = "lower_station", columnDefinition = "TEXT COMMENT '下游电站'")
    private String lowerStation;
    @Column(name = "is_under_ddh", columnDefinition = "bit(1) COMMENT '是否归大渡河公司管辖'")
    private Boolean isUnderDdh;
    @Column(name = "is_stay_sichuan", columnDefinition = "bit(1) COMMENT '是否留川'")
    private Boolean isStaySichuan;
    @Column(name = "is_participate_market", columnDefinition = "bit(1) COMMENT '是否参与电力市场'")
    private Boolean isParticipateMarket;

    // 关联子表，水库特征信息
    @OneToOne(mappedBy = "hydropowerStation", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private ReservoirInf reservoirInf;
    // 关联子表，工程特征信息
    @OneToMany(mappedBy = "hydropowerStation", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<UnitInf> unitInfs;
    // 关联子表，尾水位流量信息
    @OneToMany(mappedBy = "hydropowerStation", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<TailWaterLevelFlow> tailWaterLevelFlows;
    // 关联子表，水位耗水率信息
    @OneToMany(mappedBy = "hydropowerStation", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<WaterLevelConsumption> waterLevelDischarges;
    // 关联子表，电站发电约束
    @OneToMany(mappedBy = "hydropowerStation", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<DispatchConstraint> dispatchConstraints;


}
