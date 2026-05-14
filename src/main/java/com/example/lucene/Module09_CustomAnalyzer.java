package com.example.lucene;

import com.example.lucene.util.Console;
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

import java.io.StringReader;
import java.util.List;

/**
 * MODULE 9 — Building a custom Analyzer.
 * <p>
 * An Analyzer is a small pipeline:
 * <pre>
 *   Tokenizer  →  TokenFilter  →  TokenFilter  →  ...  →  indexed tokens
 * </pre>
 * The Tokenizer splits the input into raw tokens. Each TokenFilter rewrites the stream:
 * lower-casing, removing stop words, stemming, adding synonyms, generating edge n-grams, etc.
 * <p>
 * Two custom analyzers are demonstrated below — one for "natural language with synonyms",
 * one for autocomplete-style prefix matching.
 */
public class Module09_CustomAnalyzer {

    public static void run() throws Exception {
        Console.header("Module 9 — Custom Analyzer");

        Console.section("Synonym + stemming analyzer");
        try (Analyzer synonymAnalyzer = buildSynonymAnalyzer()) {
            dumpTokens(synonymAnalyzer, "search engine for fast retrieval");
            dumpTokens(synonymAnalyzer, "I was running quickly to look up an article");
        }

        Console.section("Edge n-gram analyzer (good for autocomplete on the index side)");
        try (Analyzer edgeNgram = buildEdgeNGramAnalyzer()) {
            dumpTokens(edgeNgram, "Lucene");
            dumpTokens(edgeNgram, "Effective");
        }
    }

    /**
     * StandardTokenizer → LowerCaseFilter → ASCIIFoldingFilter → StopFilter →
     * SynonymGraphFilter → PorterStemFilter.
     * Reading order matters: lower-casing before stop-word removal is important because the
     * stop set is lower-case; stemming last so synonyms expand on the original surface form.
     */
    private static Analyzer buildSynonymAnalyzer() throws Exception {
        SynonymMap synonyms = buildSynonyms();
        CharArraySet stopWords = EnglishAnalyzer.ENGLISH_STOP_WORDS_SET;

        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer source = new StandardTokenizer();
                TokenStream stream = new LowerCaseFilter(source);
                stream = new ASCIIFoldingFilter(stream);           // "café" -> "cafe"
                stream = new StopFilter(stream, stopWords);        // drop "the", "a", "to", ...
                stream = new SynonymGraphFilter(stream, synonyms, true);
                stream = new PorterStemFilter(stream);             // "running" -> "run"
                return new TokenStreamComponents(source, stream);
            }
        };
    }

    /**
     * Build a hand-rolled synonym map. In a real system you'd usually load WordNet or a curated
     * file via {@code SolrSynonymParser} or {@code WordnetSynonymParser}.
     */
    private static SynonymMap buildSynonyms() throws Exception {
        SynonymMap.Builder builder = new SynonymMap.Builder(/* dedup */ true);
        addSynonyms(builder, "fast",     List.of("quick", "rapid", "speedy"));
        addSynonyms(builder, "retrieval", List.of("search", "lookup"));
        addSynonyms(builder, "article",  List.of("paper", "document"));
        return builder.build();
    }

    private static void addSynonyms(SynonymMap.Builder builder, String input, List<String> outputs) {
        CharsRefBuilder inputRef = new CharsRefBuilder();
        inputRef.append(input);
        for (String out : outputs) {
            CharsRefBuilder outRef = new CharsRefBuilder();
            outRef.append(out);
            // includeOrig=true so the original word is still indexed alongside its synonyms.
            builder.add(inputRef.get(), outRef.get(), /* includeOrig */ true);
        }
    }

    /**
     * StandardTokenizer → LowerCaseFilter → EdgeNGramTokenFilter(1..15).
     * Indexing "Lucene" stores prefixes "l", "lu", "luc", "luce", "lucen", "lucene" so that a
     * query for "luc" can be served by a simple term lookup. Pair with a non-ngram analyzer at
     * search time (otherwise the query itself would also be ngramed, which you don't want).
     */
    private static Analyzer buildEdgeNGramAnalyzer() {
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer source = new StandardTokenizer();
                TokenStream stream = new LowerCaseFilter(source);
                stream = new EdgeNGramTokenFilter(stream, 1, 15, /* preserveOriginal */ false);
                return new TokenStreamComponents(source, stream);
            }
        };
    }

    private static void dumpTokens(Analyzer analyzer, String text) throws Exception {
        System.out.print("  '" + text + "' -> [");
        try (TokenStream ts = analyzer.tokenStream("dummy", new StringReader(text))) {
            CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
            ts.reset();
            boolean first = true;
            while (ts.incrementToken()) {
                if (!first) System.out.print(", ");
                System.out.print(term);
                first = false;
            }
            ts.end();
        }
        System.out.println("]");
    }
}
