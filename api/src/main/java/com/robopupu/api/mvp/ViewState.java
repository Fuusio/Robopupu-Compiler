/*
 * Copyright (C) 2015 Marko Salmela, http://robopupu.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.robopupu.api.mvp;

import com.robopupu.api.util.LifecycleState;

/*
 * {@link ViewState} is an object that is used to represent the current lifecycle state of
 * a {@link View}.
 */
public class ViewState {

    private final View view;

    private boolean instanceStateSaved;
    private LifecycleState lifecycleState;
    private boolean isMovedToBackground;
    private boolean isRestarted;

    public ViewState() {
        this(null);
    }

    public ViewState(final View view) {
        this.view = view;
        instanceStateSaved = false;
        lifecycleState = LifecycleState.DORMANT;
        isMovedToBackground = false;
        isRestarted = false;
    }

    public void setInstanceStateSaved(final boolean saved) {
        instanceStateSaved = saved;
    }

    /*
     * Gets the current {@link LifecycleState} of this {@link ViewState}.
     * @return A {@link LifecycleState}.
     */
    public LifecycleState getLifecycleState() {
        return lifecycleState;
    }

    /*
     * Tests if the {@link LifecycleState} is dormant. In dormant state the {@link View}
     * is instantiated, but not created i.e. method {@linx Fragment#onCreate(Bundle)}/{@linx Activity#onCreate(Bundle)}
     * has not been invoked yet.
     * @return A {@code boolean} value.
     */
    public boolean isDormant() {
        return lifecycleState.isDormant();
    }

    /*
     * Tests if the {@link LifecycleState} is created. In created state the {@link View}
     * method {@linx Fragment#onCreate(Bundle)}/{@linx Activity#onCreate(Bundle)} has been invoked.
     * @return A {@code boolean} value.
     */
    public boolean isCreated() {
        return lifecycleState.isCreated();
    }

    /*
     * Tests if the {@link LifecycleState} is started or resumed. In started state the method
     * {@linx Fragment#onStart()}/{@linx Activity#onStart()} and possibly {@linx Fragment#onResume()}/{@linx Activity#onResume()}
     * have been invoked. Method {@linx ViewState#getLifecycleState()} can be used to determinate
     * the exact lifecycle state.
     * @return A {@code boolean} value.
     */
    public boolean isStarted() {
        return lifecycleState.isStarted() || lifecycleState.isResumed();
    }

    /*
     * Tests if the {@link LifecycleState} is resumed. In resumed state both the methods
     * {@linx Fragment#onStart()}/{@linx Activity#onStart()} and {@linx Fragment#onResume()}/{@linx Activity#onResume()}
     * have been invoked.
     * @return A {@code boolean} value.
     */
    public boolean isResumed() {
        return lifecycleState.isResumed();
    }

    /*
     * Tests if the {@link LifecycleState} is paused. In paused state the method
     * {@linx Fragment#onPause()}/{@linx Activity#onPause()} has been invoked.
     * @return A {@code boolean} value.
     */
    public boolean isPaused() {
        return lifecycleState.isPaused();
    }

    /*
     * Tests if the {@link LifecycleState} is restarted. In restarted state the method
     * {@linx Activity#onRestart()} has been invoked.
     * @return A {@code boolean} value.
     */
    public boolean isRestarted() {
        return isStarted() && isRestarted;
    }

    /*
     * Tests if the {@link LifecycleState} is stopped. In stopped state the method
     * {@linx Fragment#onStop()}/{@linx Activity#onStop()} has been invoked.
     * @return A {@code boolean} value.
     */
    public boolean isStopped() {
        return lifecycleState.isStopped() || lifecycleState.isDestroyed();
    }

    /*
     * Tests if the {@link LifecycleState} is destroyed. In destroyed state the method
     * {@linx Fragment#onDestroy()}/{@linx Activity#onDestroy()} has been invoked.
     * @return A {@code boolean} value.
     */
    public boolean isDestroyed() {
        return lifecycleState.isDestroyed();
    }

    /*
     * Invoked when the {@link View} is created.
     */
    public void onCreate() {
        lifecycleState = LifecycleState.CREATED;
    }

    /*
     * Invoked when the {@link View} is started.
     */
    public void onStart() {
        lifecycleState = LifecycleState.STARTED;
    }

    /*
     * Invoked when the {@link View} is restarted.
     */
    public void onRestart() {
        isRestarted = true;
    }

    /*
     * Invoked when the {@link View} is paused.
     */
    public void onPause() {
        lifecycleState = LifecycleState.PAUSED;
        isMovedToBackground = true;
    }

    /*
     * Invoked when the {@link View} is resumed.
     */
    public void onResume() {
        lifecycleState = LifecycleState.RESUMED;
        instanceStateSaved = false;
        isMovedToBackground = false;
    }

    /*
     * Invoked when the {@link View} is stopped.
     */
    public void onStop() {
        lifecycleState = LifecycleState.STOPPED;
    }

    /*
     * Invoked when the {@link View} is destroyed.
     */
    public void onDestroy() {
        lifecycleState = LifecycleState.DESTROYED;
    }

    public boolean canCommitFragment() {
        return !instanceStateSaved;
    }

    public boolean isMovedToBackground() {
        return isMovedToBackground;
    }
}
