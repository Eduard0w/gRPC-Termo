package manager;

import core.Partida;
import br.com.ucsal.termo.grpc.EventoPartida;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class PartidaManager {
    private final Map<String, Partida> partidas = new ConcurrentHashMap<>();
    private final Map<String, List<StreamObserver<EventoPartida>>> observers = new ConcurrentHashMap<>();

    public void registrarPartida(String idPartida, Partida partida){
        partidas.put(idPartida, partida);
        observers.put(idPartida, new CopyOnWriteArrayList<>());
    }

    public Partida buscarPartida(String idPartida){
        return partidas.get(idPartida);
    }

    public boolean existePartida(String idPartida){
        return partidas.containsKey(idPartida);
    }

    public void addObserver(String idPartida, StreamObserver<EventoPartida> observer){
        List<StreamObserver<EventoPartida>> lista = observers.get(idPartida);
        if(lista == null) lista.add(observer);
    }

    public void notificarOponentes(String idPartida, boolean oponenteGanhou, String mensagem){
        List<StreamObserver<EventoPartida>> lista = observers.get(idPartida);
        if(lista == null) return;

        EventoPartida evento = EventoPartida.newBuilder()
                .setOponenteGanhou(oponenteGanhou)
                .setMensagem(mensagem)
                .build();

        for (StreamObserver<EventoPartida> obs : lista) {
            try{
                obs.onNext(evento);
                obs.onCompleted();
            } catch (Exception e){
                lista.remove(obs);
            }
        }
    }

    public void removerPartida(String idPartida) {
        partidas.remove(idPartida);
        observers.remove(idPartida);
    }
}
