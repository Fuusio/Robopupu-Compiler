/*
 * Copyright (C) 2001 - 2015 Marko Salmela, http://robopupu.com
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
package com.robopupu.api.util;

import java.nio.charset.Charset;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class StringToolkit {

    public static final Charset CHARSET_UTF8 = Charset.forName("UTF8");

    public static final String INVALID_FILENAME_CHARS = "|\\?*<\":>+[]/'";

    public static String createValidIdentifier(final String string) {
        return createValidIdentifier(string, '_');
    }

    public static String createValidIdentifier(final String string, final char replaceChar) {
        if (string == null) {
            throw new IllegalArgumentException();
        }

        final StringBuilder identifier = new StringBuilder();
        final int length = string.length();

        for (int i = 0; i < length; i++) {
            final char character = string.charAt(i);

            if (character == '.' || Character.isJavaIdentifierPart(character)) {
                identifier.append(character);
            } else {
                identifier.append(replaceChar);
            }
        }

        return identifier.toString();
    }

    public static boolean containsOnlyWhitespaces(final String string, final int start, final int length) {
        if (string == null) {
            return true;
        }
        final char[] buffer = new char[length - start];
        string.getChars(start, start + length, buffer, 0);
        return containsOnlyWhitespaces(buffer, 0, length - start);
    }

    public static boolean containsOnlyWhitespaces(final char[] chars, final int start, final int length) {
        final String string = new String(chars, start, length);
        final int stringLength = string.length();

        for (int index = 0; index < stringLength; index++) {
            final char character = string.charAt(index);

            if (character >= 0x21 && character <= 0x7E) {
                return false;
            }
        }

        return true;
    }

    public static boolean equals(final char[] chars, final int length, final String string) {
        if (length == string.length()) {
            for (int i = 0; i < length; i++) {
                if (chars[i] != string.charAt(i)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static boolean equals(final char[] chars, final int offset, final int length,
                                 final String string) {
        if (length == string.length()) {
            for (int i = 0; i < length; i++) {
                if (chars[i + offset] != string.charAt(i)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static boolean isValidIdentifier(final String identifier) {
        if (identifier == null) {
            return false;
        }

        final int length = identifier.length();

        if (length == 0) {
            return false;
        }

        char character = identifier.charAt(0);

        if (!Character.isJavaIdentifierStart(character)) {
            return false;
        }

        for (int i = 1; i < length; i++) {
            character = identifier.charAt(i);

            if (!Character.isJavaIdentifierPart(character)) {
                return false;
            }
        }

        return true;
    }

    public static boolean isValidNamespace(final String string) {
        if (string == null) {
            return false;
        }

        int counter = 0;
        int beginIndex = 0;
        int endIndex = 0;

        final int length = string.length();

        while (endIndex >= 0 && endIndex < length) {
            endIndex = string.indexOf('.', beginIndex);

            if (endIndex == -1) {
                endIndex = length;
            } else if (endIndex + 1 == length) {
                return false;
            }

            final String identifier = string.substring(beginIndex, endIndex);

            if (identifier == null || !isValidIdentifier(identifier)) {
                return false;
            }

            beginIndex = endIndex + 1;
            counter++;
        }
        return (counter > 0);
    }

    public static String normalize(final String string) {
        final Form form = Form.NFC;
        return normalize(string, form);
    }

    public static String normalize(final String string, final Form form) {
        return Normalizer.normalize(string, form);
    }

    public static String encodeFileName(final String fileName) {
        return new String(fileName.getBytes(), CHARSET_UTF8);
    }

    public static String formatString(final String string, final List<?> args) {
        if (args != null) {
            return formatString(string, args.toArray());
        } else {
            return string;
        }
    }

    /*
     * Formats the given  {@link String} with given arguments and by using the
     * {@link String#format} method.
     *
     * @param string The string {@link String}.
     * @param args   The arguments for {@link String#format} method.
     * @return A formatted string {@link String}.
     */
    public static String formatString(final String string, final Object... args) {

        try {
            return String.format(string, args);
        } catch (final Exception exception) {
            String formattedString = string;

            if (args != null) {
                for (Object arg : args) {
                    formattedString += arg;
                    formattedString += ',';
                }
            }
            return formattedString;
        }
    }

    /*
     * Changes the first character of the given {@link String} to be a lowercase character.
     *
     * @param string The given {@link String}. It must contain at least one character.
     * @return The modified {@link String}.
     */
    public static String lowerCaseFirstCharacter(final String string) {
        final char firstChar = string.charAt(0);

        if (Character.isLowerCase(firstChar)) {
            return string;
        } else {
            final StringBuilder buffer = new StringBuilder(string);
            buffer.setCharAt(0, Character.toLowerCase(firstChar));
            return buffer.toString();
        }
    }

    /*
     * Parses the property key to create an array of {@link String}s containing individual
     * tokens of the key.
     *
     * @param propertyKey The specified property key {@link String}.
     * @return An array of {@link String}s containing the key tokens.
     */
    public static String[] parseKeyTokens(final String propertyKey) {
        final StringTokenizer tokenizer = new StringTokenizer(propertyKey, ".");
        final ArrayList<String> tokens = new ArrayList<String>();

        while (tokenizer.hasMoreElements()) {
            tokens.add(tokenizer.nextToken());
        }

        final String[] tokenStrings = new String[tokens.size()];
        tokens.toArray(tokenStrings);
        return tokenStrings;
    }

    /*
     * Parses the tokens separated by the specified separator from the given {@link String}. Parsed
     * tokens are stored into the given Vector.
     *
     * @param tokensString The given tokens {@link String}.
     * @param separator    The specified separator {@link String}.
     * @param tokens       A {@link List} used for storing the parsed tokens.
     */
    public static void parseKeyTokens(final String tokensString, final String separator,
                                      final List<String> tokens) {
        final StringTokenizer tokenizer = new StringTokenizer(tokensString, separator);

        while (tokenizer.hasMoreElements()) {
            tokens.add(tokenizer.nextToken());
        }
    }

    /*
     * Strips all the white spaces from the given {@link String}.
     *
     * @param string The given {@link String}.
     * @return A {@link String}.
     */
    public static String stripWhiteSpaces(final String string) {
        final StringBuilder buffer = new StringBuilder(string.length());
        final int length = string.length();

        for (int index = 0; index < length; index++) {
            final char character = string.charAt(index);

            if (character > 32) {
                buffer.append(character);
            }
        }

        return buffer.toString();
    }

    /*
     * Changes the first character of the given {@link String} to be a uppercase character.
     *
     * @param string The given {@link String}. It must contain at least one character.
     * @return The modified {@link String}.
     */
    public static String upperCaseFirstCharacter(final String string) {
        final char firstChar = string.charAt(0);

        if (Character.isUpperCase(firstChar)) {
            return string;
        } else {
            final StringBuilder buffer = new StringBuilder(string);
            buffer.setCharAt(0, Character.toUpperCase(firstChar));
            return buffer.toString();
        }
    }

    /*
     * Test if the given {@link CharSequence} is not {@code null}, but empty, or contains just
     * white space.
     * @param string A {@link CharSequence} to be tested.
     * @return A {@code boolean}.
     */
    public static boolean isBlank(final CharSequence string) {
        return (string != null && string.toString().trim().length() == 0);
    }

    /*
     * Return the given {@link String} as value if it is not empty as tested by
     * {@link StringToolkit#isNotEmpty(String)} or the given default value.
     * @param string A {@link String}
     * @param alternativeString A alternative value as a {@link String}.
     * @return The selected {@link String} value.
     */
    public static String valueOrDefault(final String string, final String alternativeString) {
        return isNotEmpty(string) ? string : alternativeString;
    }

    /*
     * Truncates the given {@link String} to specified length.
     * @param string A {@link String} to be truncated.
     * @param length The length of truncated {@link String}.
     * @return The truncated {@link String}.
     */
    public static String truncateAt(final String string, final int length) {
        if (string != null) {
            if (string.length() > length) {
                return string.substring(0, length);
            }
        }
        return string;
    }

    /*
     * Reverses the given {@link String}.
     * @param string A {@link String} to be reversed.
     * @return The reversed {@link String}.
     */
    public static String reverse(final String string) {
        return new StringBuilder(string).reverse().toString();
    }

    /*
     * Test if the given {@link String} is either {@code null}, empty, or contains just white space
     * @param string A {@link String} to be tested.
     * @return A {@code boolean}.
     */
    public static boolean isEmpty(final String string) {
        return (string == null || string.trim().isEmpty());
    }

    /*
     * Test if the given {@link String} is not {@code null}, empty, nor contains just white space
     * @param string A {@link String} to be tested.
     * @return A {@code boolean}.
     */
    public static boolean isNotEmpty(final String string) {
        return (string != null && !string.trim().isEmpty());
    }
}
