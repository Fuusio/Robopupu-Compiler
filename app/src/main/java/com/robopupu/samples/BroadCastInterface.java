package com.robopupu.samples;

import com.robopupu.api.plugin.PlugInterface;
import com.robopupu.api.plugin.PlugMode;

/**
 * {@link BroadCastInterface} ...
 */
@PlugInterface(PlugMode.BROADCAST)
public interface BroadCastInterface {

    void onIntChange(int oldValue, int newValue);
}
