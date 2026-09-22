CREATE TABLE IF NOT EXISTS gestopago_tokens (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_distribuidor       INT           NOT NULL,
    codigo_dispositivo    VARCHAR(100)  NOT NULL,
    token                 TEXT          NOT NULL,
    token_type            VARCHAR(50),
    expires_in            BIGINT,
    fecha_creacion        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activo                BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_gestopago_tokens UNIQUE (id_distribuidor, codigo_dispositivo)
);