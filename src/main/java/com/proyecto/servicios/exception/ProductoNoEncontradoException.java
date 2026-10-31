package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class ProductoNoEncontradoException extends NegocioException {

    public ProductoNoEncontradoException(Integer idProducto) {
        super(String.format("El producto con ID %d no fue encontrado en el catálogo de servicios de GestoPago", idProducto),
                HttpStatus.BAD_REQUEST);
    }
}
