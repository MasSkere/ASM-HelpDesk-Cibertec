-- Datos de prueba (SOLO desarrollo). Ejecutar después de asm_helpdesk_postgres16.sql y procesos.sql.
-- Las contraseñas son marcadores: reemplace por hashes BCrypt generados por la app.
INSERT INTO usuarios (nombres, apellidos, correo, password, rol) VALUES
    ('Admin',  'Sistema',  'admin@cibertec.edu.pe',   '$2a$10$REEMPLAZAR_HASH_BCRYPT', 'ADMINISTRADOR'),
    ('Carlos', 'Ramos',    'tecnico1@cibertec.edu.pe','$2a$10$REEMPLAZAR_HASH_BCRYPT', 'TECNICO'),
    ('Lucía',  'Torres',   'tecnico2@cibertec.edu.pe','$2a$10$REEMPLAZAR_HASH_BCRYPT', 'TECNICO'),
    ('Ana',    'Quispe',   'docente@cibertec.edu.pe', '$2a$10$REEMPLAZAR_HASH_BCRYPT', 'DOCENTE'),
    ('Luis',   'Mendoza',  'alumno@cibertec.edu.pe',  '$2a$10$REEMPLAZAR_HASH_BCRYPT', 'ESTUDIANTE');

INSERT INTO tickets (asunto, descripcion, tipo_solicitud, prioridad, id_solicitante, id_categoria) VALUES
    ('Wi-Fi caído en laboratorio 3', 'No hay señal desde las 8am', 'INCIDENTE',     'ALTA',  4, 3),
    ('No puedo ingresar al aula virtual', 'Error de credenciales', 'INCIDENTE',     'MEDIA', 5, 6),
    ('Instalar Visual Studio Code', 'Para el curso de LP2',        'REQUERIMIENTO', 'BAJA',  5, 2);
