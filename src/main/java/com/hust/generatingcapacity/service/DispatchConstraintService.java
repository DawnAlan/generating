package com.hust.generatingcapacity.service;

import com.hust.generatingcapacity.dto.ConstraintInfDTO;
import com.hust.generatingcapacity.entity.ConstraintCondition;
import com.hust.generatingcapacity.entity.ConstraintParam;
import com.hust.generatingcapacity.iservice.IDispatchConstraintService;
import com.hust.generatingcapacity.repository.ConstraintConditionRepository;
import com.hust.generatingcapacity.repository.ConstraintParamRepository;
import com.hust.generatingcapacity.repository.DispatchConstraintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DispatchConstraintService implements IDispatchConstraintService {
    @Autowired
    DispatchConstraintRepository dispatchConstraintRepository;
    @Autowired
    ConstraintConditionRepository constraintConditionRepository;
    @Autowired
    ConstraintParamRepository constraintParamRepository;

    @Override
    public List<ConstraintInfDTO> get(String stationName) {
        List<ConstraintInfDTO> result = dispatchConstraintRepository.findAllByStationName(stationName);
        if (!result.isEmpty()) {
            for (ConstraintInfDTO constraintInfDTO : result) {
                //获取与约束生效条件与约束值
                Integer id = constraintInfDTO.getId();
                List<ConstraintCondition> conditions = constraintConditionRepository.findByConstraintId(id);
                List<ConstraintParam> params = constraintParamRepository.findByConstraintId(id);
                //将约束转换为表达式
                String condition = buildConditionExpression(conditions);
                List<String> param = buildParamExpression(params);
                constraintInfDTO.setCondition(condition);
                constraintInfDTO.setParam(param);
            }
        }
        return result;
    }

    /**
     * 生成约束生效条件表达式
     * @param conditions
     * @return
     */
    private String buildConditionExpression(List<ConstraintCondition> conditions) {
        StringBuilder sb = new StringBuilder();
        for (ConstraintCondition c : conditions) {
            sb.append(c.getConditionName())
                    .append(" ")
                    .append(c.getOperator())
                    .append(" ")
                    .append(c.getValue());

            if (c.getConnector() != null && !c.getConnector().isBlank()) {
                sb.append(" ").append(c.getConnector()).append(" ");
            }
        }
        return sb.toString().trim();
    }

    /**
     * 生成约束参数表达式
     * @param params
     * @return
     */
    private List<String> buildParamExpression(List<ConstraintParam> params) {
        List<String> paramList = new ArrayList<>();
        for (ConstraintParam c : params) {
            StringBuilder builder = new StringBuilder();
            builder.append(c.getParamName())
                    .append(" ")
                    .append(c.getOperator())
                    .append(" ")
                    .append(c.getValue());
            paramList.add(builder.toString().trim());
        }
        return paramList;
    }


}
