package com.hust.generatingcapacity.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "ddh_hystation_generation_cal_basin_out")
@Comment("单个时段流域发电能力计算结果")
public class GenerationCalBasinOut {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "time", columnDefinition = "datetime COMMENT '计算时间'")
    private Date time;
    @Column(name = "basin", columnDefinition = "TEXT COMMENT '流域'")
    private String basin;
    // 关联子表，单个流域所以电站发电能力计算结果，一对多关系
    @OneToMany(mappedBy = "generationCalBasinOut", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<GenerationCalStationOut> generationCalStationOuts;
    // 关联父表，发电能力计算方案
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ddh_hystation_generation_cal_scheme_id")
    @JsonBackReference
    private GenerationCalScheme generationCalScheme;
}
