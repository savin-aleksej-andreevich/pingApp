package ru.bmstu;

import akka.actor.ActorRef;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.Query;
import akka.japi.Pair;
import akka.pattern.Patterns;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Source;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

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

    private static CompeletionStage <Object> asy (Pair<String, Integer> pair) {
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

    private static Sink<String, Integer>, CompetionStage<Long>> testSink() {
        return Flow
                .<Pair<String, Integer>>create()
                .mapConcat(request -> Collections.nCopies(request.second(), request.first()))
    }
}
