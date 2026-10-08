package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EstatusCuentaConverter implements AttributeConverter<EstatusCuenta, String> {

    @Override
    public String convertToDatabaseColumn(EstatusCuenta attribute) {
        if (attribute == null) return null;
        return attribute.getCodigo();
    }

    @Override
    public EstatusCuenta convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) return null;
        return EstatusCuenta.fromCodigo(dbData.trim());
    }
}
