package com.fiap.techchallenge.execution.support;

import com.fiap.techchallenge.execution.security.JwtUtil;

public final class TokenSupport {

    private static final JwtUtil JWT = new JwtUtil("minha-chave-secreta-dev-minimo-32-chars!!", 3600);

    private TokenSupport() {}

    public static String bearer(String role) {
        return "Bearer " + JWT.generateAccessToken("usuario", role);
    }
}