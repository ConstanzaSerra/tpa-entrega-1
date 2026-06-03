package ar.edu.utn.frba.dds;

public enum Categoria {
  //estos true y false, son porque, un alimento puede vencer pero no puede tener el estado de nuevo o usado (antes estaba en los tests)
  ALIMENTOS(true, false),
  MOBILIARIO(false, true),
  VESTIMENTA(false, true);
  private final boolean esPerecedero;
  private final boolean requiereEstado;

  Categoria(boolean esPerecedero, boolean requiereEstado) {
    this.esPerecedero = esPerecedero;
    this.requiereEstado = requiereEstado;
  }

  public boolean esPerecedero() { return esPerecedero; }
  public boolean requiereEstado() { return requiereEstado; }
}
