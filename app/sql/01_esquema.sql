-- Tablas para registrar turno, atención y cobro, con sus datos de soporte.
-- Las tablas de seguridad se conservan en futuros-casos-de-uso.
-- No elimina tablas ni registros existentes.

CREATE DATABASE IF NOT EXISTS SisLum CHARACTER SET utf8mb4;

USE SisLum;

CREATE TABLE IF NOT EXISTS `profesional` (
  `id_profesional` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  `apellido` varchar(100) NOT NULL,
  `matricula` varchar(50) NOT NULL,
  `especialidad` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id_profesional`),
  UNIQUE KEY `matricula` (`matricula`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `paciente` (
  `id_paciente` int NOT NULL AUTO_INCREMENT,
  `dni` varchar(20) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `apellido` varchar(100) NOT NULL,
  `telefono` varchar(30) DEFAULT NULL,
  `email` varchar(150) DEFAULT NULL,
  PRIMARY KEY (`id_paciente`),
  UNIQUE KEY `dni` (`dni`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `historia_clinica` (
  `id_historia` int NOT NULL AUTO_INCREMENT,
  `id_paciente` int NOT NULL,
  PRIMARY KEY (`id_historia`),
  UNIQUE KEY `id_paciente` (`id_paciente`),
  CONSTRAINT `fk_historia_paciente` FOREIGN KEY (`id_paciente`) REFERENCES `paciente` (`id_paciente`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `agenda` (
  `id_agenda` int NOT NULL AUTO_INCREMENT,
  `id_profesional` int NOT NULL,
  PRIMARY KEY (`id_agenda`),
  UNIQUE KEY `id_profesional` (`id_profesional`),
  CONSTRAINT `fk_agenda_profesional` FOREIGN KEY (`id_profesional`) REFERENCES `profesional` (`id_profesional`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `tratamiento` (
  `id_tratamiento` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(150) NOT NULL,
  `descripcion` text,
  `precio` decimal(12,2) NOT NULL DEFAULT '0.00',
  PRIMARY KEY (`id_tratamiento`),
  CONSTRAINT `chk_tratamiento_precio` CHECK ((`precio` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `turno` (
  `id_turno` int NOT NULL AUTO_INCREMENT,
  `id_paciente` int NOT NULL,
  `id_agenda` int NOT NULL,
  `fecha` date NOT NULL,
  `hora` time NOT NULL,
  `estado` enum('DISPONIBLE','CONFIRMADO','CANCELADO','ATENDIDO') NOT NULL DEFAULT 'CONFIRMADO',
  PRIMARY KEY (`id_turno`),
  KEY `fk_turno_paciente` (`id_paciente`),
  KEY `idx_turno_fecha` (`fecha`),
  KEY `idx_turno_agenda_fecha` (`id_agenda`,`fecha`,`hora`),
  CONSTRAINT `fk_turno_agenda` FOREIGN KEY (`id_agenda`) REFERENCES `agenda` (`id_agenda`),
  CONSTRAINT `fk_turno_paciente` FOREIGN KEY (`id_paciente`) REFERENCES `paciente` (`id_paciente`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `atencion` (
  `id_atencion` int NOT NULL AUTO_INCREMENT,
  `id_turno` int NOT NULL,
  `id_historia` int NOT NULL,
  `id_profesional` int NOT NULL,
  `fecha` date NOT NULL,
  `informe` text NOT NULL,
  PRIMARY KEY (`id_atencion`),
  UNIQUE KEY `id_turno` (`id_turno`),
  KEY `fk_atencion_historia` (`id_historia`),
  KEY `fk_atencion_profesional` (`id_profesional`),
  CONSTRAINT `fk_atencion_historia` FOREIGN KEY (`id_historia`) REFERENCES `historia_clinica` (`id_historia`),
  CONSTRAINT `fk_atencion_profesional` FOREIGN KEY (`id_profesional`) REFERENCES `profesional` (`id_profesional`),
  CONSTRAINT `fk_atencion_turno` FOREIGN KEY (`id_turno`) REFERENCES `turno` (`id_turno`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `tratamiento_realizado` (
  `id_detalle` int NOT NULL AUTO_INCREMENT,
  `id_atencion` int NOT NULL,
  `id_tratamiento` int NOT NULL,
  `importe` decimal(12,2) NOT NULL,
  `observaciones` text,
  PRIMARY KEY (`id_detalle`),
  KEY `fk_detalle_atencion` (`id_atencion`),
  KEY `fk_detalle_tratamiento` (`id_tratamiento`),
  CONSTRAINT `fk_detalle_atencion` FOREIGN KEY (`id_atencion`) REFERENCES `atencion` (`id_atencion`),
  CONSTRAINT `fk_detalle_tratamiento` FOREIGN KEY (`id_tratamiento`) REFERENCES `tratamiento` (`id_tratamiento`),
  CONSTRAINT `chk_detalle_importe` CHECK ((`importe` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `cobro` (
  `id_cobro` int NOT NULL AUTO_INCREMENT,
  `id_atencion` int NOT NULL,
  `fecha` date NOT NULL,
  `importe_total` decimal(12,2) NOT NULL,
  `medio_pago` enum('EFECTIVO','TRANSFERENCIA','DEBITO','CREDITO') NOT NULL,
  `estado` enum('PENDIENTE','PAGADO') NOT NULL DEFAULT 'PAGADO',
  PRIMARY KEY (`id_cobro`),
  UNIQUE KEY `id_atencion` (`id_atencion`),
  CONSTRAINT `fk_cobro_atencion` FOREIGN KEY (`id_atencion`) REFERENCES `atencion` (`id_atencion`),
  CONSTRAINT `chk_cobro_importe` CHECK ((`importe_total` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
