package com.hust.generatingcapacity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hust.generatingcapacity.entity.GenerationCalStationOut;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;
@Data
@Getter
@Setter
public class GenerationCalBasinOutDTO {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date time;
    private String basin;
    // 关联子表，单个流域电站发电能力计算结果，一对多关系
    private List<GenerationCalStationOutDTO> generationCalStationOuts;
}
