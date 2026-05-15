package com.example.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Module 2: token-level behaviour of the standard analyzers.
 * Running text through each analyzer and asserting the resulting token list is the
 * easiest way to lock in expected behaviour and catch surprise upgrades.
 */
@DisplayName("Module 2: Analyzers tokenize as documented")
class Module02_AnalyzersIT {

    @Test
    @DisplayName("StandardAnalyzer lower-cases, splits on Unicode word breaks, drops punctuation")
    void standard_analyzer() throws Exception {
        try (Analyzer a = new StandardAnalyzer()) {
            List<String> tokens = tokensOf(a, "The Quick-Brown Foxes JUMP! 42.");
            assertThat(tokens).containsExactly("quick", "brown", "foxes", "jump", "42");
        }
    }

    @Test
    @DisplayName("EnglishAnalyzer applies Porter stemming so fish-variants share a stem")
    void english_analyzer_stems() throws Exception {
        try (Analyzer a = new EnglishAnalyzer()) {
            List<String> tokens = tokensOf(a, "fishing fished fisher fishes");
            // All four reduce to the same stem ("fish") so the resulting token *set* is size 1.
            assertThat(tokens).hasSize(4);
            assertThat(tokens.stream().distinct().toList()).containsExactly("fish");
        }
    }

    @Test
    @DisplayName("KeywordAnalyzer emits the whole input as a single token (no splitting)")
    void keyword_analyzer_does_not_split() throws Exception {
        try (Analyzer a = new KeywordAnalyzer()) {
            assertThat(tokensOf(a, "Hello, World!"))
                .containsExactly("Hello, World!");
        }
    }

    @Test
    @DisplayName("WhitespaceAnalyzer splits on whitespace and preserves case + punctuation")
    void whitespace_analyzer() throws Exception {
        try (Analyzer a = new WhitespaceAnalyzer()) {
            assertThat(tokensOf(a, "Hello, World! foo@bar.com"))
                .containsExactly("Hello,", "World!", "foo@bar.com");
        }
    }

    private static List<String> tokensOf(Analyzer analyzer, String text) throws Exception {
        List<String> tokens = new ArrayList<>();
        try (TokenStream ts = analyzer.tokenStream("dummy", new StringReader(text))) {
            CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
            ts.reset();
            while (ts.incrementToken()) {
                tokens.add(term.toString());
            }
            ts.end();
        }
        return tokens;
    }
}
