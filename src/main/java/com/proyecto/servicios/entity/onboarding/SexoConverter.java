package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SexoConverter implements AttributeConverter<Sexo, String> {

    @Override
    public String convertToDatabaseColumn(Sexo attribute) {
        if (attribute == null) return null;
        return attribute.getCodigo();
    }

    @Override
    public Sexo convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) return null;
        return Sexo.fromCodigo(dbData.trim());
    }
}
