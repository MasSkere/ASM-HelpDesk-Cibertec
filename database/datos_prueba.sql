-- =============================================================================
--  ASM-HelpDesk-Cibertec — Datos de prueba (SOLO DESARROLLO)
--  ---------------------------------------------------------------------------
--  Ejecutar DESPUÉS de database/schema.sql.
--  Requiere que la BD esté vacía (schema.sql hace DROP de las tablas).
--
--  Credenciales de los usuarios de prueba (contraseña en texto plano):
--    admin@cibertec.edu.pe     / Admin123!
--    tecnico1@cibertec.edu.pe  / Tecnico123!
--    tecnico2@cibertec.edu.pe  / Tecnico123!
--    docente@cibertec.edu.pe   / Docente123!
--    alumno@cibertec.edu.pe    / Alumno123!
--
--  Los hashes fueron generados con BCryptPasswordEncoder (Spring Security,
--  coste 10, prefijo $2a$), por lo que el login funciona tal cual.
--
--  Las referencias se resuelven por correo/nombre (no por IDs fijos) para que
--  el script funcione aunque la secuencia de identidad no empiece en 1.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- USUARIOS
-- -----------------------------------------------------------------------------
INSERT INTO usuarios (nombres, apellidos, correo, password, rol) VALUES
    ('Admin',  'Sistema', 'admin@cibertec.edu.pe',
     '$2a$10$j0VhlUwbZokTu7seAwLmt.jysuUoekzs6E/nG0jcEKwUI.jJJ1L6O', 'ADMINISTRADOR'),

    ('Carlos', 'Ramos',   'tecnico1@cibertec.edu.pe',
     '$2a$10$lGBpSEASpHv9LNzxCdcj4eIFJW1rvqZpWeWR4OK4iR7PfA3w/xfYG', 'TECNICO'),

    ('Lucía',  'Torres',  'tecnico2@cibertec.edu.pe',
     '$2a$10$VK8P7Ts/ftM37IqquL2LPuxUD2/2E/czQdRLPea1jaU8cKw7fPuuy', 'TECNICO'),

    ('Ana',    'Quispe',  'docente@cibertec.edu.pe',
     '$2a$10$R3vOFPGFLZ1sLS43/dxxH.DB4CiYkYhUQd2AYCEZV3Kwm4Q7xPA8.', 'DOCENTE'),

    ('Luis',   'Mendoza', 'alumno@cibertec.edu.pe',
     '$2a$10$JLkDNa70iZ1bo/pJ5W4uXuAyWRx7cQGDQea3zaYzfiSqZWl30B/vm', 'ESTUDIANTE');

-- -----------------------------------------------------------------------------
-- TICKETS  (el código TCK-AAAA-NNNN lo genera la BD; el historial lo registra
--           el trigger trg_tickets_historial automáticamente)
-- -----------------------------------------------------------------------------
INSERT INTO tickets (asunto, descripcion, tipo_solicitud, prioridad, id_solicitante, id_categoria)
SELECT 'Wi-Fi caído en laboratorio 3', 'No hay señal desde las 8am',
       'INCIDENTE', 'ALTA',
       (SELECT id_usuario FROM usuarios WHERE correo = 'docente@cibertec.edu.pe'),
       (SELECT id_categoria FROM categorias WHERE nombre = 'Redes e Internet')
UNION ALL
SELECT 'No puedo ingresar al aula virtual', 'Error de credenciales',
       'INCIDENTE', 'MEDIA',
       (SELECT id_usuario FROM usuarios WHERE correo = 'alumno@cibertec.edu.pe'),
       (SELECT id_categoria FROM categorias WHERE nombre = 'Aula Virtual')
UNION ALL
SELECT 'Instalar Visual Studio Code', 'Para el curso de LP2',
       'REQUERIMIENTO', 'BAJA',
       (SELECT id_usuario FROM usuarios WHERE correo = 'alumno@cibertec.edu.pe'),
       (SELECT id_categoria FROM categorias WHERE nombre = 'Software');

-- -----------------------------------------------------------------------------
-- Un ticket asignado de ejemplo (para probar la bandeja del técnico)
-- -----------------------------------------------------------------------------
UPDATE tickets
   SET id_tecnico = (SELECT id_usuario FROM usuarios WHERE correo = 'tecnico1@cibertec.edu.pe'),
       estado     = 'EN_PROCESO'
 WHERE asunto = 'Wi-Fi caído en laboratorio 3';
