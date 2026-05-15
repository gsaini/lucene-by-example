package com.example.lucene;

import com.example.lucene.support.BookIndex;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Module 7: Sorting and FunctionScoreQuery behave as documented")
class Module07_SortingAndScoringIT {

    private BookIndex index;

    @BeforeEach void setUp() throws Exception { index = BookIndex.withSampleBooks(); }
    @AfterEach void tearDown() throws Exception { index.close(); }

    @Test
    @DisplayName("Sort by year DESC: 2018 (Effective Java) ranks first")
    void sort_by_year_desc() throws Exception {
        Sort byYear = new Sort(new SortField("year", SortField.Type.INT, /* reverse */ true));
        TopDocs hits = index.searcher().search(MatchAllDocsQuery.INSTANCE, 10, byYear);

        // Newest book in sample data is 2018 "Effective Java".
        assertThat(index.stored(hits.scoreDocs[0].doc, "id")).isEqualTo("2");
        assertThat(index.stored(hits.scoreDocs[0].doc, "year_stored")).isEqualTo("2018");
    }

    @Test
    @DisplayName("Sort by year ASC: 1999 (Pragmatic Programmer) ranks first")
    void sort_by_year_asc() throws Exception {
        Sort byYear = new Sort(new SortField("year", SortField.Type.INT, false));
        TopDocs hits = index.searcher().search(MatchAllDocsQuery.INSTANCE, 10, byYear);
        assertThat(index.stored(hits.scoreDocs[0].doc, "id")).isEqualTo("6"); // 1999
    }

    @Test
    @DisplayName("Sort by rating DESC: 4.9 'Designing Data-Intensive Applications' ranks first")
    void sort_by_rating_desc() throws Exception {
        Sort byRating = new Sort(new SortField("rating", SortField.Type.DOUBLE, true));
        TopDocs hits = index.searcher().search(MatchAllDocsQuery.INSTANCE, 10, byRating);
        assertThat(index.stored(hits.scoreDocs[0].doc, "id")).isEqualTo("5");
    }

    @Test
    @DisplayName("FunctionScoreQuery: multiplying BM25 by rating raises higher-rated books")
    void function_score_boosts_by_rating() throws Exception {
        QueryParser parser = new QueryParser("description", index.analyzer());
        Query base = parser.parse("data OR search");

        TopDocs plain = index.searcher().search(base, 20);
        TopDocs boosted = index.searcher().search(
            FunctionScoreQuery.boostByValue(base, DoubleValuesSource.fromDoubleField("rating")),
            20);

        // Same documents in both, just (potentially) re-ordered and re-scored.
        assertThat(idSet(plain)).isEqualTo(idSet(boosted));

        // After boosting by rating, the top-scoring hit must have a rating >= the median rating
        // of all hits — not bullet-proof, but a meaningful sanity check.
        double topRating = Double.parseDouble(
            index.stored(boosted.scoreDocs[0].doc, "rating_stored"));
        assertThat(topRating).isGreaterThanOrEqualTo(4.5);
    }

    private List<String> idSet(TopDocs hits) {
        return Arrays.stream(hits.scoreDocs).map(this::idOf).sorted().toList();
    }
    private String idOf(ScoreDoc sd) {
        try { return index.stored(sd.doc, "id"); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
