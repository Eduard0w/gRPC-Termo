package core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class GameEngine {
    /*
    TODO: Lista de palavras; leitura dessa lista; sortear palavra (para cada partida);
     validar tentativa (se a palavra chutada faz sentido/existe); comparar palavra chutada com a escolhida;
     checar condição de vitoria
     */
    private List<String> palavrasPossiveis = new ArrayList<>();
    //setBuscas foi adicionado afim de ser mais rápido
    // nas buscas de palavras para validação da palavra chutada pelo usuário
    // Pois ele armazena cada palavra num bloco de memória especifico através de um cálculo (hash), facilitando a busca.
    private Set<String> setBuscas = new HashSet<>();
    private Random random = new Random();

    public void pegarPalavrasDisponiveis() {
        InputStream dicionario = getClass().getResourceAsStream("/dicionario.txt");

        // Checa se é nulo antes de tentar abrir o leitor
        if (dicionario == null) {
            System.err.println("Erro: Arquivo dicionario.txt não encontrado na pasta resources!");
            return;
        }

        // Guarda o que está escrito no dicionário.txt em um ‘buffer’ temporário na memória
        try (BufferedReader br = new BufferedReader(new InputStreamReader(dicionario))) {

            String linha;
            // Lê a linha, guarda na variável, e verifica se é nula ao mesmo tempo
            while ((linha = br.readLine()) != null) {
                palavrasPossiveis.add(linha.trim().toUpperCase());
                setBuscas.add(linha.trim().toUpperCase());
            }

            System.out.println("Dicionário carregado com " + palavrasPossiveis.size() + " palavras.");

        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo: " + e.getMessage());
        }
    }

    public boolean validarPalavraChutada(String chute) {
        return chute != null && chute.length() == 5 && setBuscas.contains(chute);
    }

    public String palavraSorteada() {
        int indice = random.nextInt(palavrasPossiveis.size());
        return palavrasPossiveis.get(indice);
    }

    int[] avaliarCores(String palavraSorteada, String palavraChute) {
        int[] resultado = new int[5];
        int[] mapaFrequencia = new int[26];
        /*
        if palavraChute.contains(palavraSorteada[n]) && palavraChute[n] == palavraSorteada[n] -> adiciona em um array o valor correspondente a essa letra(nesse caso 2, porque ele representa o verde)
         */
        for(int i = 0; i < 5; i++){
            char letraSecreta = palavraSorteada.charAt(i);
            char letraChute = palavraChute.charAt(i);

            if(letraChute == letraSecreta) {
                //nesse caso 2 = verde
                resultado[i] = 2;
            }else {
                //Ele vai na posição da letra do chute no array e armazena quantas vezes ela aparece na palavraChute
                mapaFrequencia[letraSecreta - 'A']++;
            }
        }

        //Array para definir as letras que ficarão amarelas
        for(int i = 0; i < 5; i++){
            if(resultado[i] == 2) continue;

            char letraChute = palavraChute.charAt(i);
            if(mapaFrequencia[letraChute - 'A'] > 0) {
                //nesse caso 1 = amarelo
                resultado[i] = 1;
                mapaFrequencia[letraChute - 'A']--;
            }
        }
        return resultado;
    }


    public boolean checarVitoria(String palavraSorteada, String palavraChutada) {
        if (!validarPalavraChutada(palavraChutada)) {
            System.err.println("palavra invalida");
            return false;
        }
        return palavraSorteada.equals(palavraChutada);
    }
}
