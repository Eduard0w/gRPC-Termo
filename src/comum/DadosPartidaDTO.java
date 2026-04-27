package comum;

import java.io.Serializable;

//DTO dos dados que serão enviados para o servidor e recebidos pelo cliente.
public class DadosPartidaDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String oponente;
    private Long idPartida;
    private statusPartida status;

    public DadosPartidaDTO(String oponente, Long idPartida, statusPartida status){
        this.oponente = oponente;
        this.idPartida = idPartida;
        this.status = status;
    }

    public String getOponente() {
        return oponente;
    }

    public void setOponente(String oponente) {
        this.oponente = oponente;
    }

    public Long getIdPartida() {
        return idPartida;
    }

    public void setIdPartida(Long idPartida) {
        this.idPartida = idPartida;
    }

    public statusPartida getStatus() {
        return status;
    }

    public void setStatus(statusPartida status) {
        this.status = status;
    }
}
