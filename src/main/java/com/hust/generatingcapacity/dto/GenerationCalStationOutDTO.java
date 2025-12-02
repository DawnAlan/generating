package com.hust.generatingcapacity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
@Data
@Getter
@Setter
public class GenerationCalStationOutDTO {

    private Integer genStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")

    private Date time;

    private String station;

    private Double inFlow;

    private Double levelBef;

    private Double levelAft;

    private Double qp;

    private Double qo;

    private Double calGen;

    private Double calHis;

    private String remark;
}
