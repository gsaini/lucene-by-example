package com.example.lucene;

import com.example.lucene.support.BookIndex;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Module 3: Each Query type matches the documents we expect")
class Module03_QueryTypesIT {

    private BookIndex index;

    @BeforeEach void setUp() throws Exception { index = BookIndex.withSampleBooks(); }
    @AfterEach void tearDown() throws Exception { index.close(); }

    @Test
    @DisplayName("PhraseQuery with slop matches 'data … systems'")
    void phrase_with_slop() throws Exception {
        PhraseQuery q = new PhraseQuery.Builder()
            .add(new Term("description", "data"))
            .add(new Term("description", "systems"))
            .setSlop(3)
            .build();

        // "The big ideas behind reliable, scalable, and maintainable data systems."
        // matches: tokens "data" and "systems" are adjacent (slop 0 would also work).
        assertThat(ids(index.searcher().search(q, 10)))
            .containsExactly("5"); // Designing Data-Intensive Applications
    }

    @Test
    @DisplayName("BooleanQuery: MUST + FILTER intersects matches with the filter")
    void boolean_must_plus_filter() throws Exception {
        BooleanQuery q = new BooleanQuery.Builder()
            .add(new TermQuery(new Term("description", "search")), BooleanClause.Occur.MUST)
            .add(new TermQuery(new Term("category", "Search")),    BooleanClause.Occur.FILTER)
            .build();

        // Books in the "Search" category whose description contains "search":
        //   #7 "Search Patterns"             — "...delightful search user experiences."
        //   #8 "Introduction to Info Retr."  — "...evaluation of search systems."
        // #9 is also in "Search" category but its description only contains "elasticsearch"
        // (a single token after analysis), not the term "search" — so it should NOT match.
        assertThat(ids(index.searcher().search(q, 10)))
            .containsExactlyInAnyOrder("7", "8");
    }

    @Test
    @DisplayName("WildcardQuery matches author names by prefix pattern")
    void wildcard_query() throws Exception {
        WildcardQuery q = new WildcardQuery(new Term("author", "mart*"));
        // "Robert C. Martin" (#4) and "Martin Kleppmann" (#5) both tokenize to include "martin".
        assertThat(ids(index.searcher().search(q, 10)))
            .containsExactlyInAnyOrder("4", "5");
    }

    @Test
    @DisplayName("PrefixQuery is shorthand for term*")
    void prefix_query() throws Exception {
        // Description of #3 has "multithreaded"; no other description has a token starting "multi".
        PrefixQuery q = new PrefixQuery(new Term("description", "multi"));
        assertThat(ids(index.searcher().search(q, 10))).containsExactly("3");
    }

    @Test
    @DisplayName("FuzzyQuery tolerates typos within the configured edit distance")
    void fuzzy_query() throws Exception {
        // "lucen" is one edit away from "lucene" (insert 'e') → matches "Lucene in Action" only.
        FuzzyQuery q = new FuzzyQuery(new Term("title", "lucen"), /* maxEdits */ 1);
        assertThat(ids(index.searcher().search(q, 10))).containsExactly("1");
    }

    @Test
    @DisplayName("IntPoint range query selects documents in [low, high]")
    void int_range() throws Exception {
        TopDocs hits = index.searcher().search(IntPoint.newRangeQuery("year", 2005, 2015), 50);
        // Years 2006, 2008, 2008, 2010, 2010 in sample → 5 hits.
        assertThat(hits.totalHits.value()).isEqualTo(5);
    }

    @Test
    @DisplayName("DoublePoint range query supports POSITIVE_INFINITY upper bound")
    void double_range() throws Exception {
        TopDocs hits = index.searcher().search(
            DoublePoint.newRangeQuery("rating", 4.6, Double.POSITIVE_INFINITY), 50);
        // Ratings >= 4.6: 4.6, 4.8, 4.7, 4.9, 4.6 → 5 hits.
        assertThat(hits.totalHits.value()).isEqualTo(5);
    }

    private List<String> ids(TopDocs hits) {
        return Arrays.stream(hits.scoreDocs).map(this::idOf).toList();
    }
    private String idOf(ScoreDoc sd) {
        try { return index.stored(sd.doc, "id"); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
