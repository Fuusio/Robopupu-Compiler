/*
 * Copyright (C) 2014 - 2015 Marko Salmela, http://robopupu.com
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
package com.robopupu.api.feature;

import com.robopupu.api.dependency.DependencyScope;
import com.robopupu.api.dependency.DependencyScopeOwner;
import com.robopupu.api.mvp.PresenterListener;
import com.robopupu.api.mvp.View;
import com.robopupu.api.plugin.PluginBus;
import com.robopupu.api.plugin.PluginComponent;
import com.robopupu.api.plugin.PluginStateComponent;

import java.util.List;

/**
 * {@link Feature} defines an interface for {@link PluginStateComponent}s that implement of
 * application feature as a component. A concrete {@link Feature} implementation may implement logic
 * for UI navigation and UI flow logic.<b></b> A {@link Feature}  is also
 * a {@link DependencyScopeOwner} that provides a {@link DependencyScope} for providing dependencies.
 */
public interface Feature extends PresenterListener, PluginStateComponent {

    /**
     * Gets the currently active {@link View}s.
     *
     * @return A {@link List} containing the currently active views as {@link View}s.
     */
    List<View> getActiveViews();

    /**
     * Tests if the given {@link View} is currently active one.
     *
     * @param view A {@link View}.
     * @return A {@code boolean} value.
     */
    boolean isActiveView(View view);

    /**
     * Sets this {@link Feature} to be an Activity Feature that is owned and controlled by
     * an {@linx Activity}.
     * @param isActivityFeature A {@code boolean} value.
     */
    void setActivityFeature(boolean isActivityFeature);

    /**
     * Tests if this {@link Feature} is set to be an Activity Feature that is owned and controlled
     * by an {@linx Activity}.
     * @return  A {@code boolean} value.
     */
    boolean isActivityFeature();

    /**
     * Tests if any of the {@link View}s of this {@link Feature} is in foreground and has a focus.
     * @return A {@code boolean}.
     */
    boolean hasFocusedView();

    /**
     * Tests if this {@link Feature} is in foreground i.e. it has at least one visible
     * {@link View}.
     *
     * @return A {@code boolean} value.
     */
    boolean hasForegroundView();

    /**
     * Tests if this {@link Feature} handles the back pressed event.
     *
     * @return A {@code boolean} value.
     */
    boolean isBackPressedEventHandler();

    /**
     * Tests if this {@link Feature} has any {@link View}s in back stack.
     * @return A {@code boolean} value.
     */
    boolean hasViewsInBackStack();

    /**
     * Clears the back stack managed by {@linx FragmentManager}.
     */
    void clearBackStack();

    /**
     * Tests if the previous {@link View} can be navigated back to.
     *
     * @return A {@code boolean} value.
     */
    boolean canGoBack();

    /**
     * Goes back to previous {@link View}.
     */
    void goBack();

    /**
     * Invoked to pause this {@link Feature}.
     *
     * @param finishing A {@code boolean} value indicating if the {@link Feature} is
     *                  going to be finished i.e. it is not resumed nor restarted anymore.
     */
    void pause(boolean finishing);

    /**
     * Finishes this {@link Feature}. A finished {@link Feature} is stopped and destroyed.
     * If a {@link Feature} is implements a {@link PluginComponent} it is
     * unplugged from a {@link PluginBus}.
     */
    void finish();
}
