package com.example.lucene;

import com.example.lucene.util.Book;
import com.example.lucene.util.Console;
import com.example.lucene.util.SampleData;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

/**
 * MODULE 4 — Turning user-typed text into a Query with QueryParser.
 * <p>
 * Lucene's classic {@link QueryParser} understands a small, well-known mini-language:
 * <pre>
 *   java                       single term
 *   "data systems"             phrase
 *   title:java                 field-specific term
 *   java AND concurrency       boolean AND
 *   java OR python             boolean OR
 *   "data systems"~3           phrase with slop 3
 *   lucen~                     fuzzy match (default edit distance)
 *   mart*                      wildcard / prefix
 *   year:[2005 TO 2015]        inclusive range
 *   author:"Robert Martin"^2.0 phrase with boost
 *   -clean                     must-not contain
 * </pre>
 * <p>
 * Two gotchas to internalise:
 * <ol>
 *   <li>The same analyzer must be used for indexing and parsing, otherwise the parsed terms
 *       won't match what is in the index.</li>
 *   <li>QueryParser has a few syntax-special characters ({@code + - && || ! ( ) { } [ ] ^ " ~ * ? : \ /}).
 *       Always run untrusted input through {@link QueryParser#escape(String)} or use the
 *       programmatic Query API from Module 3 instead.</li>
 * </ol>
 */
public class Module04_QueryParser {

    public static void run() throws Exception {
        Console.header("Module 4 — QueryParser");

        try (Directory dir = new ByteBuffersDirectory();
             StandardAnalyzer analyzer = new StandardAnalyzer()) {

            buildIndex(dir, analyzer);

            try (DirectoryReader reader = DirectoryReader.open(dir)) {
                IndexSearcher searcher = new IndexSearcher(reader);

                // Single default field: any bare term goes against "description".
                QueryParser parser = new QueryParser("description", analyzer);

                runParsedQuery(searcher, parser, "java");
                runParsedQuery(searcher, parser, "\"data systems\"");
                runParsedQuery(searcher, parser, "title:java AND concurrency");
                runParsedQuery(searcher, parser, "lucen~");                 // fuzzy
                runParsedQuery(searcher, parser, "mart*");                  // wildcard
                runParsedQuery(searcher, parser, "search -elasticsearch");  // must-not
                runParsedQuery(searcher, parser, "author:\"Robert C. Martin\"^2.0");

                // Search the same query against multiple fields at once. Each field can carry
                // its own boost weight, which is the simplest "title matters more than body" trick.
                String[] fields  = { "title", "author", "description" };
                java.util.Map<String, Float> boosts = java.util.Map.of(
                    "title", 3.0f,
                    "author", 2.0f,
                    "description", 1.0f);
                MultiFieldQueryParser multi = new MultiFieldQueryParser(fields, analyzer, boosts);
                runParsedQuery(searcher, multi, "java");
            }
        }
    }

    private static void runParsedQuery(IndexSearcher searcher, QueryParser parser, String input)
            throws Exception {
        System.out.println();
        System.out.println("> Input:  " + input);
        Query q = parser.parse(input);
        System.out.println("  Parsed: " + q + "  (" + q.getClass().getSimpleName() + ")");

        TopDocs hits = searcher.search(q, 5);
        System.out.println("  totalHits = " + hits.totalHits);
        for (ScoreDoc sd : hits.scoreDocs) {
            Document doc = searcher.storedFields().document(sd.doc);
            System.out.printf("    score=%.3f  id=%s  title=%s%n",
                sd.score, doc.get("id"), doc.get("title"));
        }
    }

    private static void buildIndex(Directory dir, StandardAnalyzer analyzer) throws Exception {
        IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
        try (IndexWriter writer = new IndexWriter(dir, cfg)) {
            for (Book book : SampleData.books()) {
                Document doc = new Document();
                doc.add(new StringField("id", book.id, Field.Store.YES));
                doc.add(new TextField("title", book.title, Field.Store.YES));
                doc.add(new TextField("author", book.author, Field.Store.YES));
                doc.add(new TextField("description", book.description, Field.Store.YES));
                writer.addDocument(doc);
            }
            writer.commit();
        }
    }
}
