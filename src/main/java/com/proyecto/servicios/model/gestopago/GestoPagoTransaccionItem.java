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
@XmlRootElement(name = "TRANSACCION")
@XmlAccessorType(XmlAccessType.FIELD)
public class GestoPagoTransaccionItem {

    @XmlElement(name = "ID_TRANSACCION")
    private String idTransaccion;

    @XmlElement(name = "FOLIO")
    private String folio;

    @XmlElement(name = "AUTORIZACION")
    private String autorizacion;

    @XmlElement(name = "FECHA")
    private String fecha;

    @XmlElement(name = "MONTO")
    private String monto;

    @XmlElement(name = "COMISION")
    private String comision;

    @XmlElement(name = "SALDO")
    private String saldo;

    @XmlElement(name = "TICKET")
    private String ticket;
}
