package io.luda.services;

import org.jeasy.states.api.Event;

public interface InfotainmentStatesListener {

    void onStandBy(Event event) ;

    void onInitialize(Event event);

    void onConsume(Event event) ;

    void onAdvertisement(Event event) ;
}
