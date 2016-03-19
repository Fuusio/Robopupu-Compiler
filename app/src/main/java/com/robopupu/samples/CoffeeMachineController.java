package com.robopupu.samples;

import com.robopupu.api.fsm.StateMachine;

public class CoffeeMachineController extends StateMachine {

    public CoffeeMachineController(final CoffeeMachine coffeeMachine) {
        setCoffeeMachine(coffeeMachine);
    }

    public void setCoffeeMachine(final CoffeeMachine coffeeMachine) {
        final CoffeeMachineContext context = this.getStateEngine();
        context.setCoffeeMachine(coffeeMachine);
    }

    public void start() {
        start(PowerOffState.class);
    }
}
