package com.proyecto.servicios.model.onboarding;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String token;
    private String tipo;
    private Long expiraEn;
    private Boolean requiere2FA;
    private String tokenTemporal;
    private String codigoOtp;
    private String dispositivo;
    private String mensaje;
}
