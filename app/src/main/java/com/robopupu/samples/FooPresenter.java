package com.robopupu.samples;


import com.robopupu.api.feature.FeaturePresenter;
import com.robopupu.api.mvp.OnChecked;
import com.robopupu.api.mvp.OnClick;
import com.robopupu.api.mvp.OnTextChanged;

public interface FooPresenter extends FeaturePresenter {

    @OnClick
    void onButton1Click();

    @OnClick
    void onButton2Click();

    @OnTextChanged
    void onUserNameTextChanged(String text);

    @OnChecked
    void onRememberChecked(boolean checked);
}
