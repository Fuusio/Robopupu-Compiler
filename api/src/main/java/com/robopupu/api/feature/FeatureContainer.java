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

import java.util.HashMap;


public interface FeatureContainer {


    boolean canCommitFragment();

    /**
     * Clears {@link FeatureView}s of contained {@link Feature} from the back stack.
     *
     * @param backStackViews A {@link HashMap} containing the back
     */
    void clearBackStack(HashMap<String, FeatureView> backStackViews);

    /**
     * Tests if the previous {@link FeatureView} can be popped from the back stack.
     *
     * @return A {@code boolean} value.
     */
    boolean canGoBack();

    /**
     * Pops the previous {@link FeatureView} from the back stack.
     *
     * @return The tag of the removed {@link FeatureView}. May return {@code null}.
     */
    String goBack();
}
