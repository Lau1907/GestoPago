package com.proyecto.servicios.model.gestopago;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "MENSAJE")
@XmlAccessorType(XmlAccessType.FIELD)
public class GestoPagoMensajeResponse {

    @XmlElement(name = "CODIGO")
    private String codigo;

    @XmlElement(name = "TEXTO")
    private String texto;
}
