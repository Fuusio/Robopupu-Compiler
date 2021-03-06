package com.robopupu.compiler.plugin;

import com.robopupu.api.mvp.View;
import com.robopupu.api.mvp.ViewPlugInvoker;
import com.robopupu.api.plugin.PlugInterface;
import com.robopupu.api.plugin.PlugInvoker;
import com.robopupu.api.plugin.PlugMode;
import com.robopupu.compiler.util.JavaWriter;
import com.robopupu.compiler.util.Keyword;
import com.robopupu.compiler.util.StringToolkit;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
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

    private final TypeElement typeElement;
    private final boolean isViewInterface;

    private Types typeUtils;
    private PlugMode plugMode;

    public PlugInterfaceAnnotatedInterface(final TypeElement typeElement) throws com.robopupu.compiler.util.ProcessorException {
        this.typeElement = typeElement;
        isViewInterface = isViewInterface(this.typeElement);
    }

    public TypeElement getTypeElement() {
        return typeElement;
    }

    public PlugMode getPlugMode() {
        return plugMode;
    }

    public void setPlugMode(final PlugMode plugMode) {
        this.plugMode = plugMode;
    }

    public void generateCode(final ProcessingEnvironment environment, final Elements elementUtils, final Filer filer) throws IOException {
        typeUtils = environment.getTypeUtils();

        if (plugMode.isBroadcast()) {
            generateHandlerInvoker(environment, elementUtils, filer);
        }
        generatePlugInvoker(environment, elementUtils, filer);
    }

    private void generateHandlerInvoker(final ProcessingEnvironment environment, final Elements elementUtils, final Filer filer) throws IOException {

        final PackageElement packageElement = elementUtils.getPackageOf(typeElement);
        final String packageName = packageElement.isUnnamed() ? null : packageElement.getQualifiedName().toString();
        final ClassName interfaceName = ClassName.get(typeElement);
        final String suffixedClassName = typeElement.getSimpleName() + SUFFIX_HANDLER_INVOKER;
        final ParameterizedTypeName superClassName = ParameterizedTypeName.get(CLASS_HANDLER_INVOKER, interfaceName);

        final TypeSpec.Builder classBuilder = TypeSpec.classBuilder(suffixedClassName);
        classBuilder.superclass(superClassName);
        classBuilder.addSuperinterface(interfaceName);
        classBuilder.addModifiers(Modifier.PUBLIC);

        final TypeName looperTypeName = TypeVariableName.get("android.os.Looper");
        final TypeName handlerTypeName = TypeVariableName.get("android.os.Handler");
        final FieldSpec.Builder handlerFieldSpec = FieldSpec.builder(handlerTypeName, "handler", Modifier.PRIVATE, Modifier.FINAL);

        classBuilder.addField(handlerFieldSpec.build());

        final MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder();
        constructorBuilder.addModifiers(Modifier.PUBLIC);
        constructorBuilder.addStatement("handler = new $T($T.getMainLooper())", handlerTypeName, looperTypeName);
        classBuilder.addMethod(constructorBuilder.build());

        final List<? extends Element> enclosedElements = typeElement.getEnclosedElements();
        final List<ExecutableElement> methodElements = new ArrayList<>();

        for (final Element element : enclosedElements) {

            if (element.getKind() == ElementKind.METHOD) {
                final ExecutableElement methodElement = (ExecutableElement) element;
                final Set<Modifier> modifiers = methodElement.getModifiers();

                if (!modifiers.contains(Modifier.STATIC)) {
                    methodElements.add(methodElement);
                }
            }
        }

        final List<TypeMirror> interfaces = new ArrayList<>();

        for (final TypeMirror interfaceTypeMirror : typeElement.getInterfaces()) {
            collectInterfaces(interfaceTypeMirror, interfaces);
        }

        for (final TypeMirror interfaceTypeMirror : interfaces) {
            final TypeElement interfaceTypeElement = (TypeElement) typeUtils.asElement(interfaceTypeMirror);

            for (final Element element : interfaceTypeElement.getEnclosedElements()) {

                if (element.getKind() == ElementKind.METHOD) {
                    final ExecutableElement methodElement = (ExecutableElement) element;
                    final Set<Modifier> modifiers = methodElement.getModifiers();

                    if (!modifiers.contains(Modifier.STATIC)) {
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
                methodBuilder.beginControlFlow("handler.post(new Runnable()");
                methodBuilder.beginControlFlow("@Override public void run()");
                JavaWriter writer = new JavaWriter();

                writer.a("plugin.").a(methodName).a("(");

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

        final Element element = typeUtils.asElement(interfaceTypeMirror);
        final TypeElement typeElement = (TypeElement)element;

        for (final TypeMirror typeMirror : typeElement.getInterfaces()) {
            collectInterfaces(typeMirror, interfaces);
        }
    }

    private void generatePlugInvoker(final ProcessingEnvironment environment, final Elements elementUtils, final Filer filer) throws IOException {

        final PackageElement packageElement = elementUtils.getPackageOf(typeElement);
        final String packageName = packageElement.isUnnamed() ? null : packageElement.getQualifiedName().toString();
        final String suffixedClassName = typeElement.getSimpleName() + SUFFIX_PLUG_INVOKER;;
        final ClassName interfaceName = ClassName.get(typeElement);
        final ClassName pluginInvokerClass = isViewInterface ? CLASS_VIEW_PLUG_INVOKER : CLASS_PLUG_INVOKER;
        final ParameterizedTypeName superClassName = ParameterizedTypeName.get(pluginInvokerClass, interfaceName);

        final TypeSpec.Builder classBuilder = TypeSpec.classBuilder(suffixedClassName);
        classBuilder.superclass(superClassName);
        classBuilder.addModifiers(Modifier.PUBLIC);

        List<? extends TypeParameterElement> typeParameters = typeElement.getTypeParameters();

        if (!typeParameters.isEmpty()) {
            for (final TypeParameterElement typeParameter : typeParameters) {
                final String simpleName = typeParameter.getSimpleName().toString();
                final List<? extends TypeMirror> boundsMirrors = typeParameter.getBounds();
                final List<TypeName> boundsTypeNames = new ArrayList<>();

                for (TypeMirror typeMirror : boundsMirrors) {
                    boundsTypeNames.add(TypeName.get(typeMirror));
                }

                final TypeVariableName typeVariableName = TypeVariableName.get(simpleName).withBounds(boundsTypeNames);
                classBuilder.addTypeVariable(typeVariableName);

                final ParameterizedTypeName parameterizedInterfaceName = ParameterizedTypeName.get(interfaceName, TypeVariableName.get(simpleName));
                classBuilder.addSuperinterface(parameterizedInterfaceName);
            }
        } else {
            classBuilder.addSuperinterface(interfaceName);
        }

        final List<? extends Element> enclosedElements = typeElement.getEnclosedElements();
        final List<ExecutableElement> methodElements = new ArrayList<>();

        for (final Element element : enclosedElements) {

            if (element.getKind() == ElementKind.METHOD) {
                final ExecutableElement methodElement = (ExecutableElement) element;

                if (isInvokerMethodCreatedFor(methodElement)) {
                    methodElements.add(methodElement);
                }
            }
        }

        final List<TypeMirror> allInterfaces = new ArrayList<>();
        final List<? extends TypeMirror> extendedInterfaces = typeElement.getInterfaces();

        for (final TypeMirror interfaceTypeMirror : extendedInterfaces) {
            collectInterfaces(interfaceTypeMirror, allInterfaces);
        }

        final HashMap<String, TypeMirror> parameterTypeMappings = new HashMap<>();

        for (final TypeMirror interfaceTypeMirror : extendedInterfaces) {
            final DeclaredType interfaceDeclaredType = ((DeclaredType)interfaceTypeMirror);
            final TypeElement interfaceTypeElement = (TypeElement) typeUtils.asElement(interfaceTypeMirror);

            List<? extends TypeMirror> typeArguments = interfaceDeclaredType.getTypeArguments();
            typeParameters = interfaceTypeElement.getTypeParameters();

            if (!typeParameters.isEmpty()) {

                int index = 0;

                for (final TypeParameterElement typeParameter : typeParameters) {
                    final String simpleName = typeParameter.getSimpleName().toString();
                    parameterTypeMappings.put(simpleName, typeArguments.get(index++));
                }
            }
        }

        for (final TypeMirror interfaceTypeMirror : allInterfaces) {
            final TypeElement interfaceTypeElement = (TypeElement) typeUtils.asElement(interfaceTypeMirror);

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

            typeParameters = methodElement.getTypeParameters();

            if (!typeParameters.isEmpty()) {
                for (final TypeParameterElement typeParameter : typeParameters) {
                    final String simpleName = typeParameter.getSimpleName().toString();
                    final List<? extends TypeMirror> boundsMirrors = typeParameter.getBounds();
                    final List<TypeName> boundsTypeNames = new ArrayList<>();

                    for (TypeMirror typeMirror : boundsMirrors) {
                        boundsTypeNames.add(TypeName.get(typeMirror));
                    }

                    final TypeVariableName typeVariableName = TypeVariableName.get(simpleName).withBounds(boundsTypeNames);
                    methodBuilder.addTypeVariable(typeVariableName);
                }
            }

            if (returnsValue) {
                methodBuilder.returns(TypeName.get(returnType));
            }

            final List<? extends VariableElement> parameterElements = methodElement.getParameters();

            for (final VariableElement parameterElement : parameterElements) {
                TypeName parameterType = TypeName.get(parameterElement.asType());
                final String parameterName = parameterElement.getSimpleName().toString();
                final TypeMirror mappedType = parameterTypeMappings.get(parameterType.toString());

                if (mappedType != null) {
                    parameterType = ClassName.get((TypeElement)typeUtils.asElement(mappedType));
                }
                methodBuilder.addParameter(parameterType, parameterName, Modifier.FINAL);
            }

            final JavaWriter writer = new JavaWriter();

            boolean writeInvocation = true;

            if (returnsValue) {
                if (plugMode.isBroadcast()) {
                    writeInvocation = false;
                    methodBuilder.addStatement("throw new IllegalStateException(\"Invocation via a Handler to main thread cannot return a value.\")");
                } else {
                    methodBuilder.beginControlFlow("if (plugins.size() > 0)");
                    writer.k(com.robopupu.compiler.util.Keyword.RETURN).a("plugins.get(0).");
                }
            } else {
                if (plugMode.isBroadcast()) {
                    methodBuilder.beginControlFlow("for (int i = last(); i >= 0; i--)");
                    writer.a("plugins.get(i).");
                } else {
                    methodBuilder.beginControlFlow("if (plugins.size() > 0)");
                    writer.a("plugins.get(0).");
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

                if (plugMode.isBroadcast() && !returnsValue) {
                    methodBuilder.endControlFlow();
                } else {
                    methodBuilder.endControlFlow();

                    if (returnsValue) {
                        writer.clear();
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

        final Set<Modifier> modifiers = methodElement.getModifiers();

        if (modifiers.contains(Modifier.STATIC)) {
            return false;
        }

        if (isViewInterface) {
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
}
