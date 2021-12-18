package ru.bmstu;

import akka.actor.AbstractActor;
import akka.japi.Pair;
import akka.japi.pf.ReceiveBuilder;
import java.util.HashMap;
import java.util.Map;

public class CacheActor extends AbstractActor {
    private final Map<String, Long> data = new HashMap<>();
    @Override
    

}
