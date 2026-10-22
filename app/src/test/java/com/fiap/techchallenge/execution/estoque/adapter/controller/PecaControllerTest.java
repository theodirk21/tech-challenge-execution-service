package com.fiap.techchallenge.execution.estoque.adapter.controller;

import com.fiap.techchallenge.execution.estoque.domain.PecaEstoque;
import com.fiap.techchallenge.execution.estoque.usecase.EstoqueUseCase;
import com.fiap.techchallenge.execution.exception.NotFoundException;
import com.fiap.techchallenge.execution.security.SecurityConfig;
import com.fiap.techchallenge.execution.support.TokenSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PecaController.class)
@Import({SecurityConfig.class, PecaMapper.class})
class PecaControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean EstoqueUseCase estoque;

    @Test
    void listaPecas() throws Exception {
        when(estoque.listarPecas()).thenReturn(List.of(new PecaEstoque(1L, "Filtro", 4)));

        mvc.perform(get("/api/peca").header("Authorization", TokenSupport.bearer("GERENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Filtro"))
                .andExpect(jsonPath("$[0].quantidade").value(4));
    }

    @Test
    void buscaPorIdInexistenteRetorna404() throws Exception {
        when(estoque.buscarPeca(9L)).thenThrow(new NotFoundException("Peça de id 9 não encontrada no estoque!"));

        mvc.perform(get("/api/peca/9").header("Authorization", TokenSupport.bearer("GERENTE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Peça de id 9 não encontrada no estoque!"));
    }

    @Test
    void criaPeca() throws Exception {
        when(estoque.criarPeca("Filtro", 4)).thenReturn(new PecaEstoque(1L, "Filtro", 4));

        mvc.perform(post("/api/peca").header("Authorization", TokenSupport.bearer("GERENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Filtro\",\"quantidade\":4}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void criaPecaInvalidaRetorna400() throws Exception {
        mvc.perform(post("/api/peca").header("Authorization", TokenSupport.bearer("GERENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\" \",\"quantidade\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reposicaoSomaEstoque() throws Exception {
        when(estoque.reporEstoque(1L, 5)).thenReturn(new PecaEstoque(1L, "Filtro", 9));

        mvc.perform(post("/api/peca/1/reposicao").header("Authorization", TokenSupport.bearer("GERENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(9));
    }

    @Test
    void semTokenOuPerfilIndevidoRetorna403() throws Exception {
        mvc.perform(get("/api/peca")).andExpect(status().isForbidden());
        mvc.perform(get("/api/peca").header("Authorization", TokenSupport.bearer("MECANICO")))
                .andExpect(status().isForbidden());
    }
}