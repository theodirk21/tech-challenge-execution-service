package com.fiap.techchallenge.execution.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JwtAuthenticationFilterTest {

    static final String SECRET = "minha-chave-secreta-dev-minimo-32-chars!!";

    final JwtUtil jwt = new JwtUtil(SECRET, 3600);
    final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt);
    final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest comHeader(String valor) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        if (valor != null) {
            req.addHeader("Authorization", valor);
        }
        return req;
    }

    private void executar(MockHttpServletRequest req) throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, chain);
        verify(chain).doFilter(req, res);
    }

    @Test
    void semHeaderOuSemBearerNaoAutentica() throws Exception {
        executar(comHeader(null));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void headerSemPrefixoBearerNaoAutentica() throws Exception {
        executar(comHeader("Basic abc"));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void tokenValidoAutenticaComRole() throws Exception {
        executar(comHeader("Bearer " + jwt.generateAccessToken("ana", "MECANICO")));

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getName()).isEqualTo("ana");
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_MECANICO");
    }

    @Test
    void roleComPrefixoRoleENormalizada() throws Exception {
        executar(comHeader("Bearer " + jwt.generateAccessToken("ana", "ROLE_GERENTE")));

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString).containsExactly("ROLE_GERENTE");
    }

    @Test
    void roleVaziaNaoAutentica() throws Exception {
        executar(comHeader("Bearer " + jwt.generateAccessToken("ana", " ")));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void tokenInvalidoOuAssinadoComOutraChaveNaoAutentica() throws Exception {
        executar(comHeader("Bearer lixo"));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        var outro = new JwtUtil("outra-chave-secreta-dev-minimo-32-chars!!!", 3600);
        executar(comHeader("Bearer " + outro.generateAccessToken("ana", "GERENTE")));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void naoSobrescreveAutenticacaoExistente() throws Exception {
        var existente = new UsernamePasswordAuthenticationToken("outro", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existente);

        executar(comHeader("Bearer " + jwt.generateAccessToken("ana", "MECANICO")));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existente);
    }

    @Test
    void jwtUtilExtraiClaims() {
        String token = jwt.generateAccessToken("ana", "GERENTE");

        assertThat(jwt.isTokenValid(token)).isTrue();
        assertThat(jwt.isTokenValid("x")).isFalse();
        assertThat(jwt.extractLogin(token)).isEqualTo("ana");
        assertThat(jwt.extractRole(token)).isEqualTo("GERENTE");
    }
}