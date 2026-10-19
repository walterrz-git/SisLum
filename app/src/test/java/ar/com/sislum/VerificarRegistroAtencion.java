package ar.com.sislum;

import ar.com.sislum.controlador.*;
import ar.com.sislum.modelo.*;
import ar.com.sislum.persistencia.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

// Prueba el caso completo con registros propios que se eliminan al terminar.
public final class VerificarRegistroAtencion {
    private static void comprobar(boolean condicion,String mensaje) {
        if(!condicion) { throw new AssertionError(mensaje); }
    }
    private interface Operacion { void ejecutar() throws Exception; }
    private static void rechazar(Operacion operacion) throws Exception {
        try { operacion.ejecutar(); } catch(IllegalArgumentException esperado) { return; }
        throw new AssertionError("La operación inválida fue aceptada.");
    }

    public static void main(String[] args) throws Exception {
        Paciente paciente=null;
        List<Integer> tratamientosTemporales=new ArrayList<>();
        try {
            String dni="98700000";
            PacienteDAO pacientes=new PacienteDAO();
            while(pacientes.buscarPorDni(dni)!=null) { dni=String.valueOf(Integer.parseInt(dni)+1); }
            paciente=new PacienteController().registrar(dni,"Prueba","Atencion","","");
            final Paciente registrado=paciente;
            Tratamiento primero=crearTratamiento("Prueba atención A",new BigDecimal("10.50"));
            tratamientosTemporales.add(primero.getIdTratamiento());
            Tratamiento segundo=crearTratamiento("Prueba atención B",new BigDecimal("20.25"));
            tratamientosTemporales.add(segundo.getIdTratamiento());
            List<TratamientoRealizado> detalles=List.of(
                    new TratamientoRealizado(0,primero,primero.getPrecio(),"Detalle de prueba"),
                    new TratamientoRealizado(0,segundo,segundo.getPrecio(),"Segundo tratamiento"));
            TurnoController turnos=new TurnoController();
            AtencionController atenciones=new AtencionController();
            Profesional profesional=turnos.consultarProfesionales().get(0);
            LocalDate fecha=LocalDate.now().plusWeeks(4).with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
            List<LocalTime> horas=turnos.consultarDisponibilidad(profesional,fecha);
            comprobar(horas.size()>=3,"Faltan horarios de prueba.");
            Turno turno=turnos.registrarTurno(paciente,profesional,primero,fecha,horas.get(0));
            comprobar(atenciones.consultarHistoria(paciente).getAtenciones().isEmpty(),"La historia debe comenzar vacía.");
            comprobar(turnos.consultarAgenda(null,fecha,horas.get(0)).stream().anyMatch(t->t.getIdTurno()==turno.getIdTurno()),"Falla el filtro de hora.");
            Profesional otro=turnos.consultarProfesionales().get(1);
            comprobar(turnos.consultarAgenda(otro,fecha,null).stream().noneMatch(t->t.getIdTurno()==turno.getIdTurno()),"Falla el filtro profesional.");
            comprobar(turnos.consultarAgenda(null,LocalDate.now().minusDays(1),null)!=null,"No permite consultar días pasados.");
            rechazar(()->atenciones.registrarAtencion(turno,"",detalles));
            rechazar(()->atenciones.registrarAtencion(turno,"Informe",List.of()));
            rechazar(()->atenciones.registrarAtencion(turno,"Informe",List.of(detalles.get(0),detalles.get(0))));
            Tratamiento inexistente=new Tratamiento(Integer.MAX_VALUE,"No existe","",BigDecimal.ONE);
            List<TratamientoRealizado> incompletos=List.of(detalles.get(0),new TratamientoRealizado(0,inexistente,BigDecimal.ONE,""));
            rechazar(()->atenciones.registrarAtencion(turno,"No debe guardarse",incompletos));
            comprobar(atenciones.consultarHistoria(paciente).getAtenciones().isEmpty(),"Quedó una atención parcial.");
            comprobar(new TurnoDAO().buscarPorId(turno.getIdTurno()).getEstado().equals("CONFIRMADO"),"El error alteró el turno.");
            Atencion guardada=atenciones.registrarAtencion(turno,"Informe de la atención de prueba",detalles);
            comprobar(guardada.getIdAtencion()>0,"Falta el identificador.");
            comprobar(guardada.getImporteTotal().compareTo(new BigDecimal("30.75"))==0,"Total incorrecto.");
            comprobar(new TurnoDAO().buscarPorId(turno.getIdTurno()).getEstado().equals("ATENDIDO"),"No cambió el estado.");
            comprobar(atenciones.consultarTurnosAsignados(profesional,fecha).stream().noneMatch(t->t.getIdTurno()==turno.getIdTurno()),"El atendido sigue en pendientes.");
            rechazar(()->atenciones.registrarAtencion(turno,"Otra atención",detalles));
            try(Connection c=ConexionBD.obtenerConexion();PreparedStatement s=c.prepareStatement("UPDATE tratamiento SET precio=99.99 WHERE id_tratamiento=?")) {
                s.setInt(1,primero.getIdTratamiento());s.executeUpdate();
            }
            HistoriaClinica historia=new AtencionController().consultarHistoria(paciente);
            comprobar(historia.getAtenciones().size()==1 && historia.getAtenciones().get(0).getTratamientos().size()==2,"No recupera la historia completa.");
            comprobar(historia.getAtenciones().get(0).getImporteTotal().compareTo(new BigDecimal("30.75"))==0,"Cambió el importe histórico.");
            comprobar(atenciones.consultarImportePendiente(paciente).compareTo(new BigDecimal("30.75"))==0,"Pendiente incorrecto.");
            try(Connection c=ConexionBD.obtenerConexion();PreparedStatement s=c.prepareStatement("SELECT COUNT(*) FROM cobro WHERE id_atencion=?")) {
                s.setInt(1,guardada.getIdAtencion());try(ResultSet r=s.executeQuery()) {r.next();comprobar(r.getInt(1)==0,"Se registró un pago sin autorización.");}
            }
            Turno simultaneo=turnos.registrarTurno(paciente,profesional,segundo,fecha,horas.get(1));
            CountDownLatch inicio=new CountDownLatch(1);
            ExecutorService hilos=Executors.newFixedThreadPool(2);
            Callable<Boolean> tarea=()->{
                inicio.await();
                try {new AtencionController().registrarAtencion(simultaneo,"Atención simultánea",List.of(detalles.get(1)));return true;}
                catch(IllegalArgumentException esperado) {return false;}
            };
            try {
                Future<Boolean> una=hilos.submit(tarea),otra=hilos.submit(tarea);inicio.countDown();
                comprobar(una.get(20,TimeUnit.SECONDS)!=otra.get(20,TimeUnit.SECONDS),"Se duplicó la atención simultánea.");
            }finally{hilos.shutdownNow();hilos.awaitTermination(20,TimeUnit.SECONDS);}
            Turno cancelado=turnos.registrarTurno(paciente,profesional,segundo,fecha,horas.get(2));
            try(Connection c=ConexionBD.obtenerConexion();PreparedStatement s=c.prepareStatement("UPDATE turno SET estado='CANCELADO' WHERE id_turno=?")) {
                s.setInt(1,cancelado.getIdTurno());s.executeUpdate();
            }
            rechazar(()->atenciones.registrarAtencion(cancelado,"Cancelado",detalles));
            comprobar(atenciones.consultarHistoria(registrado).getAtenciones().size()==2,"Cantidad final incorrecta.");
            System.out.println("OK: filtros de agenda, atención completa, antecedentes, importe histórico, pendientes, rollback y atención simultánea.");
        }finally{
            if(paciente!=null)limpiar(paciente.getIdPaciente());
            for(int id:tratamientosTemporales)try(Connection c=ConexionBD.obtenerConexion();PreparedStatement s=c.prepareStatement("DELETE FROM tratamiento WHERE id_tratamiento=?")) {s.setInt(1,id);s.executeUpdate();}
        }
    }

    private static Tratamiento crearTratamiento(String nombre,BigDecimal precio) throws SQLException {
        try(Connection c=ConexionBD.obtenerConexion();PreparedStatement s=c.prepareStatement(
                "INSERT INTO tratamiento(nombre,descripcion,precio) VALUES (?,'Prueba temporal',?)",Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1,nombre);s.setBigDecimal(2,precio);s.executeUpdate();
            try(ResultSet r=s.getGeneratedKeys()) {r.next();return new Tratamiento(r.getInt(1),nombre,"Prueba temporal",precio);}
        }
    }
    private static void limpiar(int idPaciente) throws SQLException {
        try(Connection c=ConexionBD.obtenerConexion()) {
            c.setAutoCommit(false);
            String[] consultas={
                "DELETE d FROM tratamiento_realizado d JOIN atencion a ON a.id_atencion=d.id_atencion JOIN historia_clinica h ON h.id_historia=a.id_historia WHERE h.id_paciente=?",
                "DELETE a FROM atencion a JOIN historia_clinica h ON h.id_historia=a.id_historia WHERE h.id_paciente=?",
                "DELETE FROM turno WHERE id_paciente=?","DELETE FROM historia_clinica WHERE id_paciente=?","DELETE FROM paciente WHERE id_paciente=?"};
            for(String sql:consultas)try(PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,idPaciente);s.executeUpdate();}
            c.commit();
        }
    }
}
