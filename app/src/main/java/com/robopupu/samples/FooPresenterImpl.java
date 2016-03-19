package com.robopupu.samples;

import com.robopupu.api.dependency.DependencyScope;
import com.robopupu.api.feature.Feature;
import com.robopupu.api.mvp.View;
import com.robopupu.api.util.Params;

/**
 * {@link FooPresenterImpl} ...
 */
public class FooPresenterImpl implements FooPresenter {


    @Override
    public void onButton1Click() {
    }

    @Override
    public void onButton2Click() {
    }

    @Override
    public void onRememberChecked(final  boolean checked) {
    }

    @Override
    public void onUserNameTextChanged(final String text) {
    }

    @Override
    public Feature getFeature() {
        return null;
    }

    @Override
    public void setFeature(Feature feature) {
    }

    @Override
    public DependencyScope getScope() {
        return null;
    }

    @Override
    public void setScope(DependencyScope scope) {

    }

    @Override
    public View getView() {
        return null;
    }

    @Override
    public void setParams(Params params) {

    }

    @Override
    public void finish() {

    }

    @Override
    public void onViewCreated(View view, Params inState) {

    }

    @Override
    public void onViewResume(View view) {

    }

    @Override
    public void onViewPause(View view) {

    }

    @Override
    public void onViewStart(View view) {

    }

    @Override
    public void onViewStop(View view) {

    }

    @Override
    public void onViewDestroy(View view) {

    }
}
