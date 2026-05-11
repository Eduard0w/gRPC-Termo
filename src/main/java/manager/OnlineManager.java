package manager;

import br.com.ucsal.termo.grpc.LobbyStatusResponse;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class OnlineManager {
    private final AtomicInteger jogadoresOnline = new AtomicInteger(0);
    private final List<StreamObserver<LobbyStatusResponse>> observers = new CopyOnWriteArrayList<>();

    public int getJogadoresOnline() {
        return jogadoresOnline.get();
    }

    public void adicionarObserver(StreamObserver<LobbyStatusResponse> observer) {
        observers.add(observer);
        jogadoresOnline.incrementAndGet();

        // Inscricao no stream conta como presenca no site.
        // Quando o cliente fecha a aba, o cancel handler decrementa o total.
        if (observer instanceof ServerCallStreamObserver) {
            ServerCallStreamObserver<LobbyStatusResponse> serverObserver =
                    (ServerCallStreamObserver<LobbyStatusResponse>) observer;
            serverObserver.setOnCancelHandler(() -> {
                if (observers.remove(observer)) {
                    jogadoresOnline.updateAndGet(valor -> Math.max(0, valor - 1));
                    notificarObservers();
                }
            });
        }

        notificarObservers();
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
