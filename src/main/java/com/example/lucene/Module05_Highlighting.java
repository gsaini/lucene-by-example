package com.example.lucene;

import com.example.lucene.util.Book;
import com.example.lucene.util.Console;
import com.example.lucene.util.SampleData;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

/**
 * MODULE 5 — Highlighting matched terms in search results.
 * <p>
 * Why highlighting matters: a user searches for "data systems" and gets back ten hits — they want
 * to see *where* in each document the words appeared. Lucene's {@code highlighter} module produces
 * short, formatted snippets ("fragments") around the matched terms.
 * <p>
 * Two pieces collaborate:
 * <ul>
 *   <li><b>Highlighter</b> — orchestrates fragment selection.</li>
 *   <li><b>Formatter</b> — wraps each matching token (default here: {@code &lt;b&gt;...&lt;/b&gt;}).</li>
 * </ul>
 */
public class Module05_Highlighting {

    public static void run() throws Exception {
        Console.header("Module 5 — Highlighting");

        try (Directory dir = new ByteBuffersDirectory();
             Analyzer analyzer = new StandardAnalyzer()) {

            buildIndex(dir, analyzer);

            try (DirectoryReader reader = DirectoryReader.open(dir)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                QueryParser parser = new QueryParser("description", analyzer);

                Query query = parser.parse("data OR search OR java");

                TopDocs hits = searcher.search(query, 10);

                // SimpleHTMLFormatter wraps each matched token in HTML tags. Pass any tags you like
                // (terminal colour codes, Markdown, custom span classes, ...).
                SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<b>", "</b>");
                QueryScorer scorer = new QueryScorer(query);
                Highlighter highlighter = new Highlighter(formatter, scorer);
                // Cap fragments at 80 chars so the output stays readable.
                highlighter.setTextFragmenter(new SimpleFragmenter(80));

                Console.section("Highlighted snippets for: " + query);
                for (ScoreDoc sd : hits.scoreDocs) {
                    Document doc = searcher.storedFields().document(sd.doc);
                    String text = doc.get("description");

                    // TokenSources re-runs the analyzer over the stored text to locate offsets.
                    // For huge fields you'd index term vectors with offsets instead — much faster.
                    String[] fragments = highlighter.getBestFragments(
                        TokenSources.getTokenStream("description", null, text, analyzer, -1),
                        text,
                        3 // max fragments per document
                    );

                    System.out.printf("  id=%s  title=%s%n", doc.get("id"), doc.get("title"));
                    for (String fragment : fragments) {
                        System.out.println("    > " + fragment);
                    }
                }
            }
        }
    }

    private static void buildIndex(Directory dir, Analyzer analyzer) throws Exception {
        IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
        try (IndexWriter writer = new IndexWriter(dir, cfg)) {
            for (Book book : SampleData.books()) {
                Document doc = new Document();
                doc.add(new StringField("id", book.id, Field.Store.YES));
                doc.add(new TextField("title", book.title, Field.Store.YES));
                doc.add(new TextField("description", book.description, Field.Store.YES));
                writer.addDocument(doc);
            }
            writer.commit();
        }
    }
}
