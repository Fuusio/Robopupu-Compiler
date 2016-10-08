package com.robopupu.samples;

import com.robopupu.api.plugin.PlugInterface;

/**
 * {@link ModelObserver} ...
 */
@PlugInterface
public interface ModelObserver<T_Model> {

    void onModelChanged(T_Model model);
}
