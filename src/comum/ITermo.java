package comum;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ITermo extends Remote {
    //Esses métodos vai ser chamado pelos dois jogadores, então ele precisa ser sincronyzed
    //Ele retorna o id do jogador que vai ser gerado ao conectar com o servidor.
    Long conectar(String jogador) throws RemoteException;

    // Retorna um int[]: 0 (Errado), 1 (Lugar Errado), 2 (Certo) (Siga as cores normais do TERMO)
    int[] compararPalavra(Long idJogador, String Palavra) throws RemoteException;

    boolean buscarPartida(Long idJogador) throws RemoteException;


    // checka se o outro jogador vençeu a partida, ou seja o status geral da partida.
    boolean statusPartida(Long idJogador) throws RemoteException;


}
