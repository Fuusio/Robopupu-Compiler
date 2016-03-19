package com.robopupu.samples;

import com.robopupu.api.fsm.StateMachineContext;

@StateMachineContext(CoffeeMachineController.class)
public interface CoffeeMachineContext {
    void setCoffeeMachine(CoffeeMachine coffeeMachine);
}
