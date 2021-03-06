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
     * Gets the currently active {@link FeatureView}s.
     *
     * @return A {@link List} containing the currently active views as {@link FeatureView}s.
     */
    List<FeatureView> getActiveViews();

    /**
     * Tests if the given {@link FeatureView} is currently active one.
     *
     * @param view A {@link FeatureView}.
     * @return A {@code boolean} value.
     */
    boolean isActiveView(FeatureView view);

    /**
     * Adds the given {@link FeatureView} to the set of currently active {@link FeatureView}s of
     * this {@link Feature}.
     * @param view A {@link FeatureView} to be added.
     * @return The {@link FeatureView} if it was added as a new active {@link FeatureView}.
     */
    FeatureView addActiveView(FeatureView view);

    /**
     * Removes the given {@link FeatureView} from the set of currently active {@link FeatureView}s
     * of this {@link Feature}.
     * @param view A {@link FeatureView} to be removed.
     * @return The {@link FeatureView} if it was removed from being an active {@link FeatureView}.
     */
    FeatureView removeActiveView(FeatureView view);

    /**
     * Sets the {@link FeatureManager} that started this {@link Feature}.
     *
     * @param manager A {@link FeatureManager}.
     */
    void setFeatureManager(FeatureManager manager);

    /**
     * Gets the {@link FeatureContainer} that hosts the {@code FeatureCompatFragment}s of this
     * {@link Feature}.
     *
     * @return A {@link FeatureContainer}.
     */
    FeatureContainer getFeatureContainer();

    /**
     * Sets the the {@link FeatureContainer} that hosts the {@code FeatureCompatFragment}s of this
     * {@link Feature}.
     *
     * @param container A {@link FeatureContainer}.
     */
    void setFeatureContainer(FeatureContainer container);

    /**
     * Sets this {@link Feature} to be an Activity Feature that is owned and controlled by
     * an {@code Activity}.
     * @param isActivityFeature A {@code boolean} value.
     */
    void setActivityFeature(boolean isActivityFeature);

    /**
     * Tests if this {@link Feature} is set to be an Activity Feature that is owned and controlled
     * by an {@code Activity}.
     * @return  A {@code boolean} value.
     */
    boolean isActivityFeature();

    /**
     * Tests if this {@link Feature} is in foreground i.e. it has at least one visible
     * {@link FeatureView}.
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
     * Tests if this {@link Feature} has any {@link FeatureView}s in back stack.
     * @return A {@code boolean} value.
     */
    boolean hasBackStackViews();

    /**
     * Clears the back stack managed by {@code FragmentManager}.
     */
    void clearBackStack();

    /**
     * Tests if the previous {@link FeatureView} can be navigated back to.
     *
     * @return A {@code boolean} value.
     */
    boolean canGoBack();

    /**
     * Goes back to previous {@link FeatureView}.
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

    /**
     * Invoked when a {@link FeatureContainer} has been started.
     *
     * @param container A {@link FeatureContainer}.
     */
    void onFeatureContainerStarted(FeatureContainer container);

    /**
     * Invoked when a {@link FeatureContainer} has been paused.
     *
     * @param container A {@link FeatureContainer}.
     * @param finishing A {@code boolean} flag indicating if the {@link FeatureContainer} is finishing.
     */
    void onFeatureContainerPaused(FeatureContainer container, boolean finishing);

    /**
     * Invoked when a {@link FeatureContainer} has been resumed.
     *
     * @param container A {@link FeatureContainer}.
     */
    void onFeatureContainerResumed(FeatureContainer container);

    /**
     * Invoked when a {@link FeatureContainer} has been stopped.
     *
     * @param container A {@link FeatureContainer}.
     * @param finishing A {@code boolean} flag indicating if the {@link FeatureContainer} is finishing.
     */
    void onFeatureContainerStopped(FeatureContainer container, boolean finishing);
}
