package service;

import br.com.ucsal.termo.grpc.TermoGrpc;
import br.com.ucsal.termo.grpc.EventoPartida;
import br.com.ucsal.termo.grpc.LobbyResponse;
import br.com.ucsal.termo.grpc.JogadorRequest;
import br.com.ucsal.termo.grpc.TentativaRequest;
import br.com.ucsal.termo.grpc.TentativaResponse;
import br.com.ucsal.termo.grpc.PartidaRequest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

import core.GameEngine;
import core.Partida;
import io.grpc.stub.StreamObserver;

public class TermoServiceImpl extends TermoGrpc.TermoImplBase {
    private final GameEngine engine;
    private final Map<String, Partida> partidas = new ConcurrentHashMap<>();
    private final Map<String, List<StreamObserver<EventoPartida>>> observers = new ConcurrentHashMap<>();

    private String jogadorEsperandoId = null;
    private String jogadorEsperandoNome = null;
    private StreamObserver<LobbyResponse> jogadorEsperandoObserver = null;

    public TermoServiceImpl() {
        this.engine = new GameEngine();
        this.engine.pegarPalavrasDisponiveis();
    }

    @Override
    public void conectar(JogadorRequest request,
                         StreamObserver<LobbyResponse> responseObserver) {
        String nomeJogador = request.getNome();

        if (jogadorEsperandoId == null) {
            jogadorEsperandoId = gerarId();
            jogadorEsperandoNome = nomeJogador;
            jogadorEsperandoObserver = responseObserver;

        } else {
            String idPartida = gerarId();
            String idJogador1 = jogadorEsperandoId;
            String idJogador2 = gerarId();

            Partida novaPartida = new Partida(engine, idJogador1, idJogador2);
            partidas.put(idPartida, novaPartida);
            observers.put(idPartida, new CopyOnWriteArrayList<>());

            LobbyResponse respostaJ1 = LobbyResponse.newBuilder()
                    .setIdJogador1(idJogador1)
                    .setIdPartida(idPartida)
                    .setNomeOponente(nomeJogador)
                    .build();
            jogadorEsperandoObserver.onNext(respostaJ1);
            jogadorEsperandoObserver.onCompleted();

            LobbyResponse respostaJ2 = LobbyResponse.newBuilder()
                    .setIdJogador1(idJogador2)
                    .setIdPartida(idPartida)
                    .setNomeOponente(jogadorEsperandoNome)
                    .build();
            responseObserver.onNext(respostaJ2);
            responseObserver.onCompleted();

            jogadorEsperandoId = null;
            jogadorEsperandoNome = null;
            jogadorEsperandoObserver = null;
        }
    }

    @Override
    public void enviarPalavra(TentativaRequest request, StreamObserver<TentativaResponse> responseObserver) {
        String idPartida = request.getIdPartida();
        String palavraChutada = request.getPalavraChutada().toUpperCase();

        Partida partida = partidas.get(idPartida);

        if (partida == null) {
            responseObserver.onError(
                    io.grpc.Status.NOT_FOUND
                            .withDescription("Partida nao encontrada: " + idPartida)
                            .asRuntimeException()
            );
            return;
        }

        String resultado = partida.jogada(palavraChutada);

        if (resultado.equals("Palavra inválida.") || resultado.equals("Jogo acabou!")) {
            responseObserver.onError(
                    io.grpc.Status.INVALID_ARGUMENT
                            .withDescription(resultado)
                            .asRuntimeException()
            );
            return;
        }

        boolean acertou = resultado.equals("VITORIA");
        boolean derrota = resultado.equals("DERROTA");

        List<int[]> historico = partida.getHistoricoCores();
        int[] cores = historico.get(historico.size() - 1);

        TentativaResponse.Builder builder = TentativaResponse.newBuilder()
                .setAcertou(acertou)
                .setTentativasRestantes(partida.getTentativasRestantes());

        for (int cor : cores) {
            builder.addCores(cor);
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();

        if (acertou) {
            notificarOponentes(idPartida, true, "Seu oponente adivinhou a palavra! Voce perdeu.");
        } else if (derrota) {
            notificarOponentes(idPartida, false, "Seu oponente ficou sem tentativas! Continue jogando.");
        }
    }

    @Override
    public void monitorarPartida(PartidaRequest request, StreamObserver<EventoPartida> responseObserver) {
        String idPartida = request.getIdPartida();

        if (!partidas.containsKey(idPartida)) {
            responseObserver.onError(
                    io.grpc.Status.NOT_FOUND
                            .withDescription("Partida nao encontrada: " + idPartida)
                            .asRuntimeException()
            );
            return;
        }

        observers.get(idPartida).add(responseObserver);
    }

    private void notificarOponentes(String idPartida, boolean oponenteGanhou, String mensagem) {
        List<StreamObserver<EventoPartida>> lista = observers.get(idPartida);
        if (lista == null) return;

        EventoPartida evento = EventoPartida.newBuilder()
                .setOponenteGanhou(oponenteGanhou)
                .setMensagem(mensagem)
                .build();

        for (StreamObserver<EventoPartida> obs : lista) {
            try {
                obs.onNext(evento);
                if (oponenteGanhou) {
                    obs.onCompleted();
                }
            } catch (Exception e) {
                lista.remove(obs);
            }
        }
    }

    private String gerarId() {
        return Long.toHexString(System.nanoTime()) +
                Integer.toHexString((int) (Math.random() * 0xFFFF));
    }
}