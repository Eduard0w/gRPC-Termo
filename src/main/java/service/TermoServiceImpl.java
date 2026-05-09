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
import br.com.ucsal.termo.grpc.AssistirPartidaRequest;
import br.com.ucsal.termo.grpc.EstadoPartidaEspectadorResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import core.GameEngine;
import core.Partida;
import interceptor.AuthInterceptor;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import manager.LobbyManager;
import manager.OnlineManager;
import manager.PartidaManager;

public class TermoServiceImpl extends TermoGrpc.TermoImplBase {

    private final GameEngine engine;
    private final LobbyManager lobbyManager;
    private final PartidaManager partidaManager;
    private final Map<String, List<StreamObserver<EstadoPartidaEspectadorResponse>>> espectadoresPorPartida = new ConcurrentHashMap<>();
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
            responseObserver.onError(Status.UNAUTHENTICATED
                    .withDescription("Token invalido.")
                    .asRuntimeException());
            return;
        }

        Partida partida = partidaManager.buscarPartida(idPartida);
        if (partida == null) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Partida nao encontrada: " + idPartida)
                    .asRuntimeException());
            return;
        }

        // valida se jogador pertence à partida
        if (!partida.pertenceAPartida(idJogador)) {
            responseObserver.onError(Status.PERMISSION_DENIED
                    .withDescription("Jogador nao pertence a esta partida.")
                    .asRuntimeException());
            return;
        }

        String resultado = partida.jogada(idJogador, palavraChutada);

        if (resultado.equals("Palavra inválida.") || resultado.equals("Jogo acabou!")) {
            responseObserver.onError(Status.INVALID_ARGUMENT
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

        // notifica tentativa ao vivo apenas se o jogo continua
        if (!acertou && !empate) {
            int tentativasFeitas = 6 - partida.getTentativasRestantes(idJogador);
            partidaManager.notificarTentativa(idPartida, tentativasFeitas);
        }

        // espectadores sempre recebem atualização
        notificarEspectadores(idPartida, partida);

        // notifica oponente e limpa memória se partida finalizada

        // Notifica fim de jogo e limpa memória
        if (acertou) {
            partidaManager.notificarOponentes(idPartida, true,
                    "Seu oponente adivinhou a palavra! Voce perdeu.");
            partidaManager.removerPartida(idPartida);
            espectadoresPorPartida.remove(idPartida);
            onlineManager.jogadorDesconectou();
            onlineManager.jogadorDesconectou();
        } else if (empate) {
            partidaManager.notificarOponentes(idPartida, false,
                    "Empate! A palavra era: " + partida.getPalavraSecreta());
            partidaManager.removerPartida(idPartida);
            espectadoresPorPartida.remove(idPartida);
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
            responseObserver.onError(Status.NOT_FOUND
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
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Partida nao encontrada: " + idPartida)
                    .asRuntimeException());
            return;
        }

        if (!partida.pertenceAPartida(idJogador)) {
            responseObserver.onError(Status.PERMISSION_DENIED
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

    @Override
    public void assistirPartida(AssistirPartidaRequest request, StreamObserver<EstadoPartidaEspectadorResponse> responseObserver) {
        String idPartida = request.getIdPartida();
        Partida partida = partidaManager.buscarPartida(idPartida);

        if (partida == null) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Partida não encontrada.")
                    .asRuntimeException());
            return;
        }

        // adiciona espectador na lista
        espectadoresPorPartida.computeIfAbsent(idPartida, k -> new ArrayList<>()).add(responseObserver);

        // envia estado inicial imediatamente com idPartida correto
        responseObserver.onNext(montarRespostaEspectador(idPartida, partida));
    }

    // recebe idPartida como parâmetro
    private EstadoPartidaEspectadorResponse montarRespostaEspectador(String idPartida, Partida partida) {
        EstadoPartidaEspectadorResponse.Builder builder = EstadoPartidaEspectadorResponse.newBuilder()
                .setIdPartida(idPartida)
                .setTentativasRestantesJogador1(partida.getTentativasRestantes(partida.getId_jogador1()))
                .setTentativasRestantesJogador2(partida.getTentativasRestantes(partida.getId_jogador2()))
                .setFinalizada(partida.isFinalizada());

        for (int[] linha : partida.getHistoricoCores(partida.getId_jogador1())) {
            ResultadoCores.Builder coresBuilder = ResultadoCores.newBuilder();
            for (int c : linha) coresBuilder.addCores(c);
            builder.addHistoricoCoresJogador1(coresBuilder.build());
        }

        for (int[] linha : partida.getHistoricoCores(partida.getId_jogador2())) {
            ResultadoCores.Builder coresBuilder = ResultadoCores.newBuilder();
            for (int c : linha) coresBuilder.addCores(c);
            builder.addHistoricoCoresJogador2(coresBuilder.build());
        }

        return builder.build();
    }

    private void notificarEspectadores(String idPartida, Partida partida) {
        List<StreamObserver<EstadoPartidaEspectadorResponse>> observers = espectadoresPorPartida.get(idPartida);
        if (observers == null) return;

        EstadoPartidaEspectadorResponse resposta = montarRespostaEspectador(idPartida, partida);
        for (StreamObserver<EstadoPartidaEspectadorResponse> obs : observers) {
            try {
                obs.onNext(resposta);
                if (partida.isFinalizada()) {
                    obs.onCompleted();
                }
            } catch (Exception e) {
                observers.remove(obs);
            }
        }
    }

    private String gerarId() {
        return Long.toHexString(System.nanoTime()) +
                Integer.toHexString((int) (Math.random() * 0xFFFF));
    }
}