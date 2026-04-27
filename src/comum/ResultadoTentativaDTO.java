package comum;

import java.io.Serializable;

//DTO dos dados que serão enviados para o servidor e recebidos pelo cliente.
public class ResultadoTentativaDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    //EX.: [0, 1, 2, 0, 0]
    // 0 = sem cor, normal,
    // 1 = amarela (letra existe, mas posição errada),
    // 2 = verde (letra existe e está na posição certa);
    private int[] corTentativa = new int[5];
    private boolean status;
//    private String mensagemErro;

    public ResultadoTentativaDTO(int[] tentativa, boolean status){
        this.corTentativa = tentativa;
        this.status = status;
    }

    public int[] getCorTentativa() {
        return corTentativa;
    }

    public void setCorTentativa(int[] corTentativa) {
        this.corTentativa = corTentativa;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
