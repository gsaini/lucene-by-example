package com.example.lucene;

import com.example.lucene.support.BookIndex;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Module 1 concepts: Directory, Analyzer, IndexWriter, TermQuery.
 * Each test exercises the *real* Lucene stack — no mocks.
 */
@DisplayName("Module 1: Hello Lucene")
class Module01_HelloLuceneIT {

    private BookIndex index;

    @BeforeEach
    void setUp() throws Exception {
        index = BookIndex.withSampleBooks();
    }

    @AfterEach
    void tearDown() throws Exception {
        index.close();
    }

    @Test
    @DisplayName("All 10 sample books are indexed and live")
    void indexes_all_sample_books() {
        assertThat(index.reader().numDocs()).isEqualTo(10);
    }

    @Test
    @DisplayName("TermQuery on a tokenized TextField matches the right documents")
    void term_query_finds_java_books() throws Exception {
        TopDocs hits = index.searcher().search(new TermQuery(new Term("title", "java")), 10);

        assertThat(hits.totalHits.value()).isEqualTo(2);
        List<String> ids = idsOf(hits);
        // Book #2: "Effective Java", Book #3: "Java Concurrency in Practice"
        assertThat(ids).containsExactlyInAnyOrder("2", "3");
    }

    @Test
    @DisplayName("StandardAnalyzer lower-cases tokens — querying the upper-case form misses")
    void termquery_is_case_sensitive_against_lowercased_index() throws Exception {
        // This is the #1 gotcha new Lucene users hit. The TermQuery does NOT analyze its term —
        // so capital-J "Java" can never match the lower-cased index.
        TopDocs hits = index.searcher().search(new TermQuery(new Term("title", "Java")), 10);
        assertThat(hits.totalHits.value()).isZero();
    }

    @Test
    @DisplayName("StringField is indexed verbatim — exact-case match required")
    void stringfield_is_not_analyzed() throws Exception {
        // "Programming" stored as a StringField must be queried with the exact case.
        assertThat(index.searcher().search(new TermQuery(new Term("category", "Programming")), 20)
            .totalHits.value()).isEqualTo(5);
        assertThat(index.searcher().search(new TermQuery(new Term("category", "programming")), 20)
            .totalHits.value()).isZero();
    }

    @Test
    @DisplayName("MatchAllDocsQuery returns every live document")
    void match_all() throws Exception {
        TopDocs hits = index.searcher().search(MatchAllDocsQuery.INSTANCE, 50);
        assertThat(hits.totalHits.value()).isEqualTo(10);
    }

    private List<String> idsOf(TopDocs hits) throws Exception {
        return Arrays.stream(hits.scoreDocs)
            .map(this::idUnchecked)
            .toList();
    }

    private String idUnchecked(ScoreDoc sd) {
        try { return index.stored(sd.doc, "id"); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
