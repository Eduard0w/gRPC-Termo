package manager;

import core.GameEngine;
import core.Partida;
import br.com.ucsal.termo.grpc.LobbyResponse;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class LobbyManager {
    private final Object lobbyLock = new Object();

    private String jogadorEsperandoId = null;
    private String jogadorEsperandoNome = null;
    private StreamObserver<LobbyResponse> jogadorEsperandoObserver = null;

    public Partida tentarParear(String idJogador, String nomeJogador,
                                StreamObserver<LobbyResponse> responseObserver,
                                GameEngine gameEngine, PartidaManager partidaManager) {
        synchronized (lobbyLock) {
            if (jogadorEsperandoId == null) {
                jogadorEsperandoId = idJogador;
                jogadorEsperandoNome = nomeJogador;
                jogadorEsperandoObserver = responseObserver;
                return null;
            } else {
                String idPartida = gerarId();
                String idJogador1 = jogadorEsperandoId;
                String idJogador2 = idJogador;

                Partida novaPartida = new Partida(gameEngine, idJogador1, idJogador2);
                partidaManager.registrarPartida(idPartida, novaPartida);

                jogadorEsperandoObserver.onNext(LobbyResponse.newBuilder()
                        .setIdJogador1(idJogador1)
                        .setIdPartida(idPartida)
                        .setNomeOponente(nomeJogador)
                        .setNomeOponente(jogadorEsperandoNome)
                        .build());
                responseObserver.onCompleted();

                jogadorEsperandoId = null;
                jogadorEsperandoNome = null;
                jogadorEsperandoObserver = null;

                return novaPartida;
            }
        }
    }

    private String gerarId() {
        return Long.toHexString(System.nanoTime()) +
                Integer.toHexString((int) (Math.random() * 0xFFFF));
    }

    public int quantidadeEsperando() {
        synchronized (lobbyLock) {
            return jogadorEsperandoId != null ? 1 : 0;
        }
    }
}
