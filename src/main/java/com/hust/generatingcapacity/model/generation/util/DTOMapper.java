package com.hust.generatingcapacity.model.generation.util;

import com.hust.generatingcapacity.dto.CodeValueDTO;
import com.hust.generatingcapacity.dto.ConstraintInfDTO;
import com.hust.generatingcapacity.dto.NHQCellDTO;
import com.hust.generatingcapacity.model.generation.domain.CodeValue;
import com.hust.generatingcapacity.model.generation.domain.ConstraintData;
import com.hust.generatingcapacity.model.generation.domain.NHQCell;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DTOMapper {
    //快速生成NHQCellDTO的映射方法
    NHQCell toNHQCell(NHQCellDTO nhqCellDTO);
    //快速生成NHQCellDTO列表的映射方法
    List<NHQCell> toNHQCellList(List<NHQCellDTO> nhqCellDTOs);
    //快速生成CodeValueDTO的映射方法
    CodeValue toCodeValue(CodeValueDTO codeValueDTO);
    List<CodeValue> toCodeValueList(List<CodeValueDTO> codeValueDTOs);
}
