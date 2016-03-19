package com.robopupu.samples;

/**
 * {@link CoffeeMachine} ...
 */
public class CoffeeMachine {

    private boolean mPowerOn;

    public CoffeeMachine() {
        mPowerOn = false;
    }

    public void setPowerOn() {
        mPowerOn = true;
    }

    public void setPowerOff() {
        mPowerOn = false;
    }
}
