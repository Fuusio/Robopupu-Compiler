package com.robopupu.compiler.fsm;

import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

/**
 * {@link SetterMethod} is a model class used for storing information about
 * a method defined in an interface that is annotated with {@link com.robopupu.api.fsm.StateMachineEvents}.
 */
public class SetterMethod {

    protected final ExecutableElement executableElement;
    protected final String signature;

    public SetterMethod(final ExecutableElement executableElement) {
        this.executableElement = executableElement;
        signature = createSignature();
    }

    private String createSignature() {
        final StringBuilder builder = new StringBuilder(getMethodName());
        builder.append("(");

        final List<? extends VariableElement> parameters = executableElement.getParameters();

        if (!parameters.isEmpty()) {
            boolean firstParameter = true;

            for (final VariableElement parameter : parameters) {
                if (firstParameter) {
                    firstParameter = false;
                } else {
                    builder.append(',');
                }
                builder.append(getParameterType(parameter));
            }
        }

        builder.append(")");
        return builder.toString();
    }

    public ExecutableElement getExecutableElement() {
        return executableElement;
    }

    public String getMethodName() {
        return executableElement.getSimpleName().toString();
    }

    public List<? extends VariableElement> getParameters() {
        return executableElement.getParameters();
    }

    public String getSignature() {
        return signature;
    }

    private String getParameterType(final VariableElement variable) {
        final String type = variable.asType().toString();
        final int index = type.lastIndexOf('.');

        if (index > 0) {
            return type.substring(index + 1);
        } else {
            return type;
        }
    }
}
