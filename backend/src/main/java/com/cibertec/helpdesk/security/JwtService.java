package com.cibertec.helpdesk.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${security.jwt.secret}")
    private String secretKey;

    @Value("${security.jwt.expiration-ms}")
    private long expirationMs;

    // Obtener la clave secreta
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Crear un token JWT
    public String generarToken(UserDetails usuario) {
        Date ahora = new Date();

        return Jwts.builder()
                .subject(usuario.getUsername())
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    // Extraer el correo electrónico del token
    public String extraerCorreo(String token) {
        return extraerClaims(token).getSubject();
    }

    // Validar el token
    public boolean validarToken(String token, UserDetails usuario) {
        try {
            String correo = extraerCorreo(token);

            return correo.equals(usuario.getUsername())
                    && usuario.isEnabled()
                    && !extraerClaims(token)
                            .getExpiration().before(new Date());

        } catch (Exception e) {
            return false;
        }
    }

    // Leer y verificar la firma del token
    private Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
