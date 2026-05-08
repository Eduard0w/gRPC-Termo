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


import core.GameEngine;
import core.Partida;
import interceptor.AuthInterceptor;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import manager.LobbyManager;
import manager.OnlineManager;
import manager.PartidaManager;

public class TermoServiceImpl extends TermoGrpc.TermoImplBase {

    private final GameEngine engine;
    private final LobbyManager lobbyManager;
    private final PartidaManager partidaManager;
    private final OnlineManager onlineManager;

    public TermoServiceImpl() {
        this.engine = new GameEngine();
        this.engine.pegarPalavrasDisponiveis();
        this.lobbyManager = new LobbyManager();
        this.partidaManager = new PartidaManager();
        this.onlineManager = new OnlineManager();
    }

    @Override
    public void conectar(JogadorRequest request, StreamObserver<LobbyResponse> responseObserver) {
        String idJogador = gerarId();
        String nomeJogador = request.getNome();

        onlineManager.jogadorConectou();

        lobbyManager.tentarParear(idJogador, nomeJogador, responseObserver, engine, partidaManager);
    }

    @Override
    public void enviarPalavra(TentativaRequest request, StreamObserver<TentativaResponse> responseObserver) {
        String idPartida = request.getIdPartida();
        String idJogador = request.getIdJogador1();
        String palavraChutada = request.getPalavraChutada().toUpperCase();

        // valida token
        String token = AuthInterceptor.TOKEN_CONTEXT_KEY.get(Context.current());
        if (token == null || !token.equals(idJogador)) {
            responseObserver.onError(io.grpc.Status.UNAUTHENTICATED
                    .withDescription("Token invalido.")
                    .asRuntimeException());
            return;
        }

        Partida partida = partidaManager.buscarPartida(idPartida);
        if (partida == null) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription("Partida nao encontrada: " + idPartida)
                    .asRuntimeException());
            return;
        }

        // valida se jogador pertence à partida
        if (!partida.pertenceAPartida(idJogador)) {
            responseObserver.onError(io.grpc.Status.PERMISSION_DENIED
                    .withDescription("Jogador nao pertence a esta partida.")
                    .asRuntimeException());
            return;
        }

        String resultado = partida.jogada(idJogador, palavraChutada);

        if (resultado.equals("Palavra inválida.") || resultado.equals("Jogo acabou!")) {
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                    .withDescription(resultado)
                    .asRuntimeException());
            return;
        }

        boolean acertou = resultado.equals("VITORIA");
        boolean derrota = resultado.equals("DERROTA");
        boolean empate  = resultado.equals("EMPATE");

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

        // Notifica tentativa ao vivo (só se o jogo continua)
        if (!acertou && !empate) {
            int tentativasFeitas = 6 - partida.getTentativasRestantes(idJogador);
            partidaManager.notificarTentativa(idPartida, tentativasFeitas);
        }

        // Notifica fim de jogo e limpa memória
        if (acertou) {
            partidaManager.notificarOponentes(idPartida, true,
                    "Seu oponente adivinhou a palavra! Voce perdeu.");
            partidaManager.removerPartida(idPartida);
            onlineManager.jogadorDesconectou();
            onlineManager.jogadorDesconectou();
        } else if (empate) {
            partidaManager.notificarOponentes(idPartida, false,
                    "Empate! A palavra era: " + partida.getPalavraSecreta());
            partidaManager.removerPartida(idPartida);
            onlineManager.jogadorDesconectou();
            onlineManager.jogadorDesconectou();
        } else if (derrota) {
            partidaManager.notificarOponentes(idPartida, false,
                    "Seu oponente ficou sem tentativas! Continue jogando.");
        }
    }

    @Override
    public void monitorarPartida(PartidaRequest request, StreamObserver<EventoPartida> responseObserver) {
        String idPartida = request.getIdPartida();

        if (!partidaManager.existePartida(idPartida)) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription("Partida nao encontrada: " + idPartida)
                    .asRuntimeException());
            return;
        }

        partidaManager.addObserver(idPartida, responseObserver);
    }

    @Override
    public void buscarEstadoPartida(PartidaRequest request, StreamObserver<EstadoPartidaResponse> responseObserver) {
        String idPartida = request.getIdPartida();
        String idJogador = request.getIdJogador();

        Partida partida = partidaManager.buscarPartida(idPartida);
        if (partida == null) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription("Partida nao encontrada: " + idPartida)
                    .asRuntimeException());
            return;
        }

        if (!partida.pertenceAPartida(idJogador)) {
            responseObserver.onError(io.grpc.Status.PERMISSION_DENIED
                    .withDescription("Jogador nao pertence a esta partida.")
                    .asRuntimeException());
            return;
        }

        EstadoPartidaResponse.Builder builder = EstadoPartidaResponse.newBuilder()
                .setTentativasRestantes(partida.getTentativasRestantes(idJogador))
                .setFinalizada(partida.isFinalizada());

        builder.addAllHistoricoChutes(partida.getHistoricoChutes(idJogador));

        for (int[] linha : partida.getHistoricoCores(idJogador)) {
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
        onlineManager.adicionarObserver(responseObserver);
    }

    private String gerarId() {
        return Long.toHexString(System.nanoTime()) +
                Integer.toHexString((int) (Math.random() * 0xFFFF));
    }
}