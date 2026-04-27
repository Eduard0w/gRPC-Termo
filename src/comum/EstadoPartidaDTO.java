package comum;

import java.io.Serializable;

//DTO dos dados que serão enviados para o servidor e recebidos pelo cliente.
public class EstadoPartidaDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private statusPartida status;
    private String oponente;
    private String palavraCorreta; //aparece quando o jogo acabou

    public EstadoPartidaDTO(statusPartida status, String oponente){
        this.status = status;
        this.oponente = oponente;
    }

    public statusPartida getStatus() {
        return status;
    }

    public void setStatus(statusPartida status) {
        this.status = status;
    }

    public String getOponente() {
        return oponente;
    }

    public void setOponente(String oponente) {
        this.oponente = oponente;
    }

    public String getPalavraCorreta() {
        return palavraCorreta;
    }

    public void setPalavraCorreta(String palavraCorreta) {
        this.palavraCorreta = palavraCorreta;
    }
}
