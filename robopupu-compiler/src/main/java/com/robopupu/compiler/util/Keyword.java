package com.robopupu.compiler.util;

/**
 * {@link Keyword} is enum type defining the Java keywords.
 */
public enum Keyword {

    ABSTRACT("abstract"),
    CLASS("class"),
    ELSE("else"),
    EXTENDS("extends"),
    FINAL("final"),
    IF("if"),
    IMPORT("import"),
    IMPLEMENTS("implements"),
    INTERFACE("interface"),
    NEW("new"),
    NULL("null"),
    PACKAGE("package"),
    PRIVATE("private"),
    PROTECTED("protected"),
    PUBLIC("public"),
    RETURN("return"),
    STATIC("static"),
    SUPER("super"),
    THROW("throw"),
    THIS("this");

    private final String mKeyword;

    Keyword(final String keyword) {
        mKeyword = keyword;
    }

    public final String toString() {
        return mKeyword;
    }

    public JavaWriter write(final JavaWriter writer) {
        writer.append(mKeyword);
        writer.append(" ");
        return writer;
    }

    public JavaWriter w(final JavaWriter writer) {
        return write(writer);
    }
}
