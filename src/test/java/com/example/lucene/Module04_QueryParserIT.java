package com.example.lucene;

import com.example.lucene.support.BookIndex;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Module 4: QueryParser turns query strings into the right Query trees")
class Module04_QueryParserIT {

    private BookIndex index;
    private QueryParser parser;

    @BeforeEach
    void setUp() throws Exception {
        index = BookIndex.withSampleBooks();
        parser = new QueryParser("description", index.analyzer());
    }
    @AfterEach void tearDown() throws Exception { index.close(); }

    @Test
    @DisplayName("Bare term targets the default field")
    void bare_term_uses_default_field() throws Exception {
        // "java" appears in description of #2 only (#3's "Java" is in title, not description).
        assertThat(ids(parser.parse("java"))).containsExactly("2");
    }

    @Test
    @DisplayName("Quoted text is parsed as a phrase query")
    void phrase_syntax() throws Exception {
        assertThat(ids(parser.parse("\"data systems\""))).containsExactly("5");
    }

    @Test
    @DisplayName("Field prefix + AND across fields")
    void field_prefix_and_boolean() throws Exception {
        // Both "java" and "concurrency" must appear in the title field.
        assertThat(ids(parser.parse("title:java AND title:concurrency")))
            .containsExactly("3");
    }

    @Test
    @DisplayName("Tilde marker produces a FuzzyQuery")
    void fuzzy_syntax() throws Exception {
        // "~" without a number = default max edits (2). "lucen" matches "lucene" in #1's and
        // #9's descriptions, so #1 must be in the results.
        List<String> ids = ids(parser.parse("lucen~"));
        assertThat(ids).contains("1");
    }

    @Test
    @DisplayName("Minus sign is must-not")
    void must_not_syntax() throws Exception {
        // description must contain "search" AND must NOT contain "elasticsearch".
        // #9 has only "elasticsearch" (single token; not split into 'search') so doesn't appear
        // in the positive set either way. Books with "search" in description: #1, #7, #8.
        assertThat(ids(parser.parse("search -elasticsearch")))
            .containsExactlyInAnyOrder("1", "7", "8");
    }

    @Test
    @DisplayName("Phrase query against a specific field with a boost")
    void boosted_phrase_against_field() throws Exception {
        assertThat(ids(parser.parse("author:\"Robert C. Martin\"^2.0")))
            .containsExactly("4");
    }

    @Test
    @DisplayName("MultiFieldQueryParser searches all listed fields")
    void multi_field_search() throws Exception {
        String[] fields  = { "title", "author", "description" };
        Map<String, Float> boosts = Map.of("title", 3.0f, "author", 2.0f, "description", 1.0f);
        MultiFieldQueryParser mfp = new MultiFieldQueryParser(fields, index.analyzer(), boosts);

        // "java" lives in title:#2,#3 and description:#2 → unique ids {2,3}.
        assertThat(ids(mfp.parse("java"))).containsExactlyInAnyOrder("2", "3");
    }

    private List<String> ids(Query q) throws Exception {
        TopDocs hits = index.searcher().search(q, 20);
        return Arrays.stream(hits.scoreDocs).map(this::idOf).toList();
    }
    private String idOf(ScoreDoc sd) {
        try { return index.stored(sd.doc, "id"); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
