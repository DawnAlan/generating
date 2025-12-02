package com.hust.generatingcapacity.controller;

import com.hust.generatingcapacity.config.ResponseMessage;
import com.hust.generatingcapacity.dto.GenerationCalSchemeDTO;
import com.hust.generatingcapacity.dto.StationInfDTO;
import com.hust.generatingcapacity.iservice.IBasinCalculateService;
import com.hust.generatingcapacity.iservice.IGenerationCalSchemeService;
import com.hust.generatingcapacity.model.generation.domain.StationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

@RestController //接口方法可以返回对象 对象直接转换成json文本
@RequestMapping("/api/getGenerationCalScheme")
public class GenerationCalSchemeController {
    @Autowired
    IGenerationCalSchemeService generationCalSchemeService;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @GetMapping("/getSchemeDTO")
    public ResponseMessage<GenerationCalSchemeDTO> getSchemeDTO(@RequestParam("name") String name) {
        System.out.println(sdf.format(new Date()) + ":-----------已调用'GenerationCalSchemeController'中查询某方案结果的功能----------");
        GenerationCalSchemeDTO dto = generationCalSchemeService.getGenerationCalSchemeDTO(name);
        return ResponseMessage.success(dto);
    }
}
