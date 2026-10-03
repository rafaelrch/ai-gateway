package br.com.rafael.aigateway.provider;

public class PerfilNaoSuportadoException extends RuntimeException {

  public PerfilNaoSuportadoException(Perfil perfil) {
    super("Nenhum provedor para o perfil " + perfil);
  }
}
