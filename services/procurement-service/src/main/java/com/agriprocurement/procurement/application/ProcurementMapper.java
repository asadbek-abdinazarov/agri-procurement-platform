package com.agriprocurement.procurement.application;

import com.agriprocurement.procurement.domain.Procurement;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProcurementMapper {
    // Mapper is not used for domain entity creation - removed to avoid compilation issues
    // ProcurementResponse uses static factory methods instead
}
