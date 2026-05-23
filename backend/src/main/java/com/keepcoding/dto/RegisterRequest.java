package com.keepcoding.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload do POST /api/auth/register. */
public record RegisterRequest(

        @NotBlank(message = "username é obrigatório")
        @Size(min = 3, max = 50, message = "username deve ter entre 3 e 50 caracteres")
        String username,

        @NotBlank(message = "email é obrigatório")
        @Email(message = "email inválido")
        String email,

        @NotBlank(message = "password é obrigatório")
        @Size(min = 6, max = 100, message = "password deve ter ao menos 6 caracteres")
        String password
) {}
