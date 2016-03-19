package com.robopupu.samples;

/**
 * {@link PowerOffState} ...
 */
public class PowerOffState extends State {

    public PowerOffState() {
        super(State.class, null);
    }

    @Override
    public void switchOn() {
        getCoffeeMachine().setPowerOn();
    }

    @Override
    protected void onExit() {
        super.onExit();
    }

    @Override
    protected void onEnter() {

    }
}
