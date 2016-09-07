package com.robopupu.samples;

import com.robopupu.api.plugin.Plug;
import com.robopupu.api.plugin.Plugin;

import java.util.List;

/**
 * {@link SampleComponent} ...
 */
@Plugin
public class SampleComponent implements SampleInterface {


    @Plug SampleInterface mSample;

    @Plug(DummyDependencyScope.class) SampleInterface mSample2;

    @Override
    public void method1() {

    }

    @Override
    public void method2(String p1, boolean p2, List p3) {

    }

    @Override
    public char method3(String p1, boolean p2, List p3) {
        return 0;
    }

    @Override
    public List method4(String p1, boolean p2, List p3) {
        return null;
    }

    @Override
    public void foo1() {

    }

    @Override
    public boolean foo2(int a) {
        return false;
    }

    @Override
    public <S extends String, T extends List<?>> void foo(S string, T list) {

    }
}
