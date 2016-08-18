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

    protected final EventType eventType;
    protected final ExecutableElement methodElement;
    protected final String tag;

    public EventHandlerMethod(final ExecutableElement executableElement, final EventType eventType, final String tag) {
        this.eventType = eventType;
        methodElement = executableElement;
        this.tag = tag;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getTag() {
        return tag;
    }

    public ExecutableElement getMethodElement() {
        return methodElement;
    }

    public String getMethodName() {
        return methodElement.getSimpleName().toString();
    }

    public List<? extends VariableElement> getParameters() {
        return methodElement.getParameters();
    }
}
