package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.GenericResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("ClienteNoEncontradoException debe retornar HTTP 404 con timestamp y mensaje claro")
    void handleClienteNoEncontradoException() {
        ClienteNoEncontradoException ex = new ClienteNoEncontradoException(42);
        ResponseEntity<GenericResponse> response = exceptionHandler.handleNotFoundExceptions(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getCodigo());
        assertEquals("Cliente no encontrado con el ID: 42", response.getBody().getMensaje());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("CurpDuplicadaException debe retornar HTTP 409 con timestamp")
    void handleCurpDuplicadaException() {
        CurpDuplicadaException ex = new CurpDuplicadaException("HEGG560427MVZRRL04");
        ResponseEntity<GenericResponse> response = exceptionHandler.handleDuplicateExceptions(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getCodigo());
        assertTrue(response.getBody().getMensaje().contains("HEGG560427MVZRRL04"));
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("CredencialesInvalidasException debe retornar HTTP 401 con timestamp")
    void handleCredencialesInvalidasException() {
        CredencialesInvalidasException ex = new CredencialesInvalidasException("Credenciales inválidas");
        ResponseEntity<GenericResponse> response = exceptionHandler.handleCredencialesInvalidasException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().getCodigo());
        assertEquals("Credenciales inválidas", response.getBody().getMensaje());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("UsuarioInactivoException debe retornar HTTP 403 con timestamp")
    void handleUsuarioInactivoException() {
        UsuarioInactivoException ex = new UsuarioInactivoException("El usuario se encuentra inactivo");
        ResponseEntity<GenericResponse> response = exceptionHandler.handleUsuarioInactivoException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().getCodigo());
        assertEquals("El usuario se encuentra inactivo", response.getBody().getMensaje());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("ErrorValidacionException debe retornar HTTP 400 con timestamp")
    void handleBadRequestBusinessExceptions() {
        ErrorValidacionException ex = new ErrorValidacionException("La fecha inicial no puede ser posterior a la fecha final");
        ResponseEntity<GenericResponse> response = exceptionHandler.handleBadRequestBusinessExceptions(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getCodigo());
        assertEquals("La fecha inicial no puede ser posterior a la fecha final", response.getBody().getMensaje());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("MethodArgumentNotValidException debe retornar HTTP 400 con lista de detalles y timestamp")
    void handleValidationException() throws Exception {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "clienteCreateRequest");
        bindingResult.addError(new FieldError("clienteCreateRequest", "curp", "La CURP es obligatoria"));
        bindingResult.addError(new FieldError("clienteCreateRequest", "correo", "El correo electrónico debe tener un formato válido"));

        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyMethod", String.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<GenericResponse> response = exceptionHandler.handleValidationException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getCodigo());
        assertNotNull(response.getBody().getTimestamp());
        assertNotNull(response.getBody().getDetalles());
        assertEquals(2, response.getBody().getDetalles().size());
        assertTrue(response.getBody().getDetalles().contains("[curp: La CURP es obligatoria]"));
        assertTrue(response.getBody().getDetalles().contains("[correo: El correo electrónico debe tener un formato válido]"));
    }

    @SuppressWarnings("unused")
    private void dummyMethod(String param) {
    }
}
