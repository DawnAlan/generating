package com.hust.generatingcapacity.controller;

import com.hust.generatingcapacity.config.ResponseMessage;
import com.hust.generatingcapacity.dto.GenerationCalSchemeDTO;
import com.hust.generatingcapacity.iservice.IGenerationCalSchemeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping("/changeSchemeName")
    public ResponseMessage<String> changeSchemeName(@RequestParam("name") String name, @RequestParam("newName") String newName) {
        System.out.println(sdf.format(new Date()) + ":-----------已调用'GenerationCalSchemeController'中修改某方案名称的功能----------");
        try {
            String change = generationCalSchemeService.changeSchemeName(name, newName);
            return ResponseMessage.success(change);
        } catch (Exception e) {
            return ResponseMessage.error(e.getMessage());
        }
    }

    @DeleteMapping("/deleteScheme")
    public ResponseMessage<String> deleteScheme(@RequestParam("name") String name) {
        System.out.println(sdf.format(new Date()) + ":-----------已调用'GenerationCalSchemeController'中删除某方案的功能----------");
        String delete;
        try {
            delete = generationCalSchemeService.deleteGenerationCalScheme(name);
            return ResponseMessage.success(delete);
        } catch (Exception e) {
            return ResponseMessage.error(e.getMessage());
        }
    }
}
