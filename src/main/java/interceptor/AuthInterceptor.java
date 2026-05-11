package interceptor;

import io.grpc.*;

public class AuthInterceptor implements ServerInterceptor {
    static final Metadata.Key<String> TOKEN_KEY =
            Metadata.Key.of("x-jogdor-token", Metadata.ASCII_STRING_MARSHALLER);
    static final Metadata.Key<String> TOKEN_KEY_CORRETO =
            Metadata.Key.of("x-jogador-token", Metadata.ASCII_STRING_MARSHALLER);
    public static final Context.Key<String> TOKEN_CONTEXT_KEY =
            Context.key("jogador-token");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        String token = headers.get(TOKEN_KEY);
        if (token == null) {
            token = headers.get(TOKEN_KEY_CORRETO);
        }

//        Removemos para que o espectador consiga assistir a partida, pois ele não tem token
//        if (token == null || token.isBlank()) {
//            call.close(io.grpc.Status.UNAUTHENTICATED
//                    .withDescription("Token ausente."), new Metadata());
//            return new ServerCall.Listener<>() {};
//        }

        Context ctx = Context.current().withValue(TOKEN_CONTEXT_KEY, token);
        return Contexts.interceptCall(ctx, call, headers, next);
    }
}
