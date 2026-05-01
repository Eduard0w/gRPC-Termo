package core;

import java.util.ArrayList;
import java.util.List;

public class Partida {

    private GameEngine engine;

    private String palavraSecreta;
    private int tentativasRestantes;
    private boolean finalizada;
    private boolean venceu;

    private List<String> historicoChutes;
    private List<int[]> historicoCores;

    public Partida(GameEngine engine) {
        this.engine = engine;
        this.palavraSecreta = engine.palavraSorteada();
        this.tentativasRestantes = 6;
        this.finalizada = false;
        this.venceu = false;
        this.historicoChutes = new ArrayList<>();
        this.historicoCores = new ArrayList<>();

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

        // 3. Salva no histórico e diminui as tentativas
        historicoChutes.add(chute);
        historicoCores.add(cores);
        tentativasRestantes--;

        // 4. Verifica condição de vitória ou derrota
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
    }

