package com.robopupu.samples;

import com.robopupu.api.plugin.PlugInterface;

import java.util.List;

/**
 * {@link SampleInterface} ...
 */
@PlugInterface
public interface SampleInterface extends FooInterface {

    void method1();

    void method2(String p1, boolean p2, List p3);

    char method3(String p1, boolean p2, List p3);

    List method4(String p1, boolean p2, List p3);

    <S extends String, T extends List<?>> void foo(S string, T list);
}
