package com.example.lucene;

import com.example.lucene.util.Book;
import com.example.lucene.util.Console;
import com.example.lucene.util.SampleData;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.suggest.InputIterator;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * MODULE 10 — Autocomplete with a {@link Lookup} suggester.
 * <p>
 * "Suggesters" are a separate sub-index optimised for the autocomplete case: type a prefix,
 * get a few completions sorted by a weight (e.g. popularity). They are far faster than
 * running a wildcard query against the main index.
 * <p>
 * {@link AnalyzingInfixSuggester} matches the prefix anywhere within the suggestion (not just
 * at the start), which is what users expect from "infix" autocomplete: typing "java" surfaces
 * both "Java Concurrency in Practice" and "Effective Java".
 */
public class Module10_Suggester {

    public static void run() throws Exception {
        Console.header("Module 10 — Suggester / Autocomplete");

        try (Directory dir = new ByteBuffersDirectory();
             Analyzer analyzer = new StandardAnalyzer();
             AnalyzingInfixSuggester suggester = new AnalyzingInfixSuggester(dir, analyzer)) {

            // Each suggestion = (text, weight, payload).
            // Weight is what makes "effective java" rank above an obscure title when the user
            // types "java". Here we simply (rating * 10) so a 4.8 book outranks a 4.1 book.
            suggester.build(new BookInputIterator(SampleData.books()));

            for (String prefix : List.of("ja", "data", "search", "luc", "concur")) {
                Console.section("Suggestions for '" + prefix + "'");
                List<Lookup.LookupResult> results =
                    suggester.lookup(prefix, /* contexts */ null, /* onlyMorePopular */ false,
                                     /* num */ 5);
                for (Lookup.LookupResult r : results) {
                    System.out.printf("  weight=%-5d  %s%n", r.value, r.key);
                }
            }
        }
    }

    /**
     * Feeds books into the suggester. Lucene's {@link InputIterator} is a single-pass cursor;
     * implement only what you need (we leave payloads and contexts null).
     */
    private static class BookInputIterator implements InputIterator {
        private final Iterator<Book> it;
        private Book current;

        BookInputIterator(List<Book> books) { this.it = books.iterator(); }

        @Override public BytesRef next() {
            if (!it.hasNext()) return null;
            current = it.next();
            return new BytesRef(current.title);
        }
        @Override public long weight()     { return Math.round(current.rating * 10); }
        @Override public BytesRef payload(){ return null; }
        @Override public boolean hasPayloads() { return false; }
        @Override public Set<BytesRef> contexts() { return null; }
        @Override public boolean hasContexts() { return false; }
    }
}
