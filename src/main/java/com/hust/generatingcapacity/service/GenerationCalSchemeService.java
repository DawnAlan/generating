package com.hust.generatingcapacity.service;

import com.hust.generatingcapacity.dto.GenerationCalSchemeDTO;
import com.hust.generatingcapacity.entity.GenerationCalScheme;
import com.hust.generatingcapacity.iservice.IGenerationCalSchemeService;
import com.hust.generatingcapacity.repository.GenerationCalBasinOutRepository;
import com.hust.generatingcapacity.repository.GenerationCalSchemeRepository;
import com.hust.generatingcapacity.repository.GenerationCalStationOutRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GenerationCalSchemeService implements IGenerationCalSchemeService {
    @Autowired
    private GenerationCalSchemeRepository generationCalSchemeRepository;
    @Autowired
    private GenerationCalStationOutRepository generationCalStationOutRepository;
    @Autowired
    private GenerationCalBasinOutRepository generationCalBasinOutRepository;

    @Override
    public GenerationCalSchemeDTO getGenerationCalSchemeDTO(String schemeName) {
        Integer id = generationCalSchemeRepository.findIdByName(schemeName).orElseThrow(() -> new IllegalArgumentException("未按方案id找到对应方案数据！"));

        GenerationCalScheme generationCalScheme = generationCalSchemeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("未按方案id找到对应方案数据！"));


        GenerationCalSchemeDTO generationCalSchemeDTO = null;
        return generationCalSchemeDTO;
    }

}
