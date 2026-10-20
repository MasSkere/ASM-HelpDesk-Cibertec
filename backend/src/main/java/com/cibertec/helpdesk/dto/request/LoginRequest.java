package com.cibertec.helpdesk.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Ingrese un correo valido")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        String password

) {}
