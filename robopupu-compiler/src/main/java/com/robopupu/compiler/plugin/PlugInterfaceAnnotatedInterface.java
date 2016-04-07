package com.robopupu.compiler.plugin;

import com.robopupu.api.mvp.View;
import com.robopupu.api.mvp.ViewPlugInvoker;
import com.robopupu.compiler.util.Keyword;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import com.robopupu.api.plugin.PlugInterface;
import com.robopupu.api.plugin.PlugInvoker;
import com.robopupu.api.plugin.PlugMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * {@link PlugInterfaceAnnotatedInterface} is a model class used for storing information about an interface
 * annotated with {@link PlugInterface}. The class is also used for generate Java code from
 * the annotated interface.
 */
public class PlugInterfaceAnnotatedInterface {

    private static final String SUFFIX_HANDLER_INVOKER = "_HandlerInvoker";
    private static final String SUFFIX_PLUG_INVOKER = "_PlugInvoker";
    private static final ClassName CLASS_HANDLER_INVOKER = ClassName.get("com.robopupu.api.plugin", "HandlerInvoker");
    private static final ClassName CLASS_PLUG_INVOKER = ClassName.get(PlugInvoker.class);
    private static final ClassName CLASS_VIEW_PLUG_INVOKER = ClassName.get(ViewPlugInvoker.class);

    private final TypeElement mTypeElement;
    private final boolean mIsViewInterface;

    private Types mTypeUtils;
    private PlugMode mPlugMode;

    public PlugInterfaceAnnotatedInterface(final TypeElement typeElement) throws com.robopupu.compiler.util.ProcessorException {
        mTypeElement = typeElement;
        mIsViewInterface = isViewInterface(mTypeElement);
    }

    public TypeElement getTypeElement() {
        return mTypeElement;
    }

    public PlugMode getPlugMode() {
        return mPlugMode;
    }

    public void setPlugMode(final PlugMode plugMode) {
        mPlugMode = plugMode;
    }

    public void generateCode(final ProcessingEnvironment environment, final Elements elementUtils, final Filer filer) throws IOException {
        mTypeUtils = environment.getTypeUtils();

        if (mPlugMode.isBroadcast()) {
            generateHandlerInvoker(environment, elementUtils, filer);
        }
        generatePlugInvoker(environment, elementUtils, filer);
    }

    private void generateHandlerInvoker(final ProcessingEnvironment environment, final Elements elementUtils, final Filer filer) throws IOException {

        final PackageElement packageElement = elementUtils.getPackageOf(mTypeElement);
        final String packageName = packageElement.isUnnamed() ? null : packageElement.getQualifiedName().toString();
        final ClassName interfaceName = ClassName.get(mTypeElement);
        final String suffixedClassName = mTypeElement.getSimpleName() + SUFFIX_HANDLER_INVOKER;
        final ParameterizedTypeName superClassName = ParameterizedTypeName.get(CLASS_HANDLER_INVOKER, interfaceName);

        final TypeSpec.Builder classBuilder = TypeSpec.classBuilder(suffixedClassName);
        classBuilder.superclass(superClassName);
        classBuilder.addSuperinterface(interfaceName);
        classBuilder.addModifiers(Modifier.PUBLIC);

        final TypeName looperTypeName = TypeVariableName.get("android.os.Looper");
        final TypeName handlerTypeName = TypeVariableName.get("android.os.Handler");
        final FieldSpec.Builder handlerFieldSpec = FieldSpec.builder(handlerTypeName, "mHandler", Modifier.PRIVATE, Modifier.FINAL);

        classBuilder.addField(handlerFieldSpec.build());

        final MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder();
        constructorBuilder.addModifiers(Modifier.PUBLIC);
        constructorBuilder.addStatement("mHandler = new $T($T.getMainLooper())", handlerTypeName, looperTypeName);
        classBuilder.addMethod(constructorBuilder.build());

        final List<? extends Element> enclosedElements = mTypeElement.getEnclosedElements();
        final List<ExecutableElement> methodElements = new ArrayList<>();

        for (final Element element : enclosedElements) {

            if (element.getKind() == ElementKind.METHOD) {
                methodElements.add((ExecutableElement) element);
            }
        }

        final List<TypeMirror> interfaces = new ArrayList<>();

        for (final TypeMirror interfaceTypeMirror : mTypeElement.getInterfaces()) {
            collectInterfaces(interfaceTypeMirror, interfaces);
        }

        for (final TypeMirror interfaceTypeMirror : interfaces) {
            final TypeElement interfaceTypeElement = (TypeElement)mTypeUtils.asElement(interfaceTypeMirror);

            for (final Element element : interfaceTypeElement.getEnclosedElements()) {

                if (element.getKind() == ElementKind.METHOD) {
                    methodElements.add((ExecutableElement) element);
                }
            }
        }

        for (final ExecutableElement methodElement : methodElements) {

            final String methodName = methodElement.getSimpleName().toString();
            final TypeMirror returnType = methodElement.getReturnType();
            final boolean returnsValue = returnType.getKind() != TypeKind.VOID;
            final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName);
            methodBuilder.addModifiers(Modifier.PUBLIC);
            methodBuilder.addAnnotation(Override.class);

            final List<? extends VariableElement> parameterElements = methodElement.getParameters();

            for (final VariableElement parameterElement : parameterElements) {
                final TypeName type = TypeName.get(parameterElement.asType());
                methodBuilder.addParameter(type, parameterElement.getSimpleName().toString(), Modifier.FINAL);
            }

            if (returnsValue) {
                methodBuilder.returns(TypeName.get(returnType));
                methodBuilder.addStatement("throw new IllegalStateException(\"Invocation via a Handler to main thread cannot return a value.\")");
                classBuilder.addMethod(methodBuilder.build());
            } else {
                methodBuilder.beginControlFlow("mHandler.post(new Runnable()");
                methodBuilder.beginControlFlow("@Override public void run()");
                com.robopupu.compiler.util.JavaWriter writer = new com.robopupu.compiler.util.JavaWriter();

                writer.a("mPlugin.").a(methodName).a("(");

                final int parameterCount = parameterElements.size();

                if (parameterCount > 0) {

                    int index = 0;

                    for (final VariableElement parameterElement : parameterElements) {
                        writer.a(parameterElement.getSimpleName().toString());

                        if (index < parameterCount - 1) {
                            writer.a(", ");
                        }
                        index++;
                    }
                }

                writer.a(")");

                methodBuilder.addStatement(writer.getCode());
                methodBuilder.endControlFlow();
                methodBuilder.endControlFlow();
                methodBuilder.addStatement(")");
                classBuilder.addMethod(methodBuilder.build());
            }
        }

        final TypeSpec typeSpec = classBuilder.build();
        JavaFile.builder(packageName, typeSpec).build().writeTo(filer);
    }

    @SuppressWarnings("unchecked")
    private void collectInterfaces(final TypeMirror interfaceTypeMirror, final List<TypeMirror> interfaces) {
        interfaces.add(interfaceTypeMirror);

        final Element element = mTypeUtils.asElement(interfaceTypeMirror);
        final TypeElement typeElement = (TypeElement)element;

        for (final TypeMirror typeMirror : typeElement.getInterfaces()) {
            collectInterfaces(typeMirror, interfaces);
        }
    }

    private void generatePlugInvoker(final ProcessingEnvironment environment, final Elements elementUtils, final Filer filer) throws IOException {

        final PackageElement packageElement = elementUtils.getPackageOf(mTypeElement);
        final String packageName = packageElement.isUnnamed() ? null : packageElement.getQualifiedName().toString();
        final ClassName interfaceName = ClassName.get(mTypeElement);
        final String suffixedClassName = mTypeElement.getSimpleName() + SUFFIX_PLUG_INVOKER;
        final ClassName pluginInvokerClass = mIsViewInterface ? CLASS_VIEW_PLUG_INVOKER : CLASS_PLUG_INVOKER;
        final ParameterizedTypeName superClassName = ParameterizedTypeName.get(pluginInvokerClass, interfaceName);

        final TypeSpec.Builder classBuilder = TypeSpec.classBuilder(suffixedClassName);
        classBuilder.superclass(superClassName);
        classBuilder.addSuperinterface(interfaceName);
        classBuilder.addModifiers(Modifier.PUBLIC);

        final List<? extends Element> enclosedElements = mTypeElement.getEnclosedElements();
        final List<ExecutableElement> methodElements = new ArrayList<>();

        for (final Element element : enclosedElements) {

            if (element.getKind() == ElementKind.METHOD) {
                final ExecutableElement methodElement = (ExecutableElement) element;

                if (isInvokerMethodCreatedFor(methodElement)) {
                    methodElements.add(methodElement);
                }
            }
        }

        final List<TypeMirror> interfaces = new ArrayList<>();

        for (final TypeMirror interfaceTypeMirror : mTypeElement.getInterfaces()) {
            collectInterfaces(interfaceTypeMirror, interfaces);
        }

        for (final TypeMirror interfaceTypeMirror : interfaces) {
            final TypeElement interfaceTypeElement = (TypeElement)mTypeUtils.asElement(interfaceTypeMirror);

            for (final Element element : interfaceTypeElement.getEnclosedElements()) {
                if (element.getKind() == ElementKind.METHOD) {
                    final ExecutableElement methodElement = (ExecutableElement) element;

                    if (isInvokerMethodCreatedFor(methodElement)) {
                        methodElements.add(methodElement);
                    }
                }
            }
        }

        for (final ExecutableElement methodElement : methodElements) {
            final String methodName = methodElement.getSimpleName().toString();
            final TypeMirror returnType = methodElement.getReturnType();
            final boolean returnsValue = returnType.getKind() != TypeKind.VOID;
            final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName);
            methodBuilder.addModifiers(Modifier.PUBLIC);
            methodBuilder.addAnnotation(Override.class);

            if (returnsValue) {
                methodBuilder.returns(TypeName.get(returnType));
            }

            final List<? extends VariableElement> parameterElements = methodElement.getParameters();

            for (final VariableElement parameterElement : parameterElements) {
                final TypeName type = TypeName.get(parameterElement.asType());
                methodBuilder.addParameter(type, parameterElement.getSimpleName().toString(), Modifier.FINAL);
            }

            final com.robopupu.compiler.util.JavaWriter writer = new com.robopupu.compiler.util.JavaWriter();

            boolean writeInvocation = true;

            if (returnsValue) {
                if (mPlugMode.isBroadcast()) {
                    writeInvocation = false;
                    methodBuilder.addStatement("throw new IllegalStateException(\"Invocation via a Handler to main thread cannot return a value.\")");
                } else {
                    methodBuilder.beginControlFlow("if (mPlugins.size() > 0)");
                    writer.k(com.robopupu.compiler.util.Keyword.RETURN).a("mPlugins.get(0).");
                }
            } else {
                if (mPlugMode.isBroadcast()) {
                    methodBuilder.beginControlFlow("for (int i = last(); i >= 0; i--)");
                    writer.a("mPlugins.get(i).");
                } else {
                    methodBuilder.beginControlFlow("if (mPlugins.size() > 0)");
                    writer.a("mPlugins.get(0).");
                }
            }

            if (writeInvocation) {
                writer.a(methodName).a("(");

                final int parameterCount = parameterElements.size();

                if (parameterCount > 0) {

                    int index = 0;

                    for (final VariableElement parameterElement : parameterElements) {
                        writer.a(parameterElement.getSimpleName().toString());

                        if (index < parameterCount - 1) {
                            writer.a(", ");
                        }
                        index++;
                    }
                }

                writer.a(")");

                methodBuilder.addStatement(writer.getCode());

                if (mPlugMode.isBroadcast() && !returnsValue) {
                    methodBuilder.endControlFlow();
                } else {
                    methodBuilder.endControlFlow();

                    if (returnsValue) {
                        writer.clear();
                        // writer.k(com.robopupu.compiler.util.Keyword.THROW).k(com.robopupu.compiler.util.Keyword.NEW).a("NullPointerException(\"Invocation target not available.\")");
                        writer.a("handleInvocationTargetNotAvailable(").a(returnsValue).a(")");
                        methodBuilder.addStatement(writer.getCode());

                        writer.clear();
                        writer.k(Keyword.RETURN).a(getDefaultReturnValue(returnType));
                        methodBuilder.addStatement(writer.getCode());
                    }
                }
            }

            classBuilder.addMethod(methodBuilder.build());
        }

        final TypeSpec typeSpec = classBuilder.build();
        JavaFile.builder(packageName, typeSpec).build().writeTo(filer);
    }

    private boolean isInvokerMethodCreatedFor(final ExecutableElement methodElement) {
        if (mIsViewInterface) {
            if (returnsValue(methodElement)) {
                final String methodName = methodElement.getSimpleName().toString();
                final int parameterCount = getParameterCount(methodElement);

                if (parameterCount == 0 && (methodName.contentEquals("getState") || methodName.contentEquals("getViewTag"))) {
                    return false;
                }
            }
        }
        return true;
    }

    private int getParameterCount(final ExecutableElement methodElement) {
        final List<? extends VariableElement> parameterElements = methodElement.getParameters();
        return parameterElements.size();
    }

    private boolean returnsValue(final ExecutableElement methodElement) {
        final TypeMirror returnType = methodElement.getReturnType();
        return returnType.getKind() != TypeKind.VOID;
    }

    private String getDefaultReturnValue(final TypeMirror returnType) {

        if (!returnType.getKind().isPrimitive()) {
            return "null";
        } else {
            switch(returnType.getKind()) {
                case BOOLEAN: {
                    return "false";
                }
                case CHAR: {
                    return "'0'";
                }
                case BYTE:
                case SHORT:
                case INT:
                case LONG:
                case FLOAT:
                case DOUBLE: {
                    return "0";
                }
                default: {
                    return "null";
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static boolean isViewInterface(final TypeElement interfaceElement) {

        if (interfaceElement == null) {
            return false;
        }

        final String className = interfaceElement.getQualifiedName().toString();

        if (className.startsWith("java.lang")) {
            return false;
        }

        if (className.contentEquals(View.class.getName())) {
            return true;
        }

        final List<? extends TypeMirror> interfaces = interfaceElement.getInterfaces();

        for (final TypeMirror interfaceTypeMirror : interfaces) {
            if (isViewInterface((TypeElement) ((DeclaredType) interfaceTypeMirror).asElement()))  {
                return true;
            }
        }
        return false;
    }

    private boolean typesEqual(final Class<?> type, final TypeMirror typeMirror) {
        return type.getName().equals(typeMirror.toString());
    }
}
