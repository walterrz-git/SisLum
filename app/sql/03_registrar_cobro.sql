-- Observaciones opcionales del cobro, como en el prototipo.
SET @existe_observaciones = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='cobro' AND column_name='observaciones');
SET @cambio_cobro = IF(@existe_observaciones=0,
    'ALTER TABLE cobro ADD COLUMN observaciones TEXT NULL',
    'SELECT 1');
PREPARE cambio_cobro FROM @cambio_cobro;
EXECUTE cambio_cobro;
DEALLOCATE PREPARE cambio_cobro;
