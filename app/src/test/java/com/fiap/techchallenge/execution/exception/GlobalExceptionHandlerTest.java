package com.fiap.techchallenge.execution.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapeiaExcecoesDeNegocioParaStatusHttp() {
        assertThat(handler.handleConflict(new ConflictException("c")).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(handler.handleServicoBadStatus(new ServicoBadStatusException("s")).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(handler.handleNotFound(new NotFoundException("n")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handleBadRequest(new BadRequestException("b")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void corpoIlegivelRetorna400() {
        var resp = handler.handleUnreadable(mock(HttpMessageNotReadableException.class));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).hasFieldOrPropertyWithValue("message", "Corpo da requisição inválido!");
    }
}