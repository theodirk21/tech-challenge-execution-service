package com.fiap.techchallenge.execution.fila.adapter.controller;

import com.fiap.techchallenge.execution.diagnostico.domain.DiagnosticoItem;
import com.fiap.techchallenge.execution.diagnostico.domain.TipoItemDiagnostico;
import com.fiap.techchallenge.execution.exception.ConflictException;
import com.fiap.techchallenge.execution.exception.NotFoundException;
import com.fiap.techchallenge.execution.fila.domain.ItemFila;
import com.fiap.techchallenge.execution.fila.domain.StatusFila;
import com.fiap.techchallenge.execution.fila.usecase.FilaUseCase;
import com.fiap.techchallenge.execution.security.SecurityConfig;
import com.fiap.techchallenge.execution.servico.domain.Servico;
import com.fiap.techchallenge.execution.servico.usecase.ServicoUseCase;
import com.fiap.techchallenge.execution.support.TokenSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExecucaoController.class)
@Import({SecurityConfig.class, ExecucaoMapper.class})
class ExecucaoControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean FilaUseCase fila;
    @MockitoBean ServicoUseCase servicos;

    private static ItemFila item(StatusFila status) {
        return new ItemFila(1L, 10L, status, 0, 2L, "freio", "ABC1D23", "Gol",
                LocalDateTime.of(2026, 10, 22, 10, 0), null, null);
    }

    private static String mecanico() {
        return TokenSupport.bearer("MECANICO");
    }

    @Test
    void listaFilaComStatusPadraoNaFila() throws Exception {
        when(fila.listarPorStatus(StatusFila.NA_FILA)).thenReturn(List.of(item(StatusFila.NA_FILA)));

        mvc.perform(get("/api/execucao/fila").header("Authorization", TokenSupport.bearer("ATENDENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].osId").value(10))
                .andExpect(jsonPath("$[0].veiculoPlaca").value("ABC1D23"));
    }

    @Test
    void buscaItemEInexistenteRetorna404() throws Exception {
        when(fila.buscarPorOsId(10L)).thenReturn(item(StatusFila.NA_FILA));
        when(fila.buscarPorOsId(11L)).thenThrow(new NotFoundException("Item da fila da OS 11 não encontrado!"));

        mvc.perform(get("/api/execucao/10").header("Authorization", mecanico()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("NA_FILA"));
        mvc.perform(get("/api/execucao/11").header("Authorization", mecanico()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listaDiagnosticoEServicos() throws Exception {
        when(fila.listarDiagnostico(10L)).thenReturn(List.of(new DiagnosticoItem(1L, 10L, TipoItemDiagnostico.PECA, 7L, 2)));
        when(servicos.listarPorOs(10L)).thenReturn(List.of(Servico.criar(3L, 10L)));

        mvc.perform(get("/api/execucao/10/diagnostico").header("Authorization", mecanico()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].tipo").value("PECA"));
        mvc.perform(get("/api/execucao/10/servicos").header("Authorization", mecanico()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("AGUARDANDO_INICIO"));
    }

    @Test
    void iniciaDiagnostico() throws Exception {
        when(fila.iniciarDiagnostico(10L, 2L)).thenReturn(item(StatusFila.EM_DIAGNOSTICO));

        mvc.perform(post("/api/execucao/10/diagnostico/iniciar").header("Authorization", mecanico())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mecanicoId\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_DIAGNOSTICO"));
    }

    @Test
    void concluiDiagnostico() throws Exception {
        when(fila.concluirDiagnostico(eq(10L), anyList())).thenReturn(item(StatusFila.AGUARDANDO_PAGAMENTO));

        mvc.perform(post("/api/execucao/10/diagnostico/concluir").header("Authorization", mecanico())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itens\":[{\"tipo\":\"PECA\",\"refId\":7,\"quantidade\":2}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"));
    }

    @Test
    void diagnosticoInvalidoRetorna400SemChamarOCasoDeUso() throws Exception {
        mvc.perform(post("/api/execucao/10/diagnostico/concluir").header("Authorization", mecanico())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"itens\":[]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/execucao/10/diagnostico/concluir").header("Authorization", mecanico())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itens\":[{\"tipo\":\"OUTRO\",\"refId\":7,\"quantidade\":2}]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/execucao/10/diagnostico/concluir").header("Authorization", mecanico())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itens\":[{\"tipo\":\"PECA\",\"quantidade\":0}]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(fila);
    }

    @Test
    void iniciaProximaExecucao() throws Exception {
        when(fila.iniciarProximaExecucao(2L)).thenReturn(item(StatusFila.EM_EXECUCAO));

        mvc.perform(post("/api/execucao/iniciar").header("Authorization", mecanico())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mecanicoId\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_EXECUCAO"));
    }

    @Test
    void finalizaEConflitoRetorna409() throws Exception {
        when(fila.finalizarExecucao(10L)).thenReturn(item(StatusFila.FINALIZADA));
        when(fila.finalizarExecucao(11L)).thenThrow(new ConflictException("A OS 11 não está em execução!"));

        mvc.perform(post("/api/execucao/10/finalizar").header("Authorization", mecanico()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FINALIZADA"));
        mvc.perform(post("/api/execucao/11/finalizar").header("Authorization", mecanico()))
                .andExpect(status().isConflict());
    }

    @Test
    void registraFalha() throws Exception {
        when(fila.falharExecucao(10L, "PECA_DEFEITUOSA", "x")).thenReturn(item(StatusFila.FALHOU));

        mvc.perform(post("/api/execucao/10/falha").header("Authorization", mecanico())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"PECA_DEFEITUOSA\",\"detalhe\":\"x\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FALHOU"));
    }

    @Test
    void autorizacaoPorPerfil() throws Exception {
        mvc.perform(get("/api/execucao/fila")).andExpect(status().isForbidden());
        mvc.perform(post("/api/execucao/10/finalizar").header("Authorization", TokenSupport.bearer("ATENDENTE")))
                .andExpect(status().isForbidden());
    }
}