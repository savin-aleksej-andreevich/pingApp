package ru.bmstu;

import akka.actor.ActorRef;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.Query;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;

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
}
