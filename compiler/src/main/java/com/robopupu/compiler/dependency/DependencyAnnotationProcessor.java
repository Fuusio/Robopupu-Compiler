package com.robopupu.compiler.dependency;

import com.google.auto.service.AutoService;

import com.robopupu.api.dependency.DependencyScope;
import com.robopupu.api.dependency.Provides;
import com.robopupu.api.dependency.Scope;
import com.robopupu.compiler.util.ProcessorException;

import java.io.IOException;
import java.util.ArrayList;
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
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

/**
 * {@link DependencyAnnotationProcessor} implements {@link AbstractProcessor}
 * to generate runtime objects from classes annotated using (@link Provides} and scoping
 * annotations.
 */
@AutoService(Processor.class)
public class DependencyAnnotationProcessor extends AbstractProcessor {

    private static String[] PROVIDED_TYPES = {

        "android.accounts.AccountManager",
        "android.app.ActivityManager",
        "android.app.AlarmManager",
        "android.app.Application",
        "android.bluetooth.BluetoothManager",
        "android.content.Context",
        "android.content.pm.PackageManager",
        "android.hardware.SensorManager",
        "android.location.LocationManager",
        "android.media.AudioManager",
        "android.nfc.NfcManager",
        "android.os.Vibrator",
        "android.view.WindowManager",
        "android.view.inputmethod.InputMethodManager",

        "com.robopupu.api.dependency.DependenciesCache",
        "com.robopupu.api.feature.FeatureManager",
        "com.robopupu.api.graphics.BitmapManager",
        "com.robopupu.api.model.ModelObjectManager",
        "com.robopupu.api.network.RequestManager",
        "com.robopupu.api.plugin.PluginBus",
        "com.robopupu.api.ui.action.ActionManager",
    };


    private HashMap<String, DependencyProviderClass> mDependencyProviderClasses;
    private Elements mElementUtils;
    private Filer mFiler;
    private Messager mMessager;
    private HashMap<String, Element> mProvidedTypes;
    private ArrayList<ProviderMethod> mProvidesMethods;
    private ArrayList<ProviderConstructor> mProviderConstructors;

    @Override
    public synchronized void init(final ProcessingEnvironment environment) {
        super.init(environment);

        mFiler = environment.getFiler();
        mElementUtils = environment.getElementUtils();
        mMessager = environment.getMessager();
        mProviderConstructors = new ArrayList<>();
        mProvidesMethods = new ArrayList<>();
        mProvidedTypes = new HashMap<>();
        mDependencyProviderClasses = new HashMap<>();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        final Set<String> annotations = new LinkedHashSet<>();
        annotations.add(Provides.class.getCanonicalName());
        annotations.add(Scope.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {

        // Process all classes, methods and constructors annotated with Scope
        try {

            for (final Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(Scope.class)) {

                final ElementKind elementKind = annotatedElement.getKind();
                final boolean isConstructorElement = elementKind == ElementKind.CONSTRUCTOR;
                final boolean isClassElement = elementKind == ElementKind.CLASS;

                if (!isConstructorElement && !isClassElement) {
                    throw new ProcessorException(annotatedElement, "Only constructors, and classes can be annotated with @%s",
                            Scope.class.getSimpleName());
                }

                if (isClassElement) {
                    final TypeElement classElement = (TypeElement) annotatedElement;
                    final String className = classElement.getQualifiedName().toString();

                    final List<? extends AnnotationMirror> annotationMirrors = annotatedElement.getAnnotationMirrors();
                    AnnotationMirror scopeAnnotationMirror = null;

                    for (final AnnotationMirror annotationMirror : annotationMirrors) {
                        if (annotationMirror.getAnnotationType().toString().contentEquals(Scope.class.getName())) {
                            scopeAnnotationMirror = annotationMirror;
                        }
                    }

                    final Collection<? extends AnnotationValue> values = scopeAnnotationMirror.getElementValues().values();
                    final int valuesCount = values.size();

                    if (valuesCount == 0 && !isDependencyScopeClass(classElement)) {
                        throw new ProcessorException(annotatedElement, "Only classes derived from DependencyScope can be annotated with @%s",
                                Scope.class.getSimpleName());
                    }

                    if (valuesCount == 0) {
                        final DependencyProviderClass dependencyProviderClass = new DependencyProviderClass(classElement);
                        mDependencyProviderClasses.put(className, dependencyProviderClass);
                    }
                }
            }
        } catch (com.robopupu.compiler.util.ProcessorException e) {
            handleError(e.getElement(), e.getMessage());
        }

        // Process all methods and constructors annotated with Provides
        try {

            for (final Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(Provides.class)) {

                final ElementKind elementKind = annotatedElement.getKind();
                final boolean isMethodElement = (elementKind == ElementKind.METHOD);
                final boolean isConstructorElement = (elementKind == ElementKind.CONSTRUCTOR);
                final boolean isClassElement = (elementKind == ElementKind.CLASS);

                if (!isMethodElement && !isConstructorElement && !isClassElement) {
                    throw new ProcessorException(annotatedElement, "Only methods, constructors, and classes can be annotated with @%s",
                            Provides.class.getSimpleName());
                }

                final List<? extends AnnotationMirror> annotationMirrors = annotatedElement.getAnnotationMirrors();
                ProviderMethod providerMethod = null;
                ProviderConstructor providerConstructor = null;
                ProviderClass providerClass = null;
                String providedType;

                AnnotationMirror scopeAnnotationMirror = null;
                AnnotationMirror providesAnnotationMirror = null;

                for (final AnnotationMirror annotationMirror : annotationMirrors) {
                    if (annotationMirror.getAnnotationType().toString().contentEquals(Provides.class.getName())) {
                        providesAnnotationMirror = annotationMirror;
                    } else if (annotationMirror.getAnnotationType().toString().contentEquals(Scope.class.getName())) {
                        scopeAnnotationMirror = annotationMirror;
                    }
                }

                Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = null;

                if (providesAnnotationMirror != null) {
                    elementValues = providesAnnotationMirror.getElementValues();
                }

                Collection<? extends AnnotationValue> values = null;
                int valuesCount = 0;

                if (elementValues != null) {
                    values = elementValues.values();
                    valuesCount = values.size();
                }

                if (isMethodElement) {
                    final ExecutableElement executableElement = (ExecutableElement)annotatedElement;
                    providerMethod = new ProviderMethod(executableElement, valuesCount == 1 ? providesAnnotationMirror : null);
                    mProvidesMethods.add(providerMethod);
                    providedType = providerMethod.getProvidedType();
                } else if (isConstructorElement) {
                    final ExecutableElement executableElement = (ExecutableElement)annotatedElement;
                    providerConstructor = new ProviderConstructor(executableElement, valuesCount == 1 ? providesAnnotationMirror : null);
                    mProviderConstructors.add(providerConstructor);
                    providedType = providerConstructor.getProvidedType();
                } else {
                    final TypeElement typeElement = (TypeElement)annotatedElement;
                    providerClass = new ProviderClass(typeElement, valuesCount == 1 ? providesAnnotationMirror : null);
                    providedType = providerClass.getProvidedType();
                }

                if (valuesCount == 1) {
                    AnnotationValue value = values.iterator().next();
                    final String valueString = value.toString().replace(".class", "");

                    if (!valueString.equals(Object.class.getName())) {
                        providedType = valueString;
                    }
                }

                mProvidedTypes.put(providedType, annotatedElement);

                if (isConstructorElement) {
                    if (scopeAnnotationMirror != null) {
                        values = scopeAnnotationMirror.getElementValues().values();
                        valuesCount = values.size();

                        String scopeType;

                        if (valuesCount != 1) {
                            throw new ProcessorException(annotatedElement, "Scope annotation for a constructor has to define a DependencyScope class. Scope definition for @%s is invalid.",
                                    annotatedElement.toString());
                        } else {
                            final AnnotationValue value = values.iterator().next();
                            scopeType = value.toString().replace(".class", "");
                        }

                        final DependencyProviderClass dependencyProviderClass = mDependencyProviderClasses.get(scopeType);

                        if (dependencyProviderClass != null) {
                            dependencyProviderClass.addProviderConstructor(providerConstructor);
                        }
                    } else {
                        final ExecutableElement executableElement = (ExecutableElement)annotatedElement;
                        final String enclosingClassName = executableElement.getEnclosingElement().asType().toString();
                        final DependencyProviderClass dependencyProviderClass = getImplicitScopeClass(enclosingClassName);

                        if (dependencyProviderClass == null) {
                            throw new ProcessorException(annotatedElement, "No DependencyScope class annotated with @Scope was found in any parent package of the package of the class %s.",
                                    enclosingClassName);
                        }

                        dependencyProviderClass.addProviderConstructor(providerConstructor);
                    }
                } else  if (isMethodElement) {
                    final ExecutableElement executableElement = (ExecutableElement)annotatedElement;
                    final String scopeType = executableElement.getEnclosingElement().asType().toString();
                    final DependencyProviderClass dependencyProviderClass = mDependencyProviderClasses.get(scopeType);
                    dependencyProviderClass.addProviderMethod(providerMethod);
                } else {
                    if (scopeAnnotationMirror != null) {
                        values = scopeAnnotationMirror.getElementValues().values();
                        valuesCount = values.size();

                        String scopeType;

                        if (valuesCount != 1) {
                            throw new ProcessorException(annotatedElement, "Scope annotation for a class has to define a DependencyScope class. Scope definition for @%s is invalid.",
                                    annotatedElement.toString());
                        } else {
                            final AnnotationValue value = values.iterator().next();
                            scopeType = value.toString().replace(".class", "");
                        }

                        final DependencyProviderClass dependencyProviderClass = mDependencyProviderClasses.get(scopeType);

                        if (dependencyProviderClass != null) {
                            dependencyProviderClass.addProviderClass(providerClass);
                        }
                    } else {
                        final TypeElement typeElement = (TypeElement)annotatedElement;
                        final String className = typeElement.asType().toString();
                        final DependencyProviderClass dependencyProviderClass = getImplicitScopeClass(className);

                        if (dependencyProviderClass == null) {
                            throw new ProcessorException(annotatedElement, "No DependencyScope class annotated with @Scope was found in any parent package of the package of the class %s.",
                                    className);
                        }

                        dependencyProviderClass.addProviderClass(providerClass);
                    }
                }
            }
        } catch (com.robopupu.compiler.util.ProcessorException e) {
            handleError(e.getElement(), e.getMessage());
        }


        // Generate DependencyProvider implementations for all classes annotated with Scope
        try {

            for (final ProviderMethod method : mProvidesMethods) {
                validateProvidesMethodElement(method);
            }

            for (final ProviderConstructor constructor : mProviderConstructors) {
                validateProvidesConstructorElement(constructor);
            }

            for (final DependencyProviderClass dependencyProviderClass : mDependencyProviderClasses.values()) {
                validateScopeClass(dependencyProviderClass);
                dependencyProviderClass.generateCode(mElementUtils, mFiler);
            }
            mDependencyProviderClasses.clear();

        } catch (com.robopupu.compiler.util.ProcessorException e) {
            handleError(e.getElement(), e.getMessage());
        } catch (IOException e) {
            handleError(null, e.getMessage());
        }

        return true;
    }

    private DependencyProviderClass getImplicitScopeClass(final String enclosingClass) {

        for (final DependencyProviderClass dependencyProviderClass : mDependencyProviderClasses.values()) {
            final String packageName = dependencyProviderClass.getPackageName(mElementUtils);

            if (enclosingClass.startsWith(packageName)) {
                return dependencyProviderClass;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private boolean isDependencyScopeClass(final TypeElement classElement) {

        if (classElement == null) {
            return false;
        }

        final String className = classElement.getQualifiedName().toString();

        if (className.startsWith("java.lang")) {
            return false;
        }

        if (className.contentEquals(DependencyScope.class.getName())) {
            return true;
        }

        final TypeMirror superClass = classElement.getSuperclass();
        return isDependencyScopeClass((TypeElement) ((DeclaredType) superClass).asElement());
    }

    private void validateProvidesMethodElement(final ProviderMethod method) throws com.robopupu.compiler.util.ProcessorException {
        final ExecutableElement element = method.getExecutableElement();
        final TypeMirror returnType = element.getReturnType();
        final TypeKind typeKind = returnType.getKind();

        if (typeKind == TypeKind.VOID) {
            throw new ProcessorException(element, "Provides method cannot have void return type.");
        }

        if (typeKind.isPrimitive()) {
            throw new ProcessorException(element, "Provides method cannot have primitive return type.");
        }

        final List<? extends VariableElement> parameters = element.getParameters();

        if (!parameters.isEmpty()) {

            for (final VariableElement parameter : parameters) {
                final TypeMirror parameterType = parameter.asType();

                if (parameterType.getKind().isPrimitive()) {
                    throw new ProcessorException(element, "Provides method cannot have primitive type parameters.");
                }

                final String parameterTypeName = parameterType.toString();

                if (!isProvidedType(parameterTypeName)) {
                    throw new ProcessorException(element, "Provides method has a parameter whose type is not provided: " + parameterTypeName);
                }
            }
        }
    }

    private void validateProvidesConstructorElement(final ProviderConstructor constructor) throws com.robopupu.compiler.util.ProcessorException {
        final ExecutableElement element = constructor.getExecutableElement();
        final List<? extends VariableElement> parameters = element.getParameters();

        if (!parameters.isEmpty()) {

            for (final VariableElement parameter : parameters) {
                final TypeMirror parameterType = parameter.asType();

                if (parameterType.getKind().isPrimitive()) {
                    throw new ProcessorException(element, "Provides constructor cannot have primitive type parameters.");
                }

                final String parameterTypeName = parameterType.toString();

                if (!isProvidedType(parameterTypeName)) {
                    throw new ProcessorException(element, "Provides constructor has a parameter whose type is not provided: " + parameterTypeName);
                }
            }
        }
    }

    private void validateScopeClass(final DependencyProviderClass dependencyProviderClass) {
        // TODO
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

    private boolean isProvidedType(final String type) {
        if (mProvidedTypes.containsKey(type)) {
            return true;
        }
        for (int i = PROVIDED_TYPES.length - 1; i >= 0; i--) {
            if (PROVIDED_TYPES[i].contentEquals(type)) {
                return true;
            }
        }
        return false;
    }
}