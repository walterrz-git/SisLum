-- Prueba de entorno: la tabla temporal se elimina al cerrar la conexión.
SELECT VERSION() AS version_mysql, CURRENT_USER() AS usuario, DATABASE() AS base;
CREATE TEMPORARY TABLE prueba_entorno (
    id INT PRIMARY KEY,
    descripcion VARCHAR(100) NOT NULL
);
INSERT INTO prueba_entorno VALUES (1, 'Conexión de SisLum funcionando');
SELECT * FROM prueba_entorno;
DELETE FROM prueba_entorno WHERE id = 1;
SELECT COUNT(*) AS registros_despues_de_borrar FROM prueba_entorno;
DROP TEMPORARY TABLE prueba_entorno;
