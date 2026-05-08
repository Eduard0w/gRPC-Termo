import interceptor.AuthInterceptor;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import service.TermoServiceImpl;

public class Main {
    public static void main(String[] args) throws Exception{
        Server servidor = ServerBuilder
                .forPort(50051)
                .addService(new TermoServiceImpl())
                .intercept(new AuthInterceptor())
                .build();
        servidor.start();
        System.out.println("Servidor iniciado na porta 50051");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Encerrando servidor.");
            servidor.shutdown();
            System.out.println("Servidor encerrado na porta 50051");
        }));

        servidor.awaitTermination();
    }
}