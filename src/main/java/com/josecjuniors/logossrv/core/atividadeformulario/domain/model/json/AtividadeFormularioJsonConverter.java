package com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AtividadeFormularioJsonConverter implements AttributeConverter<AtividadeFormularioJson, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(AtividadeFormularioJson attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            // Registrar o módulo JavaTime é uma boa prática para lidar com datas/horas no JSON
            objectMapper.registerModule(new JavaTimeModule());
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao converter AtividadeFormularioJson para JSON", e);
        }
    }

    @Override
    public AtividadeFormularioJson convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            objectMapper.registerModule(new JavaTimeModule());
            return objectMapper.readValue(dbData, AtividadeFormularioJson.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao converter JSON para AtividadeFormularioJson", e);
        }
    }
}
