package ar.edu.utn.frba.dds;

public class Main {
  public static void main(String[] args) {
    System.out.println("Tarea programada");

    DonacionRepository repoDonaciones = new DonacionRepository();
    EntidadRepository repoEntidades = new EntidadRepository();
    PropuestaRepository repoPropuestas = new PropuestaRepository();
    ProcesadorMatchmaking procesador = new ProcesadorMatchmaking();

    TareaNocturnaMatchmaking tarea = new TareaNocturnaMatchmaking(
        repoDonaciones, repoEntidades, repoPropuestas, procesador
    );

    // Inicio de la tarea programada para producción
    //tarea.iniciarShedulerProduccion();

  }
}