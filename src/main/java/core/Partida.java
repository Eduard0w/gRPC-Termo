package core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.Getter;

@Getter
public class Partida {

    private GameEngine engine;

    private String palavraSecreta;
    private int tentativasRestantes;
    private boolean finalizada;
    private boolean venceu;
    private String id_jogador1;
    private String id_jogador2;

    private List<String> historicoChutes;
    private List<int[]> historicoCores;

    public Partida(GameEngine engine, String id_jogador1, String id_jogador2) {
        this.engine = engine;
        this.palavraSecreta = engine.palavraSorteada();
        this.tentativasRestantes = 6;
        this.finalizada = false;
        this.venceu = false;
        this.historicoChutes = new ArrayList<>();
        this.historicoCores = new ArrayList<>();
        this.id_jogador1 = id_jogador1;
        this.id_jogador2 = id_jogador2;

        System.out.println("Palavra sorteada: " + this.palavraSecreta);
    }

    public String jogada(String chute) {
        if(finalizada) {
            return "Jogo acabou!";
        }

        if(!engine.validarPalavraChutada(chute)){
            return "Palavra não valida";
        }

        int[] cores = engine.avaliarCores(palavraSecreta, chute);

        // Salva no histórico e diminui as tentativas
        historicoChutes.add(chute);
        historicoCores.add(cores);
        tentativasRestantes--;

        // Verifica condição de vitória ou derrota
        if (chute.equals(palavraSecreta)) {
            venceu = true;
            finalizada = true;
            return "VITORIA";
        } else if (tentativasRestantes <= 0) {
            finalizada = true;
            return "DERROTA";
        }

        return "CONTINUA";
    }

    public boolean PertenceAPartida(String id_jogador) {
        if(!Objects.equals(id_jogador, this.id_jogador1) && !Objects.equals(id_jogador, this.id_jogador2)){
            System.out.println("Esse jogador não pertence a partida");
            return false;
        }

        return true;
    }
