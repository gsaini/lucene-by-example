package com.example.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.CharsRefBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Module 9: Custom analyzer pipelines emit the expected tokens")
class Module09_CustomAnalyzerIT {

    @Test
    @DisplayName("Synonym filter expands 'fast' to its configured synonyms")
    void synonym_expansion_includes_original_and_synonyms() throws Exception {
        try (Analyzer a = buildSynonymAnalyzer()) {
            List<String> tokens = tokensOf(a, "fast retrieval");

            // includeOrig=true → the original token is also emitted alongside the synonyms.
            assertThat(tokens).contains("fast", "quick", "rapid", "speedy");
            assertThat(tokens).contains("retrieval", "search", "lookup");
        }
    }

    @Test
    @DisplayName("PorterStemFilter normalises related word forms to a shared stem")
    void stemming_normalises_word_forms() throws Exception {
        try (Analyzer a = buildSynonymAnalyzer()) {
            // Classic Porter-stemmer example: every form here reduces to "fish".
            List<String> tokens = tokensOf(a, "fishing fished fishes");
            assertThat(tokens.stream().distinct().toList()).containsExactly("fish");
        }
    }

    @Test
    @DisplayName("ASCII folding strips accents")
    void ascii_folding_strips_accents() throws Exception {
        try (Analyzer a = buildSynonymAnalyzer()) {
            assertThat(tokensOf(a, "café")).contains("cafe");
        }
    }

    @Test
    @DisplayName("Edge n-gram filter emits all prefixes of a token")
    void edge_ngram_emits_prefixes() throws Exception {
        try (Analyzer a = buildEdgeNGramAnalyzer()) {
            assertThat(tokensOf(a, "Lucene"))
                .containsExactly("l", "lu", "luc", "luce", "lucen", "lucene");
        }
    }

    private Analyzer buildSynonymAnalyzer() throws Exception {
        SynonymMap synonyms = buildSynonyms();
        CharArraySet stopWords = EnglishAnalyzer.ENGLISH_STOP_WORDS_SET;
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer source = new StandardTokenizer();
                TokenStream stream = new LowerCaseFilter(source);
                stream = new ASCIIFoldingFilter(stream);
                stream = new StopFilter(stream, stopWords);
                stream = new SynonymGraphFilter(stream, synonyms, true);
                stream = new PorterStemFilter(stream);
                return new TokenStreamComponents(source, stream);
            }
        };
    }

    private Analyzer buildEdgeNGramAnalyzer() {
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer source = new StandardTokenizer();
                TokenStream stream = new LowerCaseFilter(source);
                stream = new EdgeNGramTokenFilter(stream, 1, 15, false);
                return new TokenStreamComponents(source, stream);
            }
        };
    }

    private SynonymMap buildSynonyms() throws Exception {
        SynonymMap.Builder b = new SynonymMap.Builder(true);
        addSyn(b, "fast",      List.of("quick", "rapid", "speedy"));
        addSyn(b, "retrieval", List.of("search", "lookup"));
        return b.build();
    }

    private void addSyn(SynonymMap.Builder b, String in, List<String> outs) {
        CharsRefBuilder input = new CharsRefBuilder();
        input.append(in);
        for (String out : outs) {
            CharsRefBuilder o = new CharsRefBuilder();
            o.append(out);
            b.add(input.get(), o.get(), true);
        }
    }

    private static List<String> tokensOf(Analyzer analyzer, String text) throws Exception {
        List<String> tokens = new ArrayList<>();
        try (TokenStream ts = analyzer.tokenStream("dummy", new StringReader(text))) {
            CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
            ts.reset();
            while (ts.incrementToken()) tokens.add(term.toString());
            ts.end();
        }
        return tokens;
    }
}
