package com.hust.generatingcapacity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hust.generatingcapacity.entity.GenerationCalBasinOut;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Data
@Getter
@Setter
public class GenerationCalSchemeDTO {

    private String description;
    private String schemeName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endDate;
    private String period;
    private int schemeL;
    private String basin;
    //规则调度、规程优化、预设条件
    private String dispatchType;

    private List<GenerationCalBasinOutDTO> generationCalBasinOuts;
}
