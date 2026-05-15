package com.example.lucene;

import com.example.lucene.util.Book;
import com.example.lucene.util.SampleData;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.suggest.InputIterator;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Module 10: AnalyzingInfixSuggester returns prefix-matched suggestions in weight order")
class Module10_SuggesterIT {

    private Directory dir;
    private StandardAnalyzer analyzer;
    private AnalyzingInfixSuggester suggester;

    @BeforeEach
    void setUp() throws Exception {
        dir = new ByteBuffersDirectory();
        analyzer = new StandardAnalyzer();
        suggester = new AnalyzingInfixSuggester(dir, analyzer);
        suggester.build(new BookInputIterator(SampleData.books()));
    }

    @AfterEach
    void tearDown() throws Exception {
        suggester.close();
        analyzer.close();
        dir.close();
    }

    @Test
    @DisplayName("Prefix 'ja' surfaces both Java-titled books (infix match anywhere in title)")
    void prefix_ja_finds_java_books() throws Exception {
        List<Lookup.LookupResult> hits =
            suggester.lookup("ja", /* contexts */ null, /* onlyMorePopular */ false, /* num */ 5);

        // Effective Java (#2) and Java Concurrency in Practice (#3). The infix suggester is
        // case-insensitive after analysis, so "ja" finds "Java".
        List<String> titles = hits.stream().map(r -> r.key.toString()).toList();
        assertThat(titles)
            .anyMatch(t -> t.toLowerCase().contains("effective java"))
            .anyMatch(t -> t.toLowerCase().contains("java concurrency"));
    }

    @Test
    @DisplayName("Suggestions come back ordered by weight (higher first)")
    void weights_are_descending() throws Exception {
        List<Lookup.LookupResult> hits = suggester.lookup("search", null, false, 5);
        assertThat(hits).isNotEmpty();
        for (int i = 1; i < hits.size(); i++) {
            assertThat(hits.get(i).value)
                .as("suggestion %d weight should be <= previous", i)
                .isLessThanOrEqualTo(hits.get(i - 1).value);
        }
    }

    @Test
    @DisplayName("num parameter caps the result count")
    void num_caps_results() throws Exception {
        List<Lookup.LookupResult> hits = suggester.lookup("s", null, false, 2);
        assertThat(hits).hasSizeLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Non-matching prefix returns an empty list")
    void unknown_prefix_returns_empty() throws Exception {
        List<Lookup.LookupResult> hits = suggester.lookup("xyzzy", null, false, 5);
        assertThat(hits).isEmpty();
    }

    /** Feeds book titles into the suggester. Weight = rating * 10. */
    private static class BookInputIterator implements InputIterator {
        private final Iterator<Book> it;
        private Book current;

        BookInputIterator(List<Book> books) { this.it = books.iterator(); }

        @Override public BytesRef next() {
            if (!it.hasNext()) return null;
            current = it.next();
            return new BytesRef(current.title);
        }
        @Override public long weight()         { return Math.round(current.rating * 10); }
        @Override public BytesRef payload()    { return null; }
        @Override public boolean hasPayloads() { return false; }
        @Override public Set<BytesRef> contexts()    { return null; }
        @Override public boolean hasContexts()       { return false; }
    }
}
