package com.example.lucene;

import com.example.lucene.util.Console;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.util.BytesRef;

import java.io.StringReader;

/**
 * MODULE 2 — Field types and Analyzers.
 * <p>
 * Two ideas this module hammers home:
 * <ul>
 *   <li><b>Field types decide what kinds of queries are possible.</b> A field that is not indexed
 *       can not be searched. A field that is not stored can not be returned to the caller.</li>
 *   <li><b>Analyzers decide what "matches".</b> The same word can index as different tokens
 *       under different analyzers, which changes what queries hit.</li>
 * </ul>
 * <p>
 * This module does not run any queries — it focuses purely on demystifying these two layers
 * by printing the tokens produced by several analyzers.
 */
public class Module02_FieldsAndAnalyzers {

    public static void run() throws Exception {
        Console.header("Module 2 — Field types and Analyzers");

        Console.section("Field type cheat-sheet");
        printFieldCheatSheet();

        Console.section("Analyzer tokenization comparison");
        String sample = "The Quick-Brown Foxes are JUMPING over the lazy dog's email: foo@bar.com (2023).";
        compareAnalyzers(sample);

        Console.section("English analyzer applies stemming");
        // EnglishAnalyzer adds a Porter-style stemmer plus English stop-word removal:
        // "fishing", "fished", "fisher" all reduce to the same stem and therefore match each other.
        printTokens("EnglishAnalyzer", new EnglishAnalyzer(), "fishing fished fisher fishes");
    }

    private static void printFieldCheatSheet() {
        System.out.println("""
            StringField      indexed *as-is* (no tokenization), great for IDs/codes/enums.
            TextField        indexed via the Analyzer, the default choice for prose.
            StoredField      not indexed, only stored so it can be retrieved with the hit.
            IntPoint /
            LongPoint /
            DoublePoint      numeric fields, enable efficient range queries.
            SortedDocValuesField   columnar storage used for sorting, faceting, grouping.
            KeywordField     (Lucene 9.11+) string field that is also stored as doc-values.
            """);

        // Hint at how you'd combine multiple field types for one logical "field":
        Document example = new Document();
        example.add(new StringField("category", "Programming", Field.Store.YES));            // exact match
        example.add(new SortedDocValuesField("category", new BytesRef("Programming")));      // for sort/facet
        example.add(new IntPoint("year", 2018));                                              // for range query
        example.add(new StoredField("year_stored", 2018));                                    // for retrieval
        example.add(new DoublePoint("rating", 4.8));
        example.add(new TextField("description",
            "Best practices for Java", Field.Store.YES));                                     // tokenized search
        System.out.println("Built an example document with " + example.getFields().size() + " fields.");
    }

    private static void compareAnalyzers(String text) throws Exception {
        // Each analyzer applies a different chain of tokenizer + filters. Reading the
        // output side-by-side is the fastest way to build an intuition for what an analyzer
        // is actually doing under the hood.
        try (Analyzer whitespace = new WhitespaceAnalyzer();
             Analyzer keyword    = new KeywordAnalyzer();
             Analyzer standard   = new StandardAnalyzer();
             Analyzer english    = new EnglishAnalyzer()) {

            printTokens("WhitespaceAnalyzer", whitespace, text);
            printTokens("KeywordAnalyzer   ", keyword,    text);
            printTokens("StandardAnalyzer  ", standard,   text);
            printTokens("EnglishAnalyzer   ", english,    text);
        }
    }

    /** Push the text through the analyzer and dump the resulting token strings. */
    private static void printTokens(String label, Analyzer analyzer, String text) throws Exception {
        System.out.print(label + " -> [");
        try (TokenStream ts = analyzer.tokenStream("dummy", new StringReader(text))) {
            CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
            ts.reset();
            boolean first = true;
            while (ts.incrementToken()) {
                if (!first) System.out.print(", ");
                System.out.print(term.toString());
                first = false;
            }
            ts.end();
        }
        System.out.println("]");
    }
}
