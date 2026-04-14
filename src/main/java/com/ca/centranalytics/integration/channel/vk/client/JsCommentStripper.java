package com.ca.centranalytics.integration.channel.vk.client;

import org.springframework.util.StringUtils;

/**
 * Утилитарный класс для удаления JavaScript-комментарлов из строк.
 * <p>
 * Поддерживает удаление как однострочных комментариев (//), так и блочных (/* * /).
 * Корректно обрабатывает строковые литералы в двойных и одинарных кавычках,
 * не удаляя символы, похожие на комментарии внутри строк.
 */
public final class JsCommentStripper {

    private JsCommentStripper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Удаляет все JavaScript-комментарлы из переданной строки.
     * <p>
     * Метод корректно обрабатывает:
     * <ul>
     *   <li>Однострочные комментарии (//)</li>
     *   <li>Блочные комментарии (/* * /)</li>
     *   <li>Строковые литералы в двойных и одинарных кавычках</li>
     *   <li>Экранированные символы внутри строк</li>
     * </ul>
     *
     * @param value входная строка с потенциальными комментариями
     * @return строка без комментариев, или null если входная строка была null/пустая
     */
    public static String stripJsComments(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        StringBuilder result = new StringBuilder(value.length());
        boolean inString = false;
        char stringDelimiter = 0;
        boolean escaped = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            char next = i + 1 < value.length() ? value.charAt(i + 1) : 0;
            if (inLineComment) {
                if (current == '\n' || current == '\r') {
                    inLineComment = false;
                    result.append(current);
                }
                continue;
            }
            if (inBlockComment) {
                if (current == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }
            if (inString) {
                result.append(current);
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == stringDelimiter) {
                    inString = false;
                    stringDelimiter = 0;
                }
                continue;
            }
            if (current == '/' && next == '/') {
                inLineComment = true;
                i++;
                continue;
            }
            if (current == '/' && next == '*') {
                inBlockComment = true;
                i++;
                continue;
            }
            if (current == '"' || current == '\'') {
                inString = true;
                stringDelimiter = current;
            }
            result.append(current);
        }
        return result.toString();
    }
}
