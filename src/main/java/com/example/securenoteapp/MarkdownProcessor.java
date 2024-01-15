package com.example.securenoteapp;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

public class MarkdownProcessor {
    private final Parser parser;
    private final HtmlRenderer renderer;
    private final PolicyFactory sanitizer;

    public MarkdownProcessor() {
        MutableDataSet options = new MutableDataSet();
        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();

        sanitizer = new HtmlPolicyBuilder()
                .allowElements("b", "i", "h1", "h2", "h3", "h4", "h5", "img", "a")
                .allowAttributes("src").onElements("img")
                .allowAttributes("href").onElements("a")
                .allowStandardUrlProtocols()
                .toFactory();
    }

    public String parseMarkdown(String markdown) {
        String html = renderer.render(parser.parse(markdown));
        return sanitizer.sanitize(html);
    }
}
