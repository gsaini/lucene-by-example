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
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

/**
 * MODULE 8 — Updating and deleting documents.
 * <p>
 * Lucene documents are <em>immutable</em>. There is no "update field X" — instead, you delete
 * the old document and add a new one. {@link IndexWriter#updateDocument(Term, Iterable)} is a
 * convenience that does both atomically.
 * <p>
 * Common gotcha: the {@link Term} you pass to {@code updateDocument} / {@code deleteDocuments}
 * must reference a field that is <b>indexed and unique</b>, almost always a {@link StringField}
 * holding the primary key. A TextField won't behave as you expect because the value is tokenised.
 * <p>
 * Visibility rule: a writer's changes are not visible to any reader opened before the change.
 * You must either {@code reader = DirectoryReader.openIfChanged(oldReader, writer)} or close
 * and re-open the reader. We use the second form here for clarity.
 */
public class Module08_UpdatesAndDeletes {

    public static void run() throws Exception {
        Console.header("Module 8 — Updates and Deletes");

        try (Directory dir = new ByteBuffersDirectory();
             StandardAnalyzer analyzer = new StandardAnalyzer()) {

            IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
            try (IndexWriter writer = new IndexWriter(dir, cfg)) {

                // 1. Initial population.
                for (Book book : SampleData.books()) {
                    writer.addDocument(toDocument(book));
                }
                writer.commit();
                report(dir, "After initial indexing");

                // 2. Update a single document by primary key.
                //    Term("id", "2") matches the StringField("id", "2"). updateDocument()
                //    deletes any doc with that term *and* adds the new one in one step.
                Document updated = toDocument(new Book(
                    "2",
                    "Effective Java (4th Edition)",
                    "Joshua Bloch",
                    "Programming",
                    "Updated edition — best practices for modern Java (records, sealed classes, ...).",
                    2025, 4.9));
                writer.updateDocument(new Term("id", "2"), updated);
                writer.commit();
                report(dir, "After updating id=2");

                // 3. Delete by term — every document whose "category" StringField equals "Search".
                writer.deleteDocuments(new Term("category", "Search"));
                writer.commit();
                report(dir, "After deleting category=Search");

                // 4. Delete by Query — anything matching this TermQuery is removed.
                writer.deleteDocuments(new TermQuery(new Term("category", "Architecture")));
                writer.commit();
                report(dir, "After deleting category=Architecture (via Query)");

                // 5. Wipe the index completely.
                writer.deleteAll();
                writer.commit();
                report(dir, "After deleteAll()");
            }
        }
    }

    private static void report(Directory dir, String label) throws Exception {
        Console.section(label);
        try (DirectoryReader reader = DirectoryReader.open(dir)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs hits = searcher.search(MatchAllDocsQuery.INSTANCE, 20);
            System.out.println("  live documents in index: " + reader.numDocs());
            for (ScoreDoc sd : hits.scoreDocs) {
                Document doc = searcher.storedFields().document(sd.doc);
                System.out.printf("    id=%s  category=%s  title=%s%n",
                    doc.get("id"), doc.get("category"), doc.get("title"));
            }
        }
    }

    private static Document toDocument(Book book) {
        Document doc = new Document();
        doc.add(new StringField("id", book.id, Field.Store.YES));
        doc.add(new StringField("category", book.category, Field.Store.YES));
        doc.add(new TextField("title", book.title, Field.Store.YES));
        doc.add(new TextField("description", book.description, Field.Store.YES));
        return doc;
    }
}
