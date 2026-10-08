package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EstadoCivilConverter implements AttributeConverter<EstadoCivil, String> {

    @Override
    public String convertToDatabaseColumn(EstadoCivil attribute) {
        if (attribute == null) return null;
        return attribute.getCodigo();
    }

    @Override
    public EstadoCivil convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) return null;
        return EstadoCivil.fromCodigo(dbData.trim());
    }
}
