package com.hust.generatingcapacity.iservice;

import com.hust.generatingcapacity.dto.GenerationCalSchemeDTO;

public interface IGenerationCalSchemeService {
    GenerationCalSchemeDTO getGenerationCalSchemeDTO(String schemeName);

    String deleteGenerationCalScheme(String schemeName);

    Boolean isSchemeExist(String schemeName);

    String changeSchemeName(String name, String newName);
}
