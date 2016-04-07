package com.robopupu.compiler.mvp;

import com.google.auto.service.AutoService;
import com.robopupu.api.mvp.OnChecked;
import com.robopupu.api.mvp.OnClick;
import com.robopupu.api.mvp.OnTextChanged;
import com.robopupu.api.mvp.Presenter;
import com.robopupu.compiler.util.ProcessorException;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

/**
 * {@link MvpAnnotationProcessor} implements {@link AbstractProcessor}
 * to generate runtime objects from classes annotated using (@link OnClick} and
 * {@link OnTextChanged} annotations.
 */
@AutoService(Processor.class)
public class MvpAnnotationProcessor extends AbstractProcessor {

    private HashMap<String, EventsDelegateClass> mEventsDelegateClasses;

    private Filer mFiler;
    private Elements mElementUtils;
    private Messager mMessager;

    @Override
    public synchronized void init(final ProcessingEnvironment environment) {
        super.init(environment);

        mFiler = environment.getFiler();
        mElementUtils = environment.getElementUtils();
        mMessager = environment.getMessager();
        mEventsDelegateClasses = new HashMap<>();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        final Set<String> annotations = new LinkedHashSet<>();
        annotations.add(OnChecked.class.getCanonicalName());
        annotations.add(OnClick.class.getCanonicalName());
        annotations.add(OnTextChanged.class.getCanonicalName());
        return annotations;
    }

    @Override public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {

        // Process Presenter interfaces annotated with @OnChecked, @OnClick, or @OnTextChanged
        try {
            for (final Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(OnChecked.class)) {
                if (annotatedElement.getKind() == ElementKind.METHOD) {
                    handleOnCheckedAnnotation(annotatedElement, (TypeElement) annotatedElement.getEnclosingElement());
                } else {
                    throw new ProcessorException(annotatedElement, "Only methods can be annotated with @%s", OnChecked.class.getSimpleName());
                }
            }

            for (final Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(OnClick.class)) {
                if (annotatedElement.getKind() == ElementKind.METHOD) {
                    handleOnClickAnnotation(annotatedElement, (TypeElement) annotatedElement.getEnclosingElement());
                } else {
                    throw new ProcessorException(annotatedElement, "Only methods can be annotated with @%s", OnClick.class.getSimpleName());
                }
            }

            for (final Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(OnTextChanged.class)) {
                if (annotatedElement.getKind() == ElementKind.METHOD) {
                    handleOnTextChangedAnnotation(annotatedElement, (TypeElement) annotatedElement.getEnclosingElement());
                } else {
                    throw new ProcessorException(annotatedElement, "Only methods can be annotated with @%s", OnTextChanged.class.getSimpleName());
                }
            }

            // Generate code

            for (final EventsDelegateClass eventsDelegateClass : mEventsDelegateClasses.values()) {
                eventsDelegateClass.generateCode(mElementUtils, mFiler);
            }

            // We need to clear cache of EventDelegateClasses after code generation
            mEventsDelegateClasses.clear();
        } catch (ProcessorException e) {
            handleError(e.getElement(), e.getMessage());
        } catch (IOException e) {
            handleError(null, e.getMessage());
        }
        return true;
    }

    private void handleOnCheckedAnnotation(final Element annotatedElement, final TypeElement enclosingTypeElement) {
        final ExecutableElement methodElement = (ExecutableElement)annotatedElement;
        try {
            if (enclosingTypeElement.getKind() != ElementKind.INTERFACE || !isPresenterInterface(enclosingTypeElement)) {
                throw new ProcessorException(enclosingTypeElement, "Only Presenter interface methods can be annotated with @%s",
                        OnChecked.class.getSimpleName());
            }

            final String interfaceClassName = enclosingTypeElement.getQualifiedName().toString();
            EventsDelegateClass eventsDelegateClass = mEventsDelegateClasses.get(interfaceClassName);

            if (eventsDelegateClass == null) {
                eventsDelegateClass = new EventsDelegateClass(enclosingTypeElement);
                mEventsDelegateClasses.put(interfaceClassName, eventsDelegateClass);
            }

            final String methodName = methodElement.getSimpleName().toString();

            if (methodElement.getParameters().size() != 1) {
                throw new ProcessorException(enclosingTypeElement, "Method @%s annotated with OnChecked should have one parameter of type boolean.",
                        interfaceClassName + "#" + methodName + "()");
            } else if (!typesEqual(Boolean.TYPE, methodElement.getParameters().get(0).asType())) {
                throw new ProcessorException(enclosingTypeElement, "Method @%s annotated with OnChecked should have one parameter of type boolean.",
                        interfaceClassName + "#" + methodName + "()");
            }

            if (methodName.startsWith(EventsDelegateClass.PREFIX_ON) && methodName.endsWith(EventsDelegateClass.POSTFIX_CHECKED)) {
                final int beginIndex = 2;
                final int endIndex = methodName.length() - EventsDelegateClass.POSTFIX_CHECKED.length();
                final String tag = methodName.substring(beginIndex, endIndex);

                final EventHandlerMethod method = new EventHandlerMethod(methodElement, EventHandlerMethod.EventType.ON_CHECKED, tag);
                eventsDelegateClass.addEventHandlerMethod(method);
            } else {
                throw new ProcessorException(enclosingTypeElement, "Method @%s does not follow naming convention: on[tag]Checked",
                        interfaceClassName + "#" + methodName + "(boolean)");
            }
        } catch (ProcessorException e) {
            handleError(e.getElement(), e.getMessage());
        }
    }

    private void handleOnClickAnnotation(final Element annotatedElement, final TypeElement enclosingTypeElement) {
        final ExecutableElement methodElement = (ExecutableElement)annotatedElement;
        try {
            if (enclosingTypeElement.getKind() != ElementKind.INTERFACE || !isPresenterInterface(enclosingTypeElement)) {
                throw new ProcessorException(enclosingTypeElement, "Only Presenter interface methods can be annotated with @%s",
                        OnClick.class.getSimpleName());
            }

            final String interfaceClassName = enclosingTypeElement.getQualifiedName().toString();
            EventsDelegateClass eventsDelegateClass = mEventsDelegateClasses.get(interfaceClassName);

            if (eventsDelegateClass == null) {
                eventsDelegateClass = new EventsDelegateClass(enclosingTypeElement);
                mEventsDelegateClasses.put(interfaceClassName, eventsDelegateClass);
            }

            final String methodName = methodElement.getSimpleName().toString();

            if (!methodElement.getParameters().isEmpty()) {
                throw new ProcessorException(enclosingTypeElement, "Method @%s annotated with OnClick is not allowed to have parameters.",
                        interfaceClassName + "#" + methodName + "()");
            }

            if (methodName.startsWith(EventsDelegateClass.PREFIX_ON) && methodName.endsWith(EventsDelegateClass.POSTFIX_CLICK)) {
                final int beginIndex = 2;
                final int endIndex = methodName.length() - EventsDelegateClass.POSTFIX_CLICK.length();
                final String tag = methodName.substring(beginIndex, endIndex);

                final EventHandlerMethod method = new EventHandlerMethod(methodElement, EventHandlerMethod.EventType.ON_CLICK, tag);
                eventsDelegateClass.addEventHandlerMethod(method);
            } else {
                throw new ProcessorException(enclosingTypeElement, "Method @%s does not follow naming convention: on[tag]Click",
                        interfaceClassName + "#" + methodName + "()");
            }
        } catch (ProcessorException e) {
            handleError(e.getElement(), e.getMessage());
        }
    }

    private void handleOnTextChangedAnnotation(final Element annotatedElement, final TypeElement enclosingTypeElement) {
        final ExecutableElement methodElement = (ExecutableElement)annotatedElement;
        try {
            if (enclosingTypeElement.getKind() != ElementKind.INTERFACE || !isPresenterInterface(enclosingTypeElement)) {
                throw new ProcessorException(enclosingTypeElement, "Only Presenter interface methods can be annotated with @%s",
                        OnTextChanged.class.getSimpleName());
            }

            final String interfaceClassName = enclosingTypeElement.getQualifiedName().toString();
            EventsDelegateClass eventsDelegateClass = mEventsDelegateClasses.get(interfaceClassName);

            if (eventsDelegateClass == null) {
                eventsDelegateClass = new EventsDelegateClass(enclosingTypeElement);
                mEventsDelegateClasses.put(interfaceClassName, eventsDelegateClass);
            }

            final String methodName = methodElement.getSimpleName().toString();

            if (methodElement.getParameters().size() != 1) {
                throw new ProcessorException(enclosingTypeElement, "Method @%s annotated with OnTextChanged should have one parameter of type String.",
                        interfaceClassName + "#" + methodName + "()");
            } else if (!typesEqual(String.class, methodElement.getParameters().get(0).asType())) {
                throw new ProcessorException(enclosingTypeElement, "Method @%s annotated with OnTextChanged should have one parameter of type String.",
                        interfaceClassName + "#" + methodName + "()");
            }

            if (methodName.startsWith(EventsDelegateClass.PREFIX_ON) && methodName.endsWith(EventsDelegateClass.POSTFIX_TEXT_CHANGED)) {
                final int beginIndex = 2;
                final int endIndex = methodName.length() - EventsDelegateClass.POSTFIX_TEXT_CHANGED.length();
                final String tag = methodName.substring(beginIndex, endIndex);

                final EventHandlerMethod method = new EventHandlerMethod(methodElement, EventHandlerMethod.EventType.ON_TEXT_CHANGED, tag);
                eventsDelegateClass.addEventHandlerMethod(method);
            } else {
                throw new ProcessorException(enclosingTypeElement, "Method @%s does not follow naming convention: on[tag]TextChanged",
                        interfaceClassName + "#" + methodName + "(String)");
            }
        } catch (ProcessorException e) {
            handleError(e.getElement(), e.getMessage());
        }
    }

    /**
     * Print the specified error message
     *
     * @param element The {@link Element} for which the error was detected. May be {@code null}.
     * @param errorMessage A {@link String} containing the error message.
     */
    public void handleError(final Element element, final String errorMessage) {
        mMessager.printMessage(Diagnostic.Kind.ERROR, errorMessage, element);
    }

    @SuppressWarnings("unchecked")
    private static boolean isPresenterInterface(final TypeElement interfaceElement) {

        if (interfaceElement == null) {
            return false;
        }

        final String className = interfaceElement.getQualifiedName().toString();

        if (className.startsWith("java.lang")) {
            return false;
        }

        if (className.contentEquals(Presenter.class.getName())) {
            return true;
        }

        final List<? extends TypeMirror> interfaces = interfaceElement.getInterfaces();

        for (final TypeMirror interfaceTypeMirror : interfaces) {
            if (isPresenterInterface((TypeElement) ((DeclaredType) interfaceTypeMirror).asElement()))  {
                return true;
            }
        }
        return false;
    }

    private boolean typesEqual(final Class<?> type, final TypeMirror typeMirror) {
        return type.getName().equals(typeMirror.toString());
    }
}