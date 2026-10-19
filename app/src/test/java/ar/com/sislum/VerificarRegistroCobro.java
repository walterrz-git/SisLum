package ar.com.sislum;

import ar.com.sislum.controlador.*;
import ar.com.sislum.modelo.*;
import ar.com.sislum.persistencia.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

// Comprueba el circuito completo con datos temporales propios.
public final class VerificarRegistroCobro {
    private interface Operacion { void ejecutar() throws Exception; }

    private static void comprobar(boolean condicion,String mensaje) {
        if(!condicion) { throw new AssertionError(mensaje); }
    }

    private static void rechazar(Operacion operacion) throws Exception {
        try { operacion.ejecutar(); }
        catch(IllegalArgumentException esperado) { return; }
        throw new AssertionError("Se aceptó un cobro inválido.");
    }

    public static void main(String[] args) throws Exception {
        Paciente paciente=null;
        List<Integer> catalogoTemporal=new ArrayList<>();
        try {
            String dni="98400000";
            PacienteDAO pacientes=new PacienteDAO();
            while(pacientes.buscarPorDni(dni)!=null) { dni=String.valueOf(Integer.parseInt(dni)+1); }
            paciente=new PacienteController().registrar(dni,"Prueba","Cobros","","");
            final Paciente registrado=paciente;
            Tratamiento primero=crearTratamiento("Prueba cobro A",new BigDecimal("10.50"));
            catalogoTemporal.add(primero.getIdTratamiento());
            Tratamiento segundo=crearTratamiento("Prueba cobro B",new BigDecimal("20.25"));
            catalogoTemporal.add(segundo.getIdTratamiento());
            CobroController cobros=new CobroController();
            AtencionController atenciones=new AtencionController();
            TurnoController turnos=new TurnoController();
            CobroDAO cobroDAO=new CobroDAO();
            Profesional profesional=turnos.consultarProfesionales().get(0);
            LocalDate dia=LocalDate.now().plusWeeks(8).with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
            List<LocalTime> horas=turnos.consultarDisponibilidad(profesional,dia);
            comprobar(horas.size()>=5,"No hay suficientes horarios para probar.");
            List<Atencion> realizadas=new ArrayList<>();
            comprobar(cobros.consultarImporte(paciente).isEmpty(),"El paciente nuevo no debe tener deuda.");
            for(int i=0;i<5;i++) {
                Turno turno=turnos.registrarTurno(paciente,profesional,primero,dia,horas.get(i));
                List<TratamientoRealizado> detalles=i==0
                        ? List.of(new TratamientoRealizado(0,primero,primero.getPrecio(),"Detalle A"),
                                  new TratamientoRealizado(0,segundo,segundo.getPrecio(),"Detalle B"))
                        : List.of(new TratamientoRealizado(0,segundo,segundo.getPrecio(),""));
                realizadas.add(atenciones.registrarAtencion(turno,"Informe de prueba de cobro",detalles));
            }
            Atencion primera=realizadas.get(0);
            comprobar(cobros.buscarPaciente(registrado.getDni()).stream().anyMatch(p->p.getIdPaciente()==registrado.getIdPaciente()),"No encuentra al paciente.");
            comprobar(cobros.consultarImporte(paciente).size()==5,"No lista todas las atenciones pendientes.");
            comprobar(cobros.consultarImporte(dia,paciente.getDni()).size()==5,"No filtra por fecha del turno y DNI.");
            comprobar(cobros.consultarImporte(dia,"").stream().anyMatch(a->a.getIdAtencion()==primera.getIdAtencion()),"No consulta por fecha sin paciente.");
            comprobar(cobros.consultarImporte(dia.plusDays(1),paciente.getDni()).isEmpty(),"Incluye turnos de otra fecha.");
            comprobar(cobros.consultarImporte(dia,"DNI_INEXISTENTE").isEmpty(),"No aplica el filtro del paciente.");
            rechazar(()->cobros.consultarImporte(null));
            rechazar(()->cobros.registrarCobro(null,"EFECTIVO",""));
            rechazar(()->cobros.registrarCobro(primera,null,""));
            rechazar(()->cobros.registrarCobro(primera,"CHEQUE",""));
            rechazar(()->cobros.registrarCobro(primera,"EFECTIVO","x".repeat(15001)));
            rechazar(()->new Cobro(0,primera,new BigDecimal("-1"),"EFECTIVO",LocalDate.now(),"PAGADO",""));
            rechazar(()->new Cobro(0,primera,new BigDecimal("1.001"),"EFECTIVO",LocalDate.now(),"PAGADO",""));
            rechazar(()->cobroDAO.guardar(new Cobro(0,primera,new BigDecimal("0.01"),"EFECTIVO",LocalDate.now(),"PAGADO","Importe cambiado")));
            comprobar(cobroDAO.buscarPorAtencion(primera.getIdAtencion())==null,"Quedó un cobro parcial.");
            Atencion inexistente=new Atencion(Integer.MAX_VALUE,primera.getIdHistoria(),primera.getTurno(),
                    primera.getFecha(),primera.getInforme(),primera.getTratamientos());
            rechazar(()->cobros.registrarCobro(inexistente,"EFECTIVO",""));
            Paciente ajeno=new Paciente(Integer.MAX_VALUE,"12345678","Otro","Paciente","","");
            Turno otroTurno=new Turno(primera.getTurno().getIdTurno(),ajeno,primera.getTurno().getAgenda(),
                    primero,primera.getTurno().getFechaHora(),"ATENDIDO");
            Atencion ajena=new Atencion(primera.getIdAtencion(),primera.getIdHistoria(),otroTurno,primera.getFecha(),
                    primera.getInforme(),primera.getTratamientos());
            rechazar(()->cobros.registrarCobro(ajena,"EFECTIVO",""));

            // Cambiar el catálogo no modifica lo que se debe por una atención anterior.
            try(Connection c=ConexionBD.obtenerConexion();PreparedStatement s=c.prepareStatement("UPDATE tratamiento SET precio=99.99 WHERE id_tratamiento=?")) {
                s.setInt(1,primero.getIdTratamiento());s.executeUpdate();
            }
            Cobro guardado=cobros.registrarCobro(primera,"EFECTIVO","Cobro de prueba · efectivo");
            comprobar(guardado.getIdCobro()>0 && guardado.getEstado().equals("PAGADO"),"No registró el cobro.");
            comprobar(guardado.getImporteTotal().compareTo(new BigDecimal("30.75"))==0,"No usó el importe histórico.");
            Cobro recuperado=new CobroDAO().buscarPorAtencion(primera.getIdAtencion());
            comprobar(recuperado.getIdCobro()==guardado.getIdCobro()
                    && recuperado.getMedioPago().equals("EFECTIVO") && recuperado.getFecha().equals(LocalDate.now())
                    && recuperado.getObservaciones().equals("Cobro de prueba · efectivo"),"No recupera los datos del cobro.");
            rechazar(()->cobros.registrarCobro(primera,"CREDITO","Duplicado"));
            comprobar(cobros.consultarImporte(dia,paciente.getDni()).size()==4,"El cobro pagado sigue en el filtro por fecha.");
            comprobar(cobros.consultarPorEstado(dia,paciente.getDni(),"PAGADO").size()==1,"No aparece el cobro pagado.");
            comprobar(cobros.consultarPorEstado(dia,paciente.getDni(),"TODOS").size()==5,"Todos debe incluir pagados y pendientes.");
            comprobar(cobros.consultarPorEstado(dia.plusDays(1),paciente.getDni(),"PAGADO").isEmpty(),"Pagados ignora la fecha del turno.");
            comprobar(cobros.consultarPorEstado(dia,"DNI_INEXISTENTE","TODOS").isEmpty(),"Todos ignora el paciente.");
            comprobar(cobros.consultarCobro(primera.getIdAtencion()).getIdCobro()==guardado.getIdCobro(),"No recupera el detalle del cobro.");
            rechazar(()->cobros.consultarPorEstado(dia,"","INVALIDO"));
            comprobar(cobros.consultarImporte(paciente).stream().noneMatch(a->a.getIdAtencion()==primera.getIdAtencion()),"La atención cobrada sigue pendiente.");

            Atencion simultanea=realizadas.get(1);
            CountDownLatch inicio=new CountDownLatch(1);
            ExecutorService hilos=Executors.newFixedThreadPool(2);
            Callable<Boolean> cobrar=()->{
                inicio.await();
                try {new CobroController().registrarCobro(simultanea,"TRANSFERENCIA","");return true;}
                catch(IllegalArgumentException esperado) {return false;}
            };
            try {
                Future<Boolean> uno=hilos.submit(cobrar),dos=hilos.submit(cobrar);inicio.countDown();
                comprobar(uno.get(20,TimeUnit.SECONDS)!=dos.get(20,TimeUnit.SECONDS),"Se duplicó un cobro simultáneo.");
            }finally{hilos.shutdownNow();hilos.awaitTermination(20,TimeUnit.SECONDS);}
            comprobar(contarCobros(simultanea.getIdAtencion())==1,"Hay más de un cobro por atención.");

            // Un registro PENDIENTE se completa conservando su identificador.
            Atencion pendiente=realizadas.get(2);int idPendiente;
            try(Connection c=ConexionBD.obtenerConexion();PreparedStatement s=c.prepareStatement(
                    "INSERT INTO cobro(id_atencion,fecha,importe_total,medio_pago,estado,observaciones) VALUES (?,CURRENT_DATE,1.00,'EFECTIVO','PENDIENTE','Sin confirmar')",Statement.RETURN_GENERATED_KEYS)) {
                s.setInt(1,pendiente.getIdAtencion());s.executeUpdate();
                try(ResultSet claves=s.getGeneratedKeys()){claves.next();idPendiente=claves.getInt(1);}
            }
            rechazar(()->cobroDAO.guardar(new Cobro(0,pendiente,new BigDecimal("0.01"),"CREDITO",LocalDate.now(),"PAGADO","")));
            comprobar(cobroDAO.buscarPorAtencion(pendiente.getIdAtencion()).getEstado().equals("PENDIENTE"),"El error modificó el registro pendiente.");
            Cobro completado=cobros.registrarCobro(pendiente,"CREDITO","Confirmado");
            comprobar(completado.getIdCobro()==idPendiente && contarCobros(pendiente.getIdAtencion())==1,"Duplicó el registro pendiente.");
            cobros.registrarCobro(realizadas.get(3),"DEBITO","");
            comprobar(cobros.consultarImporte(paciente).size()==1,"No queda únicamente la atención sin cobrar.");
            comprobar(atenciones.consultarImportePendiente(paciente).compareTo(new BigDecimal("20.25"))==0,"El saldo pendiente no se actualizó.");
            comprobar(atenciones.consultarHistoria(paciente).getAtenciones().size()==5,"El cobro modificó la historia clínica.");
            comprobar(new TurnoDAO().buscarPorId(primera.getTurno().getIdTurno()).getEstado().equals("ATENDIDO"),"El cobro cambió el estado médico del turno.");
            System.out.println("OK: circuito completo, cuatro medios de pago, persistencia, importes históricos, pendientes, rollback y cobros simultáneos.");
        }finally{
            if(paciente!=null)limpiar(paciente.getIdPaciente());
            for(int id:catalogoTemporal)try(Connection c=ConexionBD.obtenerConexion();PreparedStatement s=c.prepareStatement("DELETE FROM tratamiento WHERE id_tratamiento=?")){s.setInt(1,id);s.executeUpdate();}
        }
    }

    private static Tratamiento crearTratamiento(String nombre,BigDecimal precio)throws SQLException {
        try(Connection c=ConexionBD.obtenerConexion();PreparedStatement s=c.prepareStatement(
                "INSERT INTO tratamiento(nombre,descripcion,precio) VALUES (?,'Prueba temporal',?)",Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1,nombre);s.setBigDecimal(2,precio);s.executeUpdate();
            try(ResultSet r=s.getGeneratedKeys()){r.next();return new Tratamiento(r.getInt(1),nombre,"Prueba temporal",precio);}
        }
    }

    private static int contarCobros(int idAtencion)throws SQLException {
        try(Connection c=ConexionBD.obtenerConexion();PreparedStatement s=c.prepareStatement("SELECT COUNT(*) FROM cobro WHERE id_atencion=?")) {
            s.setInt(1,idAtencion);try(ResultSet r=s.executeQuery()){r.next();return r.getInt(1);}
        }
    }

    private static void limpiar(int idPaciente)throws SQLException {
        try(Connection c=ConexionBD.obtenerConexion()) {
            c.setAutoCommit(false);
            String[] consultas={
                "DELETE c FROM cobro c JOIN atencion a ON a.id_atencion=c.id_atencion JOIN historia_clinica h ON h.id_historia=a.id_historia WHERE h.id_paciente=?",
                "DELETE d FROM tratamiento_realizado d JOIN atencion a ON a.id_atencion=d.id_atencion JOIN historia_clinica h ON h.id_historia=a.id_historia WHERE h.id_paciente=?",
                "DELETE a FROM atencion a JOIN historia_clinica h ON h.id_historia=a.id_historia WHERE h.id_paciente=?",
                "DELETE FROM turno WHERE id_paciente=?","DELETE FROM historia_clinica WHERE id_paciente=?","DELETE FROM paciente WHERE id_paciente=?"};
            for(String sql:consultas)try(PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,idPaciente);s.executeUpdate();}
            c.commit();
        }
    }
}
