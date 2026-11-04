package ar.edu.utn.frba.dds.donaciones.domain;

import javax.persistence.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Representa las credenciales y rol de acceso en la plataforma web (Entrega 4).
 * Puede estar vinculado a una PersonaDonante, a una EntidadBeneficiaria o ser un perfil ADMIN.
 */
@Entity
@Table(name = "usuario")
public class Usuario {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "nombre_mostrar")
  private String nombreMostrar;

  @Enumerated(EnumType.STRING)
  @Column(name = "rol", nullable = false)
  private RolUsuario rol;

  @ManyToOne
  @JoinColumn(name = "donante_id")
  private PersonaDonante donante;

  @ManyToOne
  @JoinColumn(name = "entidad_id")
  private EntidadBeneficiaria entidadBeneficiaria;

  protected Usuario() {}

  public Usuario(String email, String passwordPlana, String nombreMostrar,
                 RolUsuario rol, PersonaDonante donante, EntidadBeneficiaria entidadBeneficiaria) {
    this.email = email != null ? email.trim().toLowerCase() : null;
    this.passwordHash = hashear(passwordPlana);
    this.nombreMostrar = nombreMostrar;
    this.rol = rol;
    this.donante = donante;
    this.entidadBeneficiaria = entidadBeneficiaria;
  }

  public boolean verificarPassword(String passwordPlana) {
    if (passwordPlana == null) {
      return false;
    }
    return this.passwordHash.equals(hashear(passwordPlana));
  }

  public static String hashear(String texto) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest((texto == null ? "" : texto).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("No se encontró algoritmo SHA-256", e);
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email != null ? email.trim().toLowerCase() : null;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getNombreMostrar() {
    return nombreMostrar;
  }

  public void setNombreMostrar(String nombreMostrar) {
    this.nombreMostrar = nombreMostrar;
  }

  public RolUsuario getRol() {
    return rol;
  }

  public PersonaDonante getDonante() {
    return donante;
  }

  public void setDonante(PersonaDonante donante) {
    this.donante = donante;
  }

  public EntidadBeneficiaria getEntidadBeneficiaria() {
    return entidadBeneficiaria;
  }

  public void setEntidadBeneficiaria(EntidadBeneficiaria entidadBeneficiaria) {
    this.entidadBeneficiaria = entidadBeneficiaria;
  }
}
