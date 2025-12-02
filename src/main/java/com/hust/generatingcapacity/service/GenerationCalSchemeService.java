package com.hust.generatingcapacity.service;

import com.hust.generatingcapacity.dto.GenerationCalBasinOutDTO;
import com.hust.generatingcapacity.dto.GenerationCalSchemeDTO;
import com.hust.generatingcapacity.dto.GenerationCalStationOutDTO;
import com.hust.generatingcapacity.entity.GenerationCalBasinOut;
import com.hust.generatingcapacity.entity.GenerationCalScheme;
import com.hust.generatingcapacity.entity.GenerationCalStationOut;
import com.hust.generatingcapacity.iservice.IGenerationCalSchemeService;
import com.hust.generatingcapacity.repository.GenerationCalBasinOutRepository;
import com.hust.generatingcapacity.repository.GenerationCalSchemeRepository;
import com.hust.generatingcapacity.repository.GenerationCalStationOutRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GenerationCalSchemeService implements IGenerationCalSchemeService {
    @Autowired
    private GenerationCalSchemeRepository generationCalSchemeRepository;
    @Autowired
    private GenerationCalStationOutRepository generationCalStationOutRepository;
    @Autowired
    private GenerationCalBasinOutRepository generationCalBasinOutRepository;
    @Transactional
    @Override
    public GenerationCalSchemeDTO getGenerationCalSchemeDTO(String schemeName) {
        Integer id = generationCalSchemeRepository.findIdByName(schemeName).orElseThrow(() -> new IllegalArgumentException("未按方案名称找到对应方案数据！"));

        GenerationCalScheme generationCalScheme = generationCalSchemeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("未按方案id找到对应方案数据！"));
        List<GenerationCalBasinOut> generationCalBasinOuts = generationCalScheme.getGenerationCalBasinOuts();
        List<GenerationCalBasinOutDTO> generationCalBasinOutDTOS = new ArrayList<>();
        for (GenerationCalBasinOut generationCalBasinOut : generationCalBasinOuts) {
            GenerationCalBasinOutDTO basinOutDTO = new GenerationCalBasinOutDTO();
            BeanUtils.copyProperties(generationCalBasinOut, basinOutDTO);

            List<GenerationCalStationOut> calStationOuts = generationCalBasinOut.getGenerationCalStationOuts();
            List<GenerationCalStationOutDTO> stationOutDTOs = new ArrayList<>();
            for (GenerationCalStationOut generationCalStationOut : calStationOuts) {
                GenerationCalStationOutDTO stationOutDTO = new GenerationCalStationOutDTO();
                BeanUtils.copyProperties(generationCalStationOut, stationOutDTO);
                stationOutDTOs.add(stationOutDTO);
            }

            basinOutDTO.setGenerationCalStationOuts(stationOutDTOs);
            generationCalBasinOutDTOS.add(basinOutDTO);

        }

        GenerationCalSchemeDTO generationCalSchemeDTO = new GenerationCalSchemeDTO();
        BeanUtils.copyProperties(generationCalScheme,generationCalSchemeDTO);
        generationCalSchemeDTO.setGenerationCalBasinOuts(generationCalBasinOutDTOS);

        return generationCalSchemeDTO;
    }

}
