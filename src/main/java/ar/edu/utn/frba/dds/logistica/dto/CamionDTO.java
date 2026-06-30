package ar.edu.utn.frba.dds.logistica.dto;

import ar.edu.utn.frba.dds.logistica.domain.Camion;

public record CamionDTO(
        Long id,
        String patente,
        double volumenM3,
        double alturaM,
        double capacidadKg
) {
    public static CamionDTO fromDomain(Camion camion) {
        return new CamionDTO(
                camion.getId(),
                camion.getPatente(),
                camion.getVolumenM3(),
                camion.getAlturaM(),
                camion.getCapacidadKg()
        );
    }

    public Camion toDomain() {
        Camion camion = new Camion(patente, volumenM3, alturaM, capacidadKg);
        if (id != null) {
            camion.setId(id);
        }
        return camion;
    }
}
