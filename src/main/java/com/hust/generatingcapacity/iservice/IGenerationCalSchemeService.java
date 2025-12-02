package com.hust.generatingcapacity.iservice;

import com.hust.generatingcapacity.dto.GenerationCalSchemeDTO;

public interface IGenerationCalSchemeService {
    GenerationCalSchemeDTO getGenerationCalSchemeDTO(String schemeName);
}
