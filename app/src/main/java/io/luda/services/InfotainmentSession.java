package io.luda.services;

import android.util.Log;

import org.jeasy.states.api.Event;
import org.jeasy.states.api.EventHandler;
import org.jeasy.states.api.FiniteStateMachine;
import org.jeasy.states.api.FiniteStateMachineException;
import org.jeasy.states.api.State;
import org.jeasy.states.api.Transition;
import org.jeasy.states.core.FiniteStateMachineBuilder;
import org.jeasy.states.core.TransitionBuilder;

import java.util.Arrays;
import java.util.HashSet;

public class InfotainmentSession {

    private static final String TAG = InfotainmentSession.class.getName();
    public FiniteStateMachine machine;
    private InfotainmentStatesListener listener;

    public void sendEvent(Event event) {

        try {
            State state = machine.fire(event);
            Log.i(TAG, String.format("sendEvent %s: State atual: %s", event.toString(),state.getName()));
        } catch (FiniteStateMachineException e) {
            e.printStackTrace();
        }

    }



    public static State STANDBY = new State("STANDBY");
    public static State INITIALIZE = new State("INITIALIZE");
    public static State CONSUME = new State("CONSUME");
    public static State ADVERTISEMENT = new State("ADVERTISEMENT");

    public static class UserDetectedEvent extends Event { }
    public static class ContentLoadedEvent extends Event { }
    public static class AdsLoadedEvent extends Event { }
    public static class AdsClosedEvent extends Event { }
    public static class UserNotDetectedEvent extends Event { }



    public InfotainmentSession (InfotainmentStatesListener listener) {
        this.listener = listener;

        try {


            Transition standByToInitialize = new TransitionBuilder()
                    .name("standByToInitialize")
                    .sourceState(STANDBY)
                    .eventType(UserDetectedEvent.class)
                    .eventHandler(new EventHandler() {
                        @Override
                        public void handleEvent(Event event) throws Exception {
                            listener.onInitialize(event);
                        }
                    })
                    .targetState(INITIALIZE)
                    .build();

            Transition initializeToConsume = new TransitionBuilder()
                    .name("initializeToConsume")
                    .sourceState(INITIALIZE)
                    .eventType(ContentLoadedEvent.class)
                    .eventHandler(new EventHandler() {
                        @Override
                        public void handleEvent(Event event) throws Exception {
                            listener.onConsume(event);
                        }
                    })
                    .targetState(CONSUME)
                    .build();

            Transition consumeToAd = new TransitionBuilder()
                    .name("consumeToAd")
                    .sourceState(CONSUME)
                    .eventType(AdsLoadedEvent.class)
                    .eventHandler(new EventHandler() {
                        @Override
                        public void handleEvent(Event event) throws Exception {
                            listener.onAdvertisement(event);
                        }
                    })
                    .targetState(ADVERTISEMENT)
                    .build();

            Transition AdToConsume = new TransitionBuilder()
                    .name("AdToConsume")
                    .sourceState(ADVERTISEMENT)
                    .eventType(AdsClosedEvent.class)
                    .eventHandler(new EventHandler() {
                        @Override
                        public void handleEvent(Event event) throws Exception {
                            listener.onConsume(event);
                        }
                    })
                    .targetState(CONSUME)
                    .build();

            Transition consumeToStandBy = new TransitionBuilder()
                    .name("consumeToStandBy")
                    .sourceState(CONSUME)
                    .eventType(UserNotDetectedEvent.class)
                    .eventHandler(new EventHandler() {
                        @Override
                        public void handleEvent(Event event) throws Exception {
                            listener.onStandBy(event);
                        }
                    })
                    .targetState(STANDBY)
                    .build();



            machine = new FiniteStateMachineBuilder(new HashSet<>(Arrays.asList(STANDBY, INITIALIZE, CONSUME, ADVERTISEMENT)), STANDBY)
                    .registerTransition(standByToInitialize)
                    .registerTransition(initializeToConsume)
                    .registerTransition(consumeToAd)
                    .registerTransition(AdToConsume)
                    .registerTransition(consumeToStandBy)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
