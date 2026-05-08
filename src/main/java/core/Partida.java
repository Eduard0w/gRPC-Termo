package core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;

@Getter
public class Partida {

    private GameEngine engine;
    private String palavraSecreta;
    private boolean finalizada;

    private String id_jogador1;
    private String id_jogador2;

    // Cada jogador tem seu próprio estado
    private int tentativasJ1 = 6;
    private int tentativasJ2 = 6;
    private boolean finalizadaJ1 = false;
    private boolean finalizadaJ2 = false;

    private List<String> historicoChutesJ1 = new ArrayList<>();
    private List<String> historicoChutesJ2 = new ArrayList<>();
    private List<int[]> historicoCoresJ1 = new ArrayList<>();
    private List<int[]> historicoCoresJ2 = new ArrayList<>();

    public Partida(GameEngine engine, String id_jogador1, String id_jogador2) {
        this.engine = engine;
        this.palavraSecreta = engine.palavraSorteada();
        this.finalizada = false;
        this.id_jogador1 = id_jogador1;
        this.id_jogador2 = id_jogador2;
        System.out.println("Palavra sorteada: " + this.palavraSecreta);
    }

    public synchronized String jogada(String idJogador, String chute) {
        if (finalizada) return "Jogo acabou!";

        boolean isJ1 = Objects.equals(idJogador, id_jogador1);
        boolean isJ2 = Objects.equals(idJogador, id_jogador2);

        if (!isJ1 && !isJ2) return "Jogador não pertence à partida.";

        int tentativas = isJ1 ? tentativasJ1 : tentativasJ2;
        boolean jaFinalizado = isJ1 ? finalizadaJ1 : finalizadaJ2;

        if (jaFinalizado) return "Jogo acabou!";
        if (!engine.validarPalavraChutada(chute)) return "Palavra inválida.";

        int[] cores = engine.avaliarCores(palavraSecreta, chute);

        if (isJ1) {
            historicoChutesJ1.add(chute);
            historicoCoresJ1.add(cores);
            tentativasJ1--;
        } else {
            historicoChutesJ2.add(chute);
            historicoCoresJ2.add(cores);
            tentativasJ2--;
        }

        if (chute.equals(palavraSecreta)) {
            if (isJ1) finalizadaJ1 = true;
            else finalizadaJ2 = true;
            finalizada = true;
            return "VITORIA";
        }

        int tentativasRestantes = isJ1 ? tentativasJ1 : tentativasJ2;
        if (tentativasRestantes <= 0) {
            if (isJ1) finalizadaJ1 = true;
            else finalizadaJ2 = true;
            // Empate: ambos sem tentativas
            if (finalizadaJ1 && finalizadaJ2) {
                finalizada = true;
                return "EMPATE";
            }
            return "DERROTA";
        }

        return "CONTINUA";
    }

    public int getTentativasRestantes(String idJogador) {
        return Objects.equals(idJogador, id_jogador1) ? tentativasJ1 : tentativasJ2;
    }

    public List<int[]> getHistoricoCores(String idJogador) {
        return Objects.equals(idJogador, id_jogador1) ? historicoCoresJ1 : historicoCoresJ2;
    }

    public boolean PertenceAPartida(String id_jogador) {
        return Objects.equals(id_jogador, id_jogador1) || Objects.equals(id_jogador, id_jogador2);
    }
}