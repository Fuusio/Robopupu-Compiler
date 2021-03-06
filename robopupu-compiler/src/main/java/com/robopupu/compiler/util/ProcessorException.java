package com.robopupu.compiler.util;

import javax.lang.model.element.Element;

/**
 * {@link ProcessorException} implements {@link Exception} that contain information about
 * an exception detected in Annotation Processor implementations.
 */
public class ProcessorException extends Exception {

    private final Element element;

    public ProcessorException(final Element element, final String message, final Object... args) {
        super(String.format(message, args));
        this.element = element;
    }

    public Element getElement() {
        return element;
    }
}
