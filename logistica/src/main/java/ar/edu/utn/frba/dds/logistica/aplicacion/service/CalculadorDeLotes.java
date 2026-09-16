package ar.edu.utn.frba.dds.logistica.aplicacion.service;

import java.util.ArrayList;
import java.util.List;

public class CalculadorDeLotes {
    public static <T> List<List<T>> agrupar(List<T> elementos, int tamanioLote) {
        List<List<T>> lotes = new ArrayList<>();
        int cantidadLotes = (int) Math.ceil((double) elementos.size() / tamanioLote);
        
        for (int i = 0; i < cantidadLotes; i++) {
            int start = i * tamanioLote;
            int end = Math.min(start + tamanioLote, elementos.size());
            lotes.add(new ArrayList<>(elementos.subList(start, end)));
        }
        return lotes;
    }
}
