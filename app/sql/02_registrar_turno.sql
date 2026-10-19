-- El tratamiento del turno es el previsto, no el realizado en la atención.
SET @existe_columna = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'turno' AND column_name = 'id_tratamiento');
SET @cambio = IF(@existe_columna = 0,
    'ALTER TABLE turno ADD COLUMN id_tratamiento INT NULL, ADD CONSTRAINT fk_turno_tratamiento FOREIGN KEY (id_tratamiento) REFERENCES tratamiento(id_tratamiento)',
    'SELECT 1');
PREPARE cambio_turno FROM @cambio;
EXECUTE cambio_turno;
DEALLOCATE PREPARE cambio_turno;

START TRANSACTION;
-- Datos ficticios para probar el circuito.
INSERT INTO profesional (nombre, apellido, matricula, especialidad)
SELECT 'Ana', 'López', 'DEMO-1001', 'Medicina estética'
WHERE NOT EXISTS (SELECT 1 FROM profesional WHERE matricula = 'DEMO-1001');
INSERT INTO profesional (nombre, apellido, matricula, especialidad)
SELECT 'Martín', 'Gómez', 'DEMO-1002', 'Dermatología'
WHERE NOT EXISTS (SELECT 1 FROM profesional WHERE matricula = 'DEMO-1002');
INSERT INTO profesional (nombre, apellido, matricula, especialidad)
SELECT 'Lucía', 'Fernández', 'DEMO-1003', 'Medicina regenerativa'
WHERE NOT EXISTS (SELECT 1 FROM profesional WHERE matricula = 'DEMO-1003');

INSERT INTO agenda (id_profesional)
SELECT p.id_profesional FROM profesional p
WHERE p.matricula IN ('DEMO-1001','DEMO-1002','DEMO-1003')
  AND NOT EXISTS (SELECT 1 FROM agenda a WHERE a.id_profesional = p.id_profesional);

INSERT INTO tratamiento (nombre, descripcion, precio)
SELECT 'Consulta médica', 'Evaluación inicial. Importe ficticio para demostración.', 25000.00
WHERE NOT EXISTS (SELECT 1 FROM tratamiento WHERE nombre = 'Consulta médica');
INSERT INTO tratamiento (nombre, descripcion, precio)
SELECT 'Peeling facial', 'Tratamiento facial. Importe ficticio para demostración.', 45000.00
WHERE NOT EXISTS (SELECT 1 FROM tratamiento WHERE nombre = 'Peeling facial');
INSERT INTO tratamiento (nombre, descripcion, precio)
SELECT 'Plasma rico en plaquetas', 'Tratamiento estético. Importe ficticio para demostración.', 80000.00
WHERE NOT EXISTS (SELECT 1 FROM tratamiento WHERE nombre = 'Plasma rico en plaquetas');
COMMIT;
