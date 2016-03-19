package com.robopupu.samples;

import com.robopupu.api.fsm.StateMachineEvents;

@StateMachineEvents(CoffeeMachineController.class)
public interface PowerSwitchEvents {
    void switchOn();
    void switchOff();
}
