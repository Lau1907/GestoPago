package com.proyecto.servicios.model.gestopago;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "RESPONSE")
@XmlAccessorType(XmlAccessType.FIELD)
public class GestoPagoProductListResponse {

    @XmlElement(name = "MENSAJE")
    private GestoPagoMensajeResponse mensaje;

    @XmlElementWrapper(name = "PRODUCTOS")
    @XmlElement(name = "producto")
    @Builder.Default
    private List<GestoPagoProductoItem> productos = new ArrayList<>();
}
