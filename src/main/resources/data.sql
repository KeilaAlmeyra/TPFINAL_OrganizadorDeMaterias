-- Aulas: cada una se inserta solo si todavía no existe, así que no pisa
-- los cambios hechos desde la pantalla /espacios.
INSERT INTO espacio (id, nombre, capacidad_maxima, tipo_espacio, estado)
SELECT 'AULA-1', 'Aula 1', 40, 'aula', 'disponible'
WHERE NOT EXISTS (SELECT 1 FROM espacio WHERE id = 'AULA-1');

INSERT INTO espacio (id, nombre, capacidad_maxima, tipo_espacio, estado)
SELECT 'COMP-1', 'Aula de computación 1', 30, 'computadoras', 'disponible'
WHERE NOT EXISTS (SELECT 1 FROM espacio WHERE id = 'COMP-1');

INSERT INTO espacio (id, nombre, capacidad_maxima, tipo_espacio, estado)
SELECT 'LAB-1', 'Laboratorio 1', 25, 'laboratorio', 'disponible'
WHERE NOT EXISTS (SELECT 1 FROM espacio WHERE id = 'LAB-1');

-- Materias: ejemplo, reemplazá por las reales y descomentá.
-- INSERT INTO materias (id, nombre, carga_horaria, troncal, anio_carrera, tipo_espacio_frecuente)
-- SELECT 'MAT-1', 'Programación I', 6, TRUE, 1, 'computadoras'
-- WHERE NOT EXISTS (SELECT 1 FROM materias WHERE id = 'MAT-1');