/*
 * Copyright (C) 2001 - 2015 Marko Salmela, http://fuusio.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.robopupu.compiler.util;

public class JavaWriter {

    public static final String ANNOTATION_OVERRIDE = "@Override";

    private static final String INDENTATION = "    ";

    private final StringBuilder builder;

    private String indentation;
    private int indentationCount;

    public JavaWriter() {
        this(new StringBuilder());
    }

    public JavaWriter(final StringBuilder builder) {
        indentation = INDENTATION;
        this.builder = builder;
        indentationCount = 0;
    }

    public final String getIndentation() {
        return indentation;
    }

    public void setIndentation(final String indentation) {
        this.indentation = indentation;
    }

    public final int getIndentationCount() {
        return indentationCount;
    }

    public void setIndentationCount(final int count) {
        indentationCount = count;
    }

    public JavaWriter a(final String string) {
        return append(string);
    }

    public JavaWriter append(final String string) {
        builder.append(string);
        return this;
    }

    public JavaWriter append(final boolean value) {
        builder.append(Boolean.toString(value));
        return this;
    }

    public JavaWriter a(final boolean value) {
        return append(value);
    }

    public JavaWriter append(final float value) {
        builder.append(Float.toString(value));
        builder.append('f');
        return this;
    }

    public JavaWriter append(final long value) {
        builder.append(Long.toString(value));
        builder.append('L');
        return this;
    }

    public JavaWriter append(final int value) {
        builder.append(Integer.toString(value));
        return this;
    }

    public JavaWriter a(final int value) {
        return append(value);
    }

    public JavaWriter space() {
        builder.append(' ');
        return this;
    }

    public JavaWriter s() {
        return space();
    }


    public JavaWriter intend() {
        for (int i = 0; i < indentationCount; i++) {
            builder.append(indentation);
        }
        return this;
    }

    public JavaWriter newLine() {
        return newLine(true);
    }

    public JavaWriter n() {
        return newLine(true);
    }

    public JavaWriter n(final boolean indented) {
        return newLine(indented);
    }

    public JavaWriter newLine(final boolean indented) {
        builder.append('\n');

        if (indented) {
            intend();
        }
        return this;
    }

    public JavaWriter beginBlock() {
        builder.append('{');
        builder.append('\n');
        indentationCount++;
        intend();
        return this;
    }

    public JavaWriter bob() {
        return beginBlock();
    }

    public JavaWriter endBlock() {
        return endBlock(true);
    }

    public JavaWriter eob() {
        return endBlock(true);
    }

    public JavaWriter endBlock(final boolean newLine) {
        indentationCount--;
        intend();
        builder.append('}');

        if (newLine) {
            builder.append('\n');
        } else {
            builder.append(" ");
        }
        return this;
    }

    public JavaWriter openParenthesis() {
        builder.append('(');
        return this;
    }

    public JavaWriter closeParenthesis() {
        builder.append(')');
        return this;
    }

    public JavaWriter endStatement() {
        return endStatement(true);
    }

    public JavaWriter eos() {
        return endStatement(true);
    }

    public JavaWriter eos(final boolean intented) {
        return endStatement(intented);
    }

    public JavaWriter endStatement(final boolean intented) {
        builder.append(";\n");
        if (intented) {
            intend();
        }
        return this;
    }

    public JavaWriter keyword(final Keyword keyword) {
        keyword.write(this);
        return this;
    }

    public JavaWriter k(final Keyword keyword) {
        return keyword(keyword);
    }

    public JavaWriter writeImport(final String packageName) {
        Keyword.IMPORT.write(this);
        append(packageName);
        endStatement();
        return this;
    }

    public JavaWriter writePackage(final String packageName) {
        Keyword.PACKAGE.write(this);
        append(packageName);
        endStatement();
        return this;
    }

    public JavaWriter beginClass(final String name) {
        return beginClass(name, true);
    }

    public JavaWriter beginClass(final String name, final boolean isPublic) {
        return beginClass(name, null, isPublic);
    }

    public JavaWriter beginClass(final String name, final String superClass, final boolean isPublic) {

        if (isPublic) {
            Keyword.PUBLIC.write(this);
        } else {
            Keyword.PRIVATE.write(this);
        }
        Keyword.CLASS.write(this);
        append(name);

        if (superClass != null) {
            Keyword.EXTENDS.write(this);
            append(superClass);
        }
        space();
        beginBlock();
        return this;
    }

    public JavaWriter endClass() {
        return endBlock();
    }

    public String getCode() {
        return builder.toString();
    }

    public boolean isEmpty() {
        return builder.length() == 0;
    }

    public JavaWriter clear() {
        builder.setLength(0);
        return this;
    }

    public JavaWriter c() {
        builder.setLength(0);
        return this;
    }
}
