package com.robopupu.samples;

/**
 * {@link PowerOnState} ...
 */
public class PowerOnState extends State {

    public PowerOnState() {
        super(State.class, null);
    }

    @Override
    public void switchOff() {
        getCoffeeMachine().setPowerOff();
    }

    @Override
    protected void onExit() {
    }

    @Override
    protected void onEnter() {
    }
}
