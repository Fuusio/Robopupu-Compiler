package com.robopupu.compiler.plugin;

import com.google.auto.service.AutoService;

import com.robopupu.api.plugin.Plug;
import com.robopupu.api.plugin.PlugInterface;
import com.robopupu.api.plugin.PlugMode;
import com.robopupu.api.plugin.Plugin;
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
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

/**
 * {@link PluginAnnotationProcessor} implements {@link AbstractProcessor}
 * to generate runtime objects from classes annotated using (@link PluginInterface} and
 * {@link Plugin} annotations.
 */
@AutoService(Processor.class)
public class PluginAnnotationProcessor extends AbstractProcessor {

    private HashMap<String, PlugInterfaceAnnotatedInterface> mPlugInterfaceAnnotatedInterfaces;
    private HashMap<String, PluginAnnotatedClass> mPluginAnnotatedClasses;

    private Filer mFiler;
    private Elements mElementUtils;
    private ProcessingEnvironment mProcessingEnvironment;
    private Messager mMessager;

    @Override
    public synchronized void init(final ProcessingEnvironment environment) {
        super.init(environment);

        mFiler = environment.getFiler();
        mElementUtils = environment.getElementUtils();
        mMessager = environment.getMessager();
        mPluginAnnotatedClasses = new HashMap<>();
        mPlugInterfaceAnnotatedInterfaces = new HashMap<>();
        mProcessingEnvironment = environment;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        final Set<String> annotations = new LinkedHashSet<>();
        annotations.add(Plugin.class.getCanonicalName());
        annotations.add(PlugInterface.class.getCanonicalName());
        return annotations;
    }

    @Override public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {

        // Process interfaces and classes annodated with @PlugInterface
        try {
            for (final Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(PlugInterface.class)) {

                if (annotatedElement.getKind() == ElementKind.INTERFACE) {
                    final TypeElement typeElement = (TypeElement) annotatedElement;
                    handlePlugInterfaceAnnotatedInterface(typeElement);
                } else {
                    throw new ProcessorException(annotatedElement, "Only interfaces can be annotated with @%s",
                            PlugInterface.class.getSimpleName());
                }
            }

            for (final PlugInterfaceAnnotatedInterface annotatedInterface : mPlugInterfaceAnnotatedInterfaces.values()) {
                annotatedInterface.generateCode(mProcessingEnvironment, mElementUtils, mFiler);
            }
        } catch (ProcessorException e) {
            handleError(e.getElement(), e.getMessage());
        } catch (IOException e) {
            handleError(null, e.getMessage());
        }

        // Process interfaces and classes annodated with @Plugin
        try {

            for (final Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(Plugin.class)) {

                if (annotatedElement.getKind() == ElementKind.CLASS) {
                    final TypeElement typeElement = (TypeElement) annotatedElement;
                    handlePluginAnnotatedClass(typeElement);
                } else {
                    throw new ProcessorException(annotatedElement, "Only classes can be annotated with @%s",
                            Plugin.class.getSimpleName());
                }
            }

            for (final PluginAnnotatedClass annotatedClass : mPluginAnnotatedClasses.values()) {
                annotatedClass.generateCode(mProcessingEnvironment, mElementUtils, mFiler);
            }

            mPluginAnnotatedClasses.clear();
            mPlugInterfaceAnnotatedInterfaces.clear();
        } catch (ProcessorException e) {
            handleError(e.getElement(), e.getMessage());
        } catch (IOException e) {
            handleError(null, e.getMessage());
        }

        return true;
    }

    private void handlePluginAnnotatedClass(final TypeElement typeElement) {
        try {
            final PluginAnnotatedClass annotatedClass = new PluginAnnotatedClass(typeElement);
            final String className = typeElement.getQualifiedName().toString();

            mPluginAnnotatedClasses.put(className, annotatedClass);

            List<? extends TypeMirror> interfaces  = typeElement.getInterfaces();

            for (final TypeMirror interfaceType : interfaces) {
                final String interfaceName = interfaceType.toString();
                final PlugInterfaceAnnotatedInterface interfaceClass = mPlugInterfaceAnnotatedInterfaces.get(interfaceName);

                if (interfaceClass != null) {
                    annotatedClass.addPlugInterface(interfaceName, interfaceClass);
                }
            }

            List<? extends Element> enclosedElements = typeElement.getEnclosedElements();

            for (final Element element : enclosedElements) {
                if (element.getKind() == ElementKind.FIELD)  {
                    final VariableElement fieldElement = (VariableElement) element;
                    final Set<Modifier> modifiers = fieldElement.getModifiers();

                    if (!modifiers.contains(Modifier.FINAL) && !modifiers.contains(Modifier.STATIC)) {
                        final List<? extends AnnotationMirror> annotationMirrors = fieldElement.getAnnotationMirrors();

                        AnnotationMirror plugAnnotationMirror = null;

                        for (final AnnotationMirror annotationMirror : annotationMirrors) {
                            if (annotationMirror.getAnnotationType().toString().contentEquals(Plug.class.getName())) {
                                plugAnnotationMirror = annotationMirror;
                                break;
                            }
                        }

                        if (plugAnnotationMirror != null) {
                            String scopeClass = null;
                            final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = plugAnnotationMirror.getElementValues();
                            final Collection<? extends AnnotationValue> values = elementValues.values();

                            if (values.size() == 1) {
                                final AnnotationValue value = values.iterator().next();
                                scopeClass = value.toString().replace(".class", "");
                            }
                            annotatedClass.addPlugField(fieldElement.getSimpleName().toString(), fieldElement.asType(), scopeClass);
                        }
                    }
                }
            }
        } catch (ProcessorException e) {
            handleError(e.getElement(), e.getMessage());
        }
    }

    private void handlePlugInterfaceAnnotatedInterface(final TypeElement typeElement) {
        try {
            final PlugInterfaceAnnotatedInterface annotatedInterface = new PlugInterfaceAnnotatedInterface(typeElement);
            final String className = typeElement.getQualifiedName().toString();

            mPlugInterfaceAnnotatedInterfaces.put(className, annotatedInterface);

            final List<? extends AnnotationMirror> annotationMirrors = typeElement.getAnnotationMirrors();

            AnnotationMirror plugInterfaceAnnotationMirror = null;

            for (final AnnotationMirror annotationMirror : annotationMirrors) {
                if (annotationMirror.getAnnotationType().toString().contentEquals(PlugInterface.class.getName())) {
                    plugInterfaceAnnotationMirror = annotationMirror;
                    break;
                }
            }

            if (plugInterfaceAnnotationMirror != null) {
                final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = plugInterfaceAnnotationMirror.getElementValues();
                final Collection<? extends AnnotationValue> values = elementValues.values();

                PlugMode plugMode = PlugMode.REFERENCE;

                if (values.size() == 1) {
                    final AnnotationValue value = values.iterator().next();
                    final String stringValue = value.toString();

                    if (stringValue.contains("BROADCAST")) {
                        plugMode = PlugMode.BROADCAST;
                    }
                }
                annotatedInterface.setPlugMode(plugMode);
            }

            mPlugInterfaceAnnotatedInterfaces.put(className, annotatedInterface);
        } catch (ProcessorException e) {
            handleError(e.getElement(), e.getMessage());
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