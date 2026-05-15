package com.example.lucene;

import com.example.lucene.util.Book;
import com.example.lucene.util.Console;
import com.example.lucene.util.SampleData;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleDocValuesField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;

/**
 * MODULE 7 — Sorting and custom scoring.
 * <p>
 * Out of the box Lucene ranks hits by relevance using <b>BM25</b>. Two ways to customise this:
 * <ul>
 *   <li><b>Sort</b> — ignore the score and sort by a doc-values field (e.g. rating desc).</li>
 *   <li><b>FunctionScoreQuery</b> — combine the BM25 score with a numeric signal
 *       (e.g. multiply score by rating, so highly rated books float up).</li>
 * </ul>
 * <p>
 * KEY RULE: anything you want to sort, facet, or use in a function expression must be stored as
 * a <b>doc-values</b> field. Indexing it as a Point or TextField alone is not enough.
 */
public class Module07_SortingAndScoring {

    public static void run() throws Exception {
        Console.header("Module 7 — Sorting and Scoring");

        try (Directory dir = new ByteBuffersDirectory();
             StandardAnalyzer analyzer = new StandardAnalyzer()) {

            buildIndex(dir, analyzer);

            try (DirectoryReader reader = DirectoryReader.open(dir)) {
                IndexSearcher searcher = new IndexSearcher(reader);

                Console.section("Default scoring (BM25)");
                showHits(searcher, MatchAllDocsQuery.INSTANCE, null);

                Console.section("Sort by year DESC");
                Sort byYear = new Sort(
                    new SortField("year", SortField.Type.INT, /* reverse */ true));
                showHits(searcher, MatchAllDocsQuery.INSTANCE, byYear);

                Console.section("Sort by rating DESC then year DESC");
                Sort byRatingThenYear = new Sort(
                    new SortField("rating", SortField.Type.DOUBLE, true),
                    new SortField("year", SortField.Type.INT, true));
                showHits(searcher, MatchAllDocsQuery.INSTANCE, byRatingThenYear);

                Console.section("Sort by category (alphabetical) for a faceted-feeling display");
                Sort byCategory = new Sort(new SortField("category_sort", SortField.Type.STRING));
                showHits(searcher, MatchAllDocsQuery.INSTANCE, byCategory);

                // Hybrid: keep BM25 relevance, but multiply by the doc's rating. Highly rated
                // and topically relevant books rise to the top together.
                Console.section("FunctionScoreQuery: BM25 * rating");
                QueryParser parser = new QueryParser("description", analyzer);
                Query baseQuery = parser.parse("data OR java OR search");
                Query boosted = FunctionScoreQuery.boostByValue(
                    baseQuery,
                    DoubleValuesSource.fromDoubleField("rating"));
                showHits(searcher, boosted, null);
            }
        }
    }

    private static void showHits(IndexSearcher searcher, Query query, Sort sort) throws Exception {
        TopDocs hits = (sort == null)
            ? searcher.search(query, 10)
            : searcher.search(query, 10, sort);
        for (ScoreDoc sd : hits.scoreDocs) {
            Document doc = searcher.storedFields().document(sd.doc);
            System.out.printf("  score=%-7s  year=%s  rating=%s  category=%-12s  title=%s%n",
                Float.isNaN(sd.score) ? "n/a" : String.format("%.3f", sd.score),
                doc.get("year_stored"),
                doc.get("rating_stored"),
                doc.get("category"),
                doc.get("title"));
        }
    }

    private static void buildIndex(Directory dir, StandardAnalyzer analyzer) throws Exception {
        IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
        try (IndexWriter writer = new IndexWriter(dir, cfg)) {
            for (Book book : SampleData.books()) {
                Document doc = new Document();
                doc.add(new StringField("id", book.id, Field.Store.YES));
                doc.add(new TextField("title", book.title, Field.Store.YES));
                doc.add(new TextField("description", book.description, Field.Store.YES));
                doc.add(new StringField("category", book.category, Field.Store.YES));

                // Doc-values for sorting & function queries. One per "sortable" field.
                doc.add(new SortedDocValuesField("category_sort", new BytesRef(book.category)));
                doc.add(new NumericDocValuesField("year", book.year));
                doc.add(new IntPoint("year_point", book.year));
                doc.add(new StoredField("year_stored", book.year));
                doc.add(new DoubleDocValuesField("rating", book.rating));
                doc.add(new StoredField("rating_stored", book.rating));

                writer.addDocument(doc);
            }
            writer.commit();
        }
    }
}
