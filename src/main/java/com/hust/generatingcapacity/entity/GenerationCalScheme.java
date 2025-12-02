package com.hust.generatingcapacity.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "ddh_hystation_generation_cal_scheme")
@Comment("发电能力计算方案")
public class GenerationCalScheme {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "creat_time", columnDefinition = "datetime COMMENT '约束类型'")
    private Date creatTime = new Date();
    @Column(name = "description", columnDefinition = "TEXT COMMENT '描述'")
    private String description;
    @Column(name = "scheme_name", nullable = false, unique = true, columnDefinition = "TEXT COMMENT '方案名称'")
    private String schemeName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "start_date", columnDefinition = "datetime COMMENT '开始计算时间'")
    private Date startDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "end_date", columnDefinition = "datetime COMMENT '结束计算时间'")
    private Date endDate;
    @Column(name = "period", columnDefinition = "TEXT COMMENT '方案尺度'")
    private String period;
    @Column(name = "scheme_l", columnDefinition = "INT COMMENT '方案调度期长度'")
    private Integer schemeL;
    @Column(name = "basin", columnDefinition = "TEXT COMMENT '流域'")//先逐个流域进行计算，后面在变成多个计算
    private String basin;
    @Column(name = "dispatch_type", columnDefinition = "TEXT COMMENT '计算模型类型（规则调度、规程优化、预设条件）'")
    private String dispatchType;
    // 关联子表，单个流域发电能力计算结果，一对一关系
    @OneToMany(mappedBy = "generationCalScheme", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<GenerationCalBasinOut> generationCalBasinOuts;
}
