package com.hust.generatingcapacity.controller;

import com.hust.generatingcapacity.config.ResponseMessage;
import com.hust.generatingcapacity.dto.GenerationCalSchemeDTO;
import com.hust.generatingcapacity.iservice.IBasinCalculateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;


@RestController //接口方法可以返回对象 对象直接转换成json文本
@RequestMapping("/api/basinCalculate")
public class BainCalculateController {
    @Autowired
    IBasinCalculateService basinCalculateService;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @PostMapping("/addCalScheme")
    public ResponseMessage<GenerationCalSchemeDTO> add(@Validated @RequestBody GenerationCalSchemeDTO dto) {
        System.out.println(sdf.format(new Date()) + ":-----------已调用'basinCalculate'中计算某流域发电能力的功能----------");
        basinCalculateService.basinCalculate(dto);
        return ResponseMessage.success(dto);
    }

}
