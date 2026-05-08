package manager;

import br.com.ucsal.termo.grpc.LobbyStatusResponse;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class OnlineManager {
    private final AtomicInteger jogadoresOnline = new AtomicInteger(0);
    private final List<StreamObserver<LobbyStatusResponse>> observers = new CopyOnWriteArrayList<>();

    public void jogadorConectou() {
        jogadoresOnline.incrementAndGet();
        notificarObservers();
    }

    public void jogadorDesconectou() {
        jogadoresOnline.decrementAndGet();
        notificarObservers();
    }

    public int getJogadoresOnline() {
        return jogadoresOnline.get();
    }

    public void adicionarObserver(StreamObserver<LobbyStatusResponse> observer) {
        observers.add(observer);
        // Envia o valor atual imediatamente ao se inscrever
        try {
            observer.onNext(buildResponse());
        } catch (Exception e) {
            observers.remove(observer);
        }
    }

    private void notificarObservers() {
        LobbyStatusResponse response = buildResponse();
        for (StreamObserver<LobbyStatusResponse> obs : observers) {
            try {
                obs.onNext(response);
            } catch (Exception e) {
                observers.remove(obs);
            }
        }
    }

    private LobbyStatusResponse buildResponse() {
        return LobbyStatusResponse.newBuilder()
                .setQuantidadeJogadores(jogadoresOnline.get())
                .build();
    }
}
