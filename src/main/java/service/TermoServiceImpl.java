package service;

import br.com.ucsal.termo.grpc.TermoGrpc;
import br.com.ucsal.termo.grpc.EventoPartida;
import br.com.ucsal.termo.grpc.LobbyResponse;
import br.com.ucsal.termo.grpc.JogadorRequest;
import br.com.ucsal.termo.grpc.TentativaRequest;
import br.com.ucsal.termo.grpc.TentativaResponse;
import br.com.ucsal.termo.grpc.PartidaRequest;
import br.com.ucsal.termo.grpc.EstadoPartidaResponse;
import br.com.ucsal.termo.grpc.ResultadoCores;
import br.com.ucsal.termo.grpc.LobbyRequest;
import br.com.ucsal.termo.grpc.LobbyStatusResponse;
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
    private final Object lobbyLock = new Object();

    private String jogadorEsperandoId = null;
    private String jogadorEsperandoNome = null;
    private StreamObserver<LobbyResponse> jogadorEsperandoObserver = null;

    public TermoServiceImpl() {
        this.engine = new GameEngine();
        this.engine.pegarPalavrasDisponiveis();
    }

    @Override
    public void conectar(JogadorRequest request, StreamObserver<LobbyResponse> responseObserver) {
        String nomeJogador = request.getNome();

        synchronized (lobbyLock) {
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

                jogadorEsperandoObserver.onNext(LobbyResponse.newBuilder()
                        .setIdJogador1(idJogador1)
                        .setIdPartida(idPartida)
                        .setNomeOponente(nomeJogador)
                        .build());
                jogadorEsperandoObserver.onCompleted();

                responseObserver.onNext(LobbyResponse.newBuilder()
                        .setIdJogador1(idJogador2)
                        .setIdPartida(idPartida)
                        .setNomeOponente(jogadorEsperandoNome)
                        .build());
                responseObserver.onCompleted();

                jogadorEsperandoId = null;
                jogadorEsperandoNome = null;
                jogadorEsperandoObserver = null;
            }
        }
    }

    @Override
    public void enviarPalavra(TentativaRequest request, StreamObserver<TentativaResponse> responseObserver) {
        String idPartida = request.getIdPartida();
        String idJogador = request.getIdJogador1();
        String palavraChutada = request.getPalavraChutada().toUpperCase();

        Partida partida = partidas.get(idPartida);
        if (partida == null) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription("Partida nao encontrada: " + idPartida)
                    .asRuntimeException());
            return;
        }

        // ✅ Passa o idJogador para controle individual de tentativas
        String resultado = partida.jogada(idJogador, palavraChutada);

        if (resultado.equals("Palavra inválida.") || resultado.equals("Jogo acabou!")
                || resultado.equals("Jogador não pertence à partida.")) {
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                    .withDescription(resultado)
                    .asRuntimeException());
            return;
        }

        boolean acertou = resultado.equals("VITORIA");
        boolean derrota = resultado.equals("DERROTA");
        boolean empate = resultado.equals("EMPATE");

        List<int[]> historico = partida.getHistoricoCores(idJogador);
        int[] cores = historico.get(historico.size() - 1);

        TentativaResponse.Builder builder = TentativaResponse.newBuilder()
                .setAcertou(acertou)
                .setTentativasRestantes(partida.getTentativasRestantes(idJogador));

        if (acertou || empate) {
            builder.setPalavraSecreta(partida.getPalavraSecreta());
        }

        for (int cor : cores) builder.addCores(cor);

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();

        if (acertou) {
            notificarOponentes(idPartida, true, "Seu oponente adivinhou a palavra! Voce perdeu.");
        } else if (empate) {
            notificarOponentes(idPartida, false, "Empate! A palavra era: " + partida.getPalavraSecreta());
        } else if (derrota) {
            notificarOponentes(idPartida, false, "Seu oponente ficou sem tentativas! Continue jogando.");
        }
    }

    @Override
    public void monitorarPartida(PartidaRequest request, StreamObserver<EventoPartida> responseObserver) {
        String idPartida = request.getIdPartida();
        if (!partidas.containsKey(idPartida)) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription("Partida nao encontrada: " + idPartida)
                    .asRuntimeException());
            return;
        }
        observers.get(idPartida).add(responseObserver);
    }

    @Override
    public void buscarEstadoPartida(PartidaRequest request, StreamObserver<EstadoPartidaResponse> responseObserver) {
        String idPartida = request.getIdPartida();
        String idJogador = request.getIdJogador();

        Partida partida = partidas.get(idPartida);
        if (partida == null) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription("Partida nao encontrada: " + idPartida)
                    .asRuntimeException());
            return;
        }

        EstadoPartidaResponse.Builder builder = EstadoPartidaResponse.newBuilder()
                .setTentativasRestantes(partida.getTentativasRestantes(idJogador))
                .setFinalizada(partida.isFinalizada());

        // Histórico de chutes
        List<String> chutes = partida.PertenceAPartida(idJogador)
                ? (idJogador.equals(partida.getId_jogador1())
                ? partida.getHistoricoChutesJ1()
                : partida.getHistoricoChutesJ2())
                : List.of();
        builder.addAllHistoricoChutes(chutes);

        // Histórico de cores
        List<int[]> cores = partida.getHistoricoCores(idJogador);
        for (int[] linha : cores) {
            ResultadoCores.Builder coresBuilder = ResultadoCores.newBuilder();
            for (int c : linha) coresBuilder.addCores(c);
            builder.addHistoricoCores(coresBuilder.build());
        }

        if (partida.isFinalizada()) {
            builder.setPalavraSecreta(partida.getPalavraSecreta());
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void jogadoresDisponiveis(LobbyRequest request, StreamObserver<LobbyStatusResponse> responseObserver) {
        // Envia quantidade atual e mantém stream aberto (simplificado)
        int quantidade = jogadorEsperandoId != null ? 1 : 0;
        responseObserver.onNext(LobbyStatusResponse.newBuilder()
                .setQuantidadeJogadores(quantidade)
                .build());
        // Stream fica aberto — seria atualizado via push quando o lobby mudar
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
                obs.onCompleted();
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