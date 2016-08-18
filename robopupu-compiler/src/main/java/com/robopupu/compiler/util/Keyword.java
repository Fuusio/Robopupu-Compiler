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

    private final String keyword;

    Keyword(final String keyword) {
        this.keyword = keyword;
    }

    public final String toString() {
        return keyword;
    }

    public JavaWriter write(final JavaWriter writer) {
        writer.append(keyword);
        writer.append(" ");
        return writer;
    }

    public JavaWriter w(final JavaWriter writer) {
        return write(writer);
    }
}
