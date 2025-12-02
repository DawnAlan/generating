package com.hust.generatingcapacity.controller;

import com.hust.generatingcapacity.config.ResponseMessage;
import com.hust.generatingcapacity.dto.StationBaseInfDTO;
import com.hust.generatingcapacity.dto.StationInfDTO;
import com.hust.generatingcapacity.iservice.IHydropowerStationService;
import com.hust.generatingcapacity.model.generation.domain.StationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RestController //接口方法可以返回对象 对象直接转换成json文本
@RequestMapping("/api/hydropowerStation")
public class HydropowerStationController {
    @Autowired
    IHydropowerStationService hydropowerStationService;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @GetMapping("/getOneStationDTO")
    public ResponseMessage<StationData> getOneStationDTO(@RequestParam("name") String name) {
        System.out.println(sdf.format(new Date()) + ":-----------已调用'HydropowerStation'中查询某电站信息的功能----------");
        StationInfDTO dto = hydropowerStationService.get(name);
        StationData stationData = hydropowerStationService.changeToStationData(dto);
        return ResponseMessage.success(stationData);
    }

    @GetMapping("/getOneBasinDTO")
    public ResponseMessage<List<StationBaseInfDTO>> getOneBasinDTO(@RequestParam("name") String name) {
        System.out.println(sdf.format(new Date()) + ":-----------已调用'HydropowerStation'中查询某流域信息的功能----------");
        List<StationBaseInfDTO> dto = hydropowerStationService.getBasinStations(name);
        return ResponseMessage.success(dto);
    }




}
