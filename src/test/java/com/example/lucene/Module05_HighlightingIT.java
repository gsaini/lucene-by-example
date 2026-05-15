package com.example.lucene;

import com.example.lucene.support.BookIndex;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TokenSources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Module 5: Highlighter wraps matched terms in the configured tags")
class Module05_HighlightingIT {

    private BookIndex index;

    @BeforeEach void setUp() throws Exception { index = BookIndex.withSampleBooks(); }
    @AfterEach void tearDown() throws Exception { index.close(); }

    @Test
    @DisplayName("Best fragment wraps each matched term in <em>...</em>")
    void wraps_matches_in_em_tags() throws Exception {
        QueryParser parser = new QueryParser("description", index.analyzer());
        Query query = parser.parse("data systems");

        SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<em>", "</em>");
        Highlighter highlighter = new Highlighter(formatter, new QueryScorer(query));
        highlighter.setTextFragmenter(new SimpleFragmenter(80));

        TopDocs hits = index.searcher().search(query, 5);
        assertThat(hits.totalHits.value()).isGreaterThanOrEqualTo(1);

        boolean foundHighlightedHit = false;
        for (ScoreDoc sd : hits.scoreDocs) {
            String text = index.stored(sd.doc, "description");
            String[] fragments = highlighter.getBestFragments(
                TokenSources.getTokenStream("description", null, text, index.analyzer(), -1),
                text, 3);

            if (fragments.length > 0) {
                String joined = String.join(" ", fragments);
                // At least one of the matched terms is wrapped in our chosen tags.
                if (joined.contains("<em>data</em>") || joined.contains("<em>systems</em>")) {
                    foundHighlightedHit = true;
                }
            }
        }
        assertThat(foundHighlightedHit)
            .as("at least one fragment should contain a highlighted term")
            .isTrue();
    }

    @Test
    @DisplayName("Highlighter respects the maxNumFragments cap")
    void caps_number_of_fragments() throws Exception {
        QueryParser parser = new QueryParser("description", index.analyzer());
        Query query = parser.parse("search");

        Highlighter highlighter = new Highlighter(
            new SimpleHTMLFormatter("<b>", "</b>"),
            new QueryScorer(query));
        highlighter.setTextFragmenter(new SimpleFragmenter(20));

        TopDocs hits = index.searcher().search(query, 5);
        for (ScoreDoc sd : hits.scoreDocs) {
            String text = index.stored(sd.doc, "description");
            String[] fragments = highlighter.getBestFragments(
                TokenSources.getTokenStream("description", null, text, index.analyzer(), -1),
                text, /* maxNumFragments */ 2);
            assertThat(fragments.length).isLessThanOrEqualTo(2);
        }
    }
}
