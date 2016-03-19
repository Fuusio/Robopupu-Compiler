package com.robopupu.compiler.fsm;

import com.google.auto.service.AutoService;

import com.robopupu.api.fsm.StateMachineContext;
import com.robopupu.api.fsm.StateMachineEvents;
import com.robopupu.compiler.util.ProcessorException;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

/**
 * {@link FsmAnnotationProcessor} implements {@link AbstractProcessor}
 * to generate runtime objects from classes annotated using (@link StateMachineEvents} and
 * (@link StateMachineContext} annotations.
 */
@AutoService(Processor.class)
public class FsmAnnotationProcessor extends AbstractProcessor {

    private Elements mElementUtils;
    private Filer mFiler;
    private Messager mMessager;
    private HashMap<String, StateEngineClass> mStateEngineClasses;

    @Override
    public synchronized void init(final ProcessingEnvironment environment) {
        super.init(environment);

        mFiler = environment.getFiler();
        mElementUtils = environment.getElementUtils();
        mMessager = environment.getMessager();
        mStateEngineClasses = new HashMap<>();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        final Set<String> annotations = new LinkedHashSet<>();
        annotations.add(StateMachineEvents.class.getCanonicalName());
        annotations.add(StateMachineContext.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {

        // Process all classes, methods and constructors annotated with StateMachineEvents
        try {

            for (final Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(StateMachineEvents.class)) {

                final ElementKind elementKind = annotatedElement.getKind();
                final boolean isInterfaceElement = elementKind == ElementKind.INTERFACE;

                if (!isInterfaceElement) {
                    throw new ProcessorException(annotatedElement, "Only interfaces can be annotated with @%s",
                            StateMachineEvents.class.getSimpleName());
                }

                final TypeElement classElement = (TypeElement) annotatedElement;
                final List<? extends AnnotationMirror> annotationMirrors = classElement.getAnnotationMirrors();

                for (final AnnotationMirror annotationMirror : annotationMirrors) {
                    if (annotationMirror.getAnnotationType().toString().contentEquals(StateMachineEvents.class.getName())) {
                        final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = annotationMirror.getElementValues();
                        Collection<? extends AnnotationValue> values;
                        boolean error = true;

                        if (elementValues != null) {
                            values = elementValues.values();

                            if (values.size() == 1) {
                                error = false;

                                final AnnotationValue value = values.iterator().next();
                                final String stateMachineClassName = value.toString().replace(".class", "");

                                StateEngineClass stateEngineClass = mStateEngineClasses.get(stateMachineClassName);

                                if (stateEngineClass == null) {
                                    stateEngineClass = new StateEngineClass(stateMachineClassName);
                                    mStateEngineClasses.put(stateMachineClassName, stateEngineClass);
                                }

                                stateEngineClass.addEventInterface(classElement);

                                final List<? extends Element> elements = classElement.getEnclosedElements();

                                for (final Element element : elements) {
                                    if (element.getKind() == ElementKind.METHOD) {
                                        final ExecutableElement method = (ExecutableElement)element;
                                        final EventMethod eventMethod = new EventMethod(method);
                                        validateEventMethod(eventMethod);
                                        stateEngineClass.addEventMethod(eventMethod);
                                    }
                                }
                            }
                        }

                        if (error) {
                            throw new ProcessorException(annotatedElement, StateMachineEvents.class.getSimpleName() + " has to specify StateMachine implementation");
                        }
                        break;
                    }
                }
            }
        } catch (ProcessorException e) {
            handleError(e.getElement(), e.getMessage());
        }

        // Process all classes, methods and constructors annotated with StateMachineEvents
        try {

            for (final Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(StateMachineContext.class)) {

                final ElementKind elementKind = annotatedElement.getKind();
                final boolean isInterfaceElement = elementKind == ElementKind.INTERFACE;

                if (!isInterfaceElement) {
                    throw new ProcessorException(annotatedElement, "Only interfaces can be annotated with @%s",
                            StateMachineContext.class.getSimpleName());
                }

                final TypeElement classElement = (TypeElement) annotatedElement;
                final List<? extends AnnotationMirror> annotationMirrors = classElement.getAnnotationMirrors();

                for (final AnnotationMirror annotationMirror : annotationMirrors) {
                    if (annotationMirror.getAnnotationType().toString().contentEquals(StateMachineContext.class.getName())) {
                        final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = annotationMirror.getElementValues();
                        Collection<? extends AnnotationValue> values;
                        boolean error = true;

                        if (elementValues != null) {
                            values = elementValues.values();

                            if (values.size() == 1) {
                                error = false;

                                final AnnotationValue value = values.iterator().next();
                                final String stateMachineClassName = value.toString().replace(".class", "");

                                StateEngineClass stateEngineClass = mStateEngineClasses.get(stateMachineClassName);

                                if (stateEngineClass == null) {
                                    stateEngineClass = new StateEngineClass(stateMachineClassName);
                                    mStateEngineClasses.put(stateMachineClassName, stateEngineClass);
                                }

                                stateEngineClass.addContextInterface(classElement);

                                final List<? extends Element> elements = classElement.getEnclosedElements();

                                for (final Element element : elements) {
                                    if (element.getKind() == ElementKind.METHOD) {
                                        final ExecutableElement method = (ExecutableElement)element;
                                        final SetterMethod setterMethod = new SetterMethod(method);
                                        validateSetterMethod(setterMethod);
                                        stateEngineClass.addSetterMethod(setterMethod);
                                    }
                                }
                            }
                        }

                        if (error) {
                            throw new ProcessorException(annotatedElement, StateMachineEvents.class.getSimpleName() + " has to specify StateMachine implementation");
                        }
                        break;
                    }
                }
            }
        } catch (ProcessorException e) {
            handleError(e.getElement(), e.getMessage());
        }

        // Generate StateEngine implementations for all classes specified in StateMachineEvents
        // annotations
        try {
            for (final StateEngineClass stateEngineClass : mStateEngineClasses.values()) {
                stateEngineClass.generateCode(mElementUtils, mFiler);
            }
            mStateEngineClasses.clear();
        } catch (IOException e) {
            handleError(null, e.getMessage());
        }

        return true;
    }

    private void validateEventMethod(final EventMethod method) throws ProcessorException {
        final ExecutableElement element = method.getExecutableElement();
        final TypeMirror returnType = element.getReturnType();
        final TypeKind typeKind = returnType.getKind();

        if (typeKind != TypeKind.VOID) {
            throw new ProcessorException(element, "Trigger event method must have void return type.");
        }
    }

    private void validateSetterMethod(final SetterMethod method) throws ProcessorException {
        final ExecutableElement element = method.getExecutableElement();
        final TypeMirror returnType = element.getReturnType();
        final TypeKind typeKind = returnType.getKind();

        if (typeKind != TypeKind.VOID) {
            throw new ProcessorException(element, "Reference setter method must have void return type.");
        }

        final List<? extends VariableElement> parameters = method.getParameters();

        if (parameters.size() != 1) {
            throw new ProcessorException(element, "Reference setter method must have one parameter.");
        }
    }

    /**
     * Print the specified error message
     *
     * @param element The {@link Element} for which the error was detected. May be {@code null).
     * @param message A {@link String} containing the error message.
     */
    public void handleError(final Element element, final String errorMessage) {
        mMessager.printMessage(Diagnostic.Kind.ERROR, errorMessage, element);
    }
}