package com.ca.centranalytics.integration.channel.vk.client;

import org.springframework.util.StringUtils;

/**
 * Извлекает JSON-объекты из JavaScript-кода.
 * <p>
 * Этот класс парсит inline JavaScript-объекты, которые могут содержать:
 * <ul>
 *   <li>Однострочные и блочные комментарии</li>
 *   <li>Строки в одинарных кавычках (нуждаются в преобразовании к двойным)</li>
 *   <li>Незакрытые ключи объектов (bare keys)</li>
 *   <li>Трейлинг-запятые</li>
 *   <li>Literal 'undefined' вместо 'null'</li>
 * </ul>
 * <p>
 * Основная задача — найти первый JSON-объект в строке после знака '='
 * и вернуть его в валидном JSON-формате.
 */
public final class JsJsonExtractor {

    private JsJsonExtractor() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Извлекает первый JSON-объект из JavaScript-строки.
     * <p>
     * Метод ищет первое вхождение '{' после знака '=' (если есть)
     * и парсит сбалансированные скобки, учитывая:
     * <ul>
     *   <li>Строковые литералы в кавычках</li>
     *   <li>Экранированные символы</li>
     *   <li>JavaScript-комментарлы</li>
     * </ul>
     *
     * @param scriptData входная строка с JavaScript-кодом
     * @return извлеченный JSON-объект или null, если не найден
     */
    public static String extractInlineJsonObject(String scriptData) {
        int assignmentIndex = scriptData.indexOf('=');
        int searchFrom = assignmentIndex >= 0 ? assignmentIndex + 1 : 0;
        int objectStart = scriptData.indexOf('{', searchFrom);
        if (objectStart < 0) {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        char stringDelimiter = 0;
        boolean escaped = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        for (int i = objectStart; i < scriptData.length(); i++) {
            char current = scriptData.charAt(i);
            char next = i + 1 < scriptData.length() ? scriptData.charAt(i + 1) : 0;
            if (inLineComment) {
                if (current == '\n' || current == '\r') {
                    inLineComment = false;
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
                continue;
            }
            if (current == '{') {
                depth++;
                continue;
            }
            if (current != '}') {
                continue;
            }
            depth--;
            if (depth == 0) {
                return scriptData.substring(objectStart, i + 1).trim();
            }
        }
        return null;
    }

    /**
     * Нормализует JavaScript-подобный объект к валидному JSON.
     * <p>
     * Выполняет следующие преобразования:
     * <ul>
     *   <li>Удаляет комментарии</li>
     *   <li>Заменяет 'undefined' на 'null'</li>
     *   <li>Удаляет трейлинг-запятые</li>
     *   <li>Преобразует одинарные кавычки в двойные</li>
     *   <li>Оборачивает bare object keys в двойные кавычки</li>
     * </ul>
     *
     * @param payload входная строка с JavaScript-объектом
     * @return валидная JSON-строка или null
     */
    public static String normalizeJsObject(String payload) {
        if (!StringUtils.hasText(payload)) {
            return payload;
        }
        String normalized = JsCommentStripper.stripJsComments(payload);
        normalized = normalized.replace("undefined", "null");
        normalized = removeTrailingCommas(normalized);
        normalized = convertSingleQuotes(normalized);
        normalized = quoteBareKeys(normalized);
        return normalized;
    }

    private static String removeTrailingCommas(String value) {
        return value.replaceAll(",(?=\\s*[}\\]])", "");
    }

    private static String convertSingleQuotes(String value) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("'((?:\\\\.|[^'\\\\])*)'").matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String quoted = matcher.group(1)
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"");
            matcher.appendReplacement(buffer, java.util.regex.Matcher.quoteReplacement("\"" + quoted + "\""));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String quoteBareKeys(String value) {
        return java.util.regex.Pattern.compile("(^|[\\{,]\\s*)([A-Za-z_$][\\w$]*)(\\s*:)", java.util.regex.Pattern.MULTILINE)
                .matcher(value)
                .replaceAll("$1\"$2\"$3");
    }
}
