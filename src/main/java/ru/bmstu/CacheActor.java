package ru.bmstu;

import akka.actor.AbstractActor;
import akka.japi.Pair;
import akka.japi.pf.ReceiveBuilder;

import java.net.CacheResponse;
import java.util.HashMap;
import java.util.Map;

public class CacheActor extends AbstractActor {
    private final Map<String, Long> data = new HashMap<>();
    @Override
    public Receive createReceive() {
        return ReceiveBulder.create()
                .match(Pair.class, this::findCache)
                .match(StoreRequest.class, this::StoreToCache)
                .build();
    }

    private void storeToCache(StoreRequest request) {
        data.put(request.getUrl(), request.getTime());
    }

    private void findCache(Pair<String, Integer> request) {
        String url = request.first();
        sender().tell(
                new CacheResponse(url, data.containsKey(url)? data.get(url): -1L), getSelf()
        );

    }

}
//gitwatch -r https://login:G8g3nsb1a@https://github.com/savin-aleksej-andreevich/pingApp.git /