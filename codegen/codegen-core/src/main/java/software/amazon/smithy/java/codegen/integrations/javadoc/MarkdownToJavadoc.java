/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.integrations.javadoc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Tag;

/**
 * Converts Markdown or HTML text (from Smithy {@code @documentation} traits) into
 * Javadoc-compatible HTML with proper formatting.
 *
 * <p>The conversion pipeline is:
 * <ol>
 *   <li>Escape Java generics ({@code List<String>}) so they are not parsed as HTML tags</li>
 *   <li>Parse Markdown to HTML via commonmark (already-HTML input passes through unchanged)</li>
 *   <li>Parse the HTML into a jsoup DOM for structural manipulation</li>
 *   <li>Clean up the DOM (unwrap invalid nesting, remove empty elements)</li>
 *   <li>Render the DOM to a Javadoc-formatted string with indentation and line wrapping</li>
 * </ol>
 */
final class MarkdownToJavadoc {

    static final int DEFAULT_MAX_WIDTH = 110;
    private static final int INDENT_WIDTH = 2;
    private static final int MIN_WRAP_WIDTH = 40;

    private static final Parser MD_PARSER = Parser.builder().build();
    private static final HtmlRenderer HTML_RENDERER = HtmlRenderer.builder().build();

    // Commonmark treats <Foo> as an HTML tag. Since Java generics like List<String> use
    // uppercase type names, we escape < before uppercase letters so they survive as &lt;
    private static final Pattern JAVA_GENERIC = Pattern.compile("<([A-Z])");

    // Fast-path pattern: matches commonmark output that is a single <p>...</p>\n with only inline content.
    // This avoids jsoup parsing for the most common case (simple single-paragraph documentation).
    private static final Pattern SIMPLE_PARAGRAPH = Pattern.compile(
            "^<p>([^<]*(?:<(?:code|em|strong|a\\b)[^>]*>[^<]*</(?:code|em|strong|a)>[^<]*)*)</p>\n$");

    // Cached indent strings to avoid String.repeat() allocations
    private static final String[] INDENTS = new String[16];

    static {
        INDENTS[0] = "";
        for (int i = 1; i < INDENTS.length; i++) {
            INDENTS[i] = " ".repeat(i);
        }
    }

    private static final Set<String> BLOCK_TAGS = Set.of(
            "p",
            "ul",
            "ol",
            "li",
            "blockquote",
            "div",
            "table",
            "tr",
            "th",
            "td",
            "thead",
            "tbody",
            "h1",
            "h2",
            "h3",
            "h4",
            "h5",
            "h6",
            "pre",
            "hr",
            "dl",
            "dt",
            "dd");

    private MarkdownToJavadoc() {}

    /**
     * Converts a Markdown or HTML string to Javadoc-compatible HTML.
     *
     * @param input the documentation trait value (Markdown or HTML)
     * @param maxWidth maximum line width for wrapping (before the " * " prefix)
     * @return formatted Javadoc content, or the input unchanged if null/empty
     */
    static String convert(String input, int maxWidth) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        input = JAVA_GENERIC.matcher(input).replaceAll("&lt;$1");
        String html = HTML_RENDERER.render(MD_PARSER.parse(input));

        // Fast path: single paragraph with only inline content — skip jsoup entirely
        var simpleMatcher = SIMPLE_PARAGRAPH.matcher(html);
        if (simpleMatcher.matches()) {
            String content = simpleMatcher.group(1);
            return renderSimpleParagraph(content, maxWidth);
        }

        Document doc = Jsoup.parseBodyFragment(html);
        cleanDom(doc.body());

        var sb = new StringBuilder(html.length());
        int[] col = {0};
        renderChildren(doc.body(), sb, col, maxWidth, 0, true, false);
        return sb.toString().strip();
    }

    static String convert(String input) {
        return convert(input, DEFAULT_MAX_WIDTH);
    }

    /**
     * Fast path for simple single-paragraph content. Handles @ escaping, HTML entity encoding,
     * whitespace collapsing, and line wrapping without building a DOM tree.
     */
    private static String renderSimpleParagraph(String content, int maxWidth) {
        var sb = new StringBuilder(content.length());
        boolean lastWasSpace = false;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '<') {
                int end = content.indexOf('>', i);
                if (end >= 0) {
                    sb.append(content, i, end + 1);
                    i = end;
                    lastWasSpace = false;
                } else {
                    sb.append("&lt;");
                    lastWasSpace = false;
                }
            } else if (c == '&') {
                int semi = content.indexOf(';', i);
                if (semi >= 0 && semi - i < 8) {
                    sb.append(content, i, semi + 1);
                    i = semi;
                } else {
                    sb.append("&amp;");
                }
                lastWasSpace = false;
            } else if (c == '@') {
                sb.append("{@literal @}");
                lastWasSpace = false;
            } else if (Character.isWhitespace(c)) {
                if (!lastWasSpace) {
                    sb.append(' ');
                    lastWasSpace = true;
                }
            } else {
                sb.append(c);
                lastWasSpace = false;
            }
        }
        String text = sb.toString().strip();

        if (text.length() <= maxWidth) {
            return text;
        }
        var result = new StringBuilder(text.length() + 16);
        int[] col = {0};
        appendWrapped(result, col, text, maxWidth, 0);
        return result.toString().strip();
    }

    /**
     * Cleans the DOM without using CSS selectors. Iterates elements directly.
     */
    private static void cleanDom(Element root) {
        // Strip non-HTML tags (e.g., <note>, <important> from AWS models).
        for (Element el : root.getAllElements()) {
            if (!el.tagName().equals("#root") && !Tag.isKnownTag(el.tagName())) {
                el.unwrap();
            }
        }
        // AWS models produce <li><p>text</p></li>; unwrap the <p> for cleaner output.
        // Also remove empty paragraphs. Single pass over all elements instead of two selector queries.
        List<Element> toUnwrap = null;
        List<Element> toRemove = null;
        for (Element el : root.getAllElements()) {
            if (!el.tagName().equals("p")) {
                continue;
            }
            Element parent = el.parent();
            if (parent != null && parent.tagName().equals("li")) {
                if (toUnwrap == null) {
                    toUnwrap = new ArrayList<>();
                }
                toUnwrap.add(el);
            } else if (el.text().isBlank() && el.children().isEmpty()) {
                if (toRemove == null) {
                    toRemove = new ArrayList<>();
                }
                toRemove.add(el);
            }
        }
        if (toUnwrap != null) {
            for (Element el : toUnwrap) {
                el.unwrap();
            }
        }
        if (toRemove != null) {
            for (Element el : toRemove) {
                el.remove();
            }
        }
    }

    /**
     * @param col single-element array tracking current column position (avoids scanning StringBuilder)
     * @param insidePre whether we are inside a pre element (avoids parent-chain walks)
     */
    private static void renderChildren(
            Element parent,
            StringBuilder sb,
            int[] col,
            int maxWidth,
            int indent,
            boolean isFirstBlock,
            boolean insidePre
    ) {
        boolean first = isFirstBlock;
        for (Node child : parent.childNodes()) {
            first = renderNode(child, sb, col, maxWidth, indent, first, insidePre);
        }
    }

    private static boolean renderNode(
            Node node,
            StringBuilder sb,
            int[] col,
            int maxWidth,
            int indent,
            boolean isFirstBlock,
            boolean insidePre
    ) {
        if (node instanceof TextNode text) {
            String content = text.getWholeText();
            if (!insidePre) {
                content = collapseAndEncode(content);
                if (content.isBlank()) {
                    return isFirstBlock;
                }
            }
            appendWrapped(sb, col, content, maxWidth, indent);
            return false;
        }

        if (node instanceof Element el) {
            renderElement(el, sb, col, maxWidth, indent, isFirstBlock, insidePre);
            return false;
        }

        return isFirstBlock;
    }

    private static void renderElement(
            Element el,
            StringBuilder sb,
            int[] col,
            int maxWidth,
            int indent,
            boolean isFirstBlock,
            boolean insidePre
    ) {
        String tag = el.tagName();

        if (tag.equals("pre")) {
            ensureNewline(sb, col);
            if (!isFirstBlock) {
                appendIndent(sb, col, indent);
                sb.append("\n");
                col[0] = 0;
            }
            appendIndent(sb, col, indent);
            // Commonmark wraps code blocks in <pre><code>; find <code> child directly
            Element codeChild = firstChildByTag(el, "code");
            if (codeChild != null) {
                sb.append("<pre>{@code\n");
                col[0] = 0;
                sb.append(codeChild.wholeText()).append("\n");
                col[0] = 0;
                appendIndent(sb, col, indent);
                sb.append("}</pre>");
                col[0] += 6;
            } else {
                String pre = "<pre>" + el.wholeText() + "</pre>";
                sb.append(pre);
                int nl = pre.lastIndexOf('\n');
                col[0] = nl < 0 ? col[0] + pre.length() : pre.length() - nl - 1;
            }
            return;
        }

        if (tag.equals("hr")) {
            ensureNewline(sb, col);
            appendIndent(sb, col, indent);
            sb.append("<hr>");
            col[0] += 4;
            return;
        }

        if (tag.equals("br")) {
            sb.append("<br>");
            col[0] += 4;
            return;
        }

        if (tag.equals("img")) {
            String html = el.outerHtml();
            sb.append(html);
            int nl = html.lastIndexOf('\n');
            col[0] = nl < 0 ? col[0] + html.length() : html.length() - nl - 1;
            return;
        }

        if (tag.equals("p") && isFirstBlock) {
            renderChildren(el, sb, col, maxWidth, indent, false, false);
            return;
        }

        if (tag.equals("p")) {
            ensureNewline(sb, col);
            sb.append("\n");
            col[0] = 0;
            appendIndent(sb, col, indent);
            sb.append("<p>");
            col[0] += 3;
            renderChildren(el, sb, col, maxWidth, indent, false, false);
            return;
        }

        if (BLOCK_TAGS.contains(tag)) {
            ensureNewline(sb, col);
            if (isTopLevelBlock(el)) {
                sb.append("\n");
                col[0] = 0;
            }
            appendIndent(sb, col, indent);
            sb.append("<").append(tag);
            appendAttributes(sb, el);
            sb.append(">\n");
            col[0] = 0;
            renderChildren(el, sb, col, maxWidth, indent + INDENT_WIDTH, false, insidePre);
            ensureNewline(sb, col);
            appendIndent(sb, col, indent);
            sb.append("</").append(tag).append(">");
            col[0] += tag.length() + 3;
            return;
        }

        // Inline tags
        if (col[0] == 0 && indent > 0) {
            appendIndent(sb, col, indent);
        }
        sb.append("<").append(tag);
        appendAttributes(sb, el);
        sb.append(">");
        col[0] += tag.length() + 2;
        renderChildren(el, sb, col, maxWidth, indent, false, insidePre);
        sb.append("</").append(tag).append(">");
        col[0] += tag.length() + 3;
    }

    /**
     * Appends text with word wrapping, tracking column position via col[0].
     */
    private static void appendWrapped(StringBuilder sb, int[] col, String text, int maxWidth, int indent) {
        int effectiveMax = Math.max(maxWidth - indent, MIN_WRAP_WIDTH);
        if (col[0] == 0 && indent > 0) {
            appendIndent(sb, col, indent);
        }
        int i = 0;
        while (i < text.length()) {
            // Javadoc inline tags (e.g., {@literal @}) must not be split
            if (text.charAt(i) == '{' && i + 1 < text.length() && text.charAt(i + 1) == '@') {
                int braceEnd = findMatchingBrace(text, i);
                int tokenLen = braceEnd - i;
                if (col[0] + tokenLen > effectiveMax && col[0] > indent) {
                    wrapLine(sb, col);
                    appendIndent(sb, col, indent);
                }
                sb.append(text, i, braceEnd);
                col[0] += tokenLen;
                i = braceEnd;
                continue;
            }

            // Find next word (including any leading whitespace)
            int wordStart = i;
            int wordEnd = i;
            while (wordEnd < text.length() && text.charAt(wordEnd) == ' ') {
                wordEnd++;
            }
            while (wordEnd < text.length() && text.charAt(wordEnd) != ' '
                    && !(text.charAt(wordEnd) == '{' && wordEnd + 1 < text.length()
                            && text.charAt(wordEnd + 1) == '@')) {
                wordEnd++;
            }
            if (wordEnd == wordStart) {
                break;
            }
            int wordLen = wordEnd - wordStart;
            if (col[0] + wordLen > effectiveMax && col[0] > indent) {
                wrapLine(sb, col);
                appendIndent(sb, col, indent);
                // Skip leading spaces after wrap
                while (wordStart < wordEnd && text.charAt(wordStart) == ' ') {
                    wordStart++;
                }
                wordLen = wordEnd - wordStart;
            }
            sb.append(text, wordStart, wordEnd);
            col[0] += wordLen;
            i = wordEnd;
        }
    }

    /**
     * Collapses whitespace and encodes HTML special characters in a single pass.
     * Also escapes @ signs for Javadoc safety.
     */
    private static String collapseAndEncode(String text) {
        var sb = new StringBuilder(text.length());
        boolean lastWasSpace = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                if (!lastWasSpace) {
                    sb.append(' ');
                    lastWasSpace = true;
                }
            } else if (c == '{' && i + 1 < text.length() && text.charAt(i + 1) == '@') {
                int end = findMatchingBrace(text, i);
                sb.append(text, i, end);
                i = end - 1;
                lastWasSpace = false;
            } else if (c == '&') {
                sb.append("&amp;");
                lastWasSpace = false;
            } else if (c == '<') {
                sb.append("&lt;");
                lastWasSpace = false;
            } else if (c == '>') {
                sb.append("&gt;");
                lastWasSpace = false;
            } else if (c == '@') {
                sb.append("{@literal @}");
                lastWasSpace = false;
            } else {
                sb.append(c);
                lastWasSpace = false;
            }
        }
        return sb.toString();
    }

    /** Finds the first direct child element with the given tag name, or null. */
    private static Element firstChildByTag(Element parent, String tagName) {
        for (Node child : parent.childNodes()) {
            if (child instanceof Element el && el.tagName().equals(tagName)) {
                return el;
            }
        }
        return null;
    }

    private static boolean isTopLevelBlock(Element el) {
        var parent = el.parent();
        if (parent == null) {
            return false;
        }
        return switch (parent.tagName()) {
            case "ul", "ol", "dl", "table", "thead", "tbody", "tr" -> false;
            default -> true;
        };
    }

    /** Appends element attributes directly to sb (avoids intermediate String allocation). */
    private static void appendAttributes(StringBuilder sb, Element el) {
        if (el.attributes().isEmpty()) {
            return;
        }
        el.attributes()
                .forEach(attr -> sb.append(" ")
                        .append(attr.getKey())
                        .append("=\"")
                        .append(attr.getValue())
                        .append("\""));
    }

    private static void appendIndent(StringBuilder sb, int[] col, int indent) {
        if (indent < INDENTS.length) {
            sb.append(INDENTS[indent]);
        } else {
            sb.append(" ".repeat(indent));
        }
        col[0] += indent;
    }

    private static void ensureNewline(StringBuilder sb, int[] col) {
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) != '\n') {
            sb.append("\n");
            col[0] = 0;
        }
    }

    private static void wrapLine(StringBuilder sb, int[] col) {
        while (!sb.isEmpty() && sb.charAt(sb.length() - 1) == ' ') {
            sb.setLength(sb.length() - 1);
        }
        sb.append("\n");
        col[0] = 0;
    }

    private static int findMatchingBrace(String s, int start) {
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) == '{') {
                depth++;
            } else if (s.charAt(i) == '}') {
                depth--;
                if (depth == 0) {
                    return i + 1;
                }
            }
        }
        return s.length();
    }
}
