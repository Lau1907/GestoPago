package com.proyecto.servicios.config;

import com.proyecto.servicios.entity.gestopago.CatalogoProducto;
import com.proyecto.servicios.repositorys.gestopago.CatalogoProductoRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga inicial del catálogo oficial de productos GestoPago desde
 * {@code classpath:gestopago.catalogo_productos.csv} cuando la tabla
 * {@code catalogo_productos} se encuentra vacía.
 */
@Component
@Order(1)
@Slf4j
public class CatalogoProductosInitializer implements ApplicationRunner {

    private static final String CSV_PATH = "gestopago.catalogo_productos.csv";

    private final CatalogoProductoRepository catalogoProductoRepository;

    public CatalogoProductosInitializer(CatalogoProductoRepository catalogoProductoRepository) {
        this.catalogoProductoRepository = catalogoProductoRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long existentes = catalogoProductoRepository.count();
        if (existentes > 0) {
            log.info("Catálogo GestoPago ya inicializado en base de datos ({} productos). Se omite carga CSV.", existentes);
            return;
        }

        ClassPathResource resource = new ClassPathResource(CSV_PATH);
        if (!resource.exists()) {
            log.warn("No se encontró el archivo CSV {} en el classpath. La tabla catalogo_productos permanecerá vacía.", CSV_PATH);
            return;
        }

        log.info("Tabla catalogo_productos vacía. Iniciando carga inicial desde {}...", CSV_PATH);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                log.warn("El archivo {} está vacío.", CSV_PATH);
                return;
            }

            Map<Integer, CatalogoProducto> productosPorId = new LinkedHashMap<>();
            StringBuilder recordBuilder = new StringBuilder();
            String line;
            boolean inQuotes = false;

            while ((line = reader.readLine()) != null) {
                if (recordBuilder.length() > 0) {
                    recordBuilder.append('\n');
                }
                recordBuilder.append(line);
                inQuotes = updateQuoteState(line, inQuotes);

                if (inQuotes) {
                    continue;
                }

                String rawRecord = recordBuilder.toString();
                recordBuilder.setLength(0);
                if (StringUtils.isBlank(rawRecord)) {
                    continue;
                }

                List<String> fields = parseCsvLine(rawRecord);
                if (fields.size() < 13) {
                    log.warn("Fila CSV ignorada por columnas insuficientes ({}): {}", fields.size(), rawRecord);
                    continue;
                }

                CatalogoProducto entidad = mapFieldsToEntity(fields);
                if (entidad != null && entidad.getIdProducto() != null) {
                    productosPorId.put(entidad.getIdProducto(), entidad);
                }
            }

            if (!productosPorId.isEmpty()) {
                catalogoProductoRepository.saveAll(productosPorId.values());
                log.info("Carga inicial completada: {} productos insertados en catalogo_productos.", productosPorId.size());
            }
        } catch (IOException e) {
            log.error("Error al leer el archivo CSV {}: {}", CSV_PATH, e.getMessage(), e);
        }
    }

    private CatalogoProducto mapFieldsToEntity(List<String> fields) {
        Integer idServicio = parseInteger(fields.get(3));
        Integer idProducto = parseInteger(fields.get(4));
        if (idServicio == null || idProducto == null) {
            return null;
        }

        String servicio = StringUtils.defaultIfBlank(fields.get(1).trim(), "Servicio");
        String nombreProducto = StringUtils.defaultIfBlank(fields.get(2).trim(), "Producto");

        return CatalogoProducto.builder()
                .servicio(truncate(servicio, 255))
                .nombreProducto(truncate(nombreProducto, 255))
                .idServicio(idServicio)
                .idProducto(idProducto)
                .idCatTipoServicio(parseInteger(fields.get(5)))
                .tipoFront(parseInteger(fields.get(6)))
                .hasDigitoVerificador(Boolean.parseBoolean(fields.get(7).trim()))
                .precio(truncate(emptyToNull(fields.get(8)), 50))
                .showAyuda(Boolean.parseBoolean(fields.get(9).trim()))
                .tipoReferencia(truncate(emptyToNull(fields.get(10)), 50))
                .legend(emptyToNull(fields.get(11)))
                .urlLogo(truncate(emptyToNull(fields.get(12)), 255))
                .fechaActualizacion(null)
                .build();
    }

    private boolean updateQuoteState(String line, boolean currentlyInQuotes) {
        boolean inQuotes = currentlyInQuotes;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '"') {
                inQuotes = !inQuotes;
            }
        }
        return inQuotes;
    }

    static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    private Integer parseInteger(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String emptyToNull(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    private String truncate(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }
}
