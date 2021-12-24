package ru.bmstu;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.Query;
import akka.japi.Pair;
import akka.pattern.Patterns;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.apache.*;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class PingApp {
    public static final int SERVER_PORT = 8080;
    public static final int OK_CODE = 200;
    public static ActorRef cache;
    public static ActorMaterializer materializer;
    public static final String DEFAULT_URL = "https://google.ru/";

    public static Pair<String, Integer> makePair(HttpRequest request) {
        Query query = request.getUri().query();
        String url = query.get("testURL").orElse(DEFAULT_URL);
        String count = query.get("count").orElse("10");
        return new Pair<String, Integer>(url, Integer.parseInt(count));
    }

    private static CompletionStage <Object> asy (Pair<String, Integer> pair) {
        return Patterns.ask(cache, pair, Duration.ofMillis(5000)).thenCompose(
                result-> {
                    long responceTime = ((CacheResponse) result).getTime();
                    if (responceTime > 0) {
                        return CompletableFuture.completedFuture(new Pair<>(pair.first(), responceTime));
                    }
                    return Source.from(Collections.singletonList(pair))
                            .toMat(
                                    testSink(),
                                    Keep.right()
                            )
                            .run(materializer)
                            .thenCompose( sum ->
                                    CompletableFuture.completedFuture(
                                            new Pair<>(pair.first(), sum / pair.second())
                                    )

                            );
                });
    }

    private static Sink<Pair<String, Integer>, CompletionStage<Long>> testSink() {
        return Flow
                .<Pair<String, Integer>>create()
                .mapConcat(request -> Collections.nCopies(request.second(), request.first()))
                .mapAsync(5,
                        url -> {
                            long start = System.currentTimeMillis();
                            AsyncHttpClient async = asyncHttpClient();
                            return async
                                    .prepareGet(url)
                                    .execute()
                                    .toCompletableFuture()
                                    .thenCompose (request ->
                                            CompletableFuture.completedFuture(System.currentTimeMillis() - start));
                        })
                .toMat(Sink.fold(0L, Long::sum), Keep.right());
    }
    public static void main(String[] args) throws IOException {
        System.out.println("started!");
        ActorSystem system = ActorSystem.create("routes");
        cache = system.actorOf(Props.create(CacheActor.class));
        final Http http = Http.get(system);
        materializer = ActorMaterializer.create(system);
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = Flow
                .of(HttpRequest.class)
                .map(PingApp::makePair)
                .mapAsync(5, PingApp::asy)
                .map(result -> {
                    Pair<String, Long> pair = (Pair<String, Long>) result;
                    cache.tell(new StoreRequest(pair.first(), pair.second()), ActorRef.noSender());
                    System.out.printf("url: %s ping: %d", pair.first(), pair.second());
                    return HttpResponse.create().withStatus(OK_CODE).withEntity(pair.second().toString());
                });
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(
                routeFlow,
                ConnectHttp.toHost("localhost", SERVER_PORT),
                materializer
        );
        System.out.println("Server online at http://localhost:8080/\nPress RETURN to stop...");
        System.in.read();
        binding
                .thenCompose(ServerBinding::unbind)
                .thenAccept(unbound -> system.terminate()); // and shutdown when done
    }
}
