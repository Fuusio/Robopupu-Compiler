package com.robopupu.compiler.mvp;

import com.robopupu.api.mvp.OnClick;
import com.robopupu.api.mvp.OnTextChanged;
import com.robopupu.api.mvp.Presenter;

import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

/**
 * {@link EventHandlerMethod} is a model class used for storing information about
 * methods defined in an {@link Presenter} interface that is annotated with {@link OnClick} or
 * {@link OnTextChanged}.
 */
public class EventHandlerMethod {

    public enum EventType {
        ON_CHECKED,
        ON_CLICK,
        ON_TEXT_CHANGED;
    }

    protected final EventType mEventType;
    protected final ExecutableElement mMethodElement;
    protected final String mTag;

    public EventHandlerMethod(final ExecutableElement executableElement, final EventType eventType, final String tag) {
        mEventType = eventType;
        mMethodElement = executableElement;
        mTag = tag;
    }

    public EventType getEventType() {
        return mEventType;
    }

    public String getTag() {
        return mTag;
    }

    public ExecutableElement getMethodElement() {
        return mMethodElement;
    }

    public String getMethodName() {
        return mMethodElement.getSimpleName().toString();
    }

    public List<? extends VariableElement> getParameters() {
        return mMethodElement.getParameters();
    }
}
