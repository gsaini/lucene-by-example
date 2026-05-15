package com.example.lucene;

import com.example.lucene.util.Book;
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
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Module 8: update/delete operations affect the live index correctly")
class Module08_UpdatesAndDeletesIT {

    private Directory dir;
    private StandardAnalyzer analyzer;
    private IndexWriter writer;

    @BeforeEach
    void setUp() throws Exception {
        dir = new ByteBuffersDirectory();
        analyzer = new StandardAnalyzer();
        writer = new IndexWriter(dir, new IndexWriterConfig(analyzer));
        for (Book book : SampleData.books()) {
            writer.addDocument(toDocument(book));
        }
        writer.commit();
    }

    @AfterEach
    void tearDown() throws Exception {
        writer.close();
        analyzer.close();
        dir.close();
    }

    @Test
    @DisplayName("updateDocument(Term, doc) replaces an existing document by primary key")
    void update_replaces_by_id() throws Exception {
        Document replacement = toDocument(new Book(
            "2", "Effective Java (4th Edition)", "Joshua Bloch", "Programming",
            "Updated description.", 2025, 4.9));
        writer.updateDocument(new Term("id", "2"), replacement);
        writer.commit();

        try (DirectoryReader reader = DirectoryReader.open(dir)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs hits = searcher.search(new TermQuery(new Term("id", "2")), 5);

            assertThat(hits.totalHits.value()).isEqualTo(1);
            String title = searcher.storedFields().document(hits.scoreDocs[0].doc).get("title");
            assertThat(title).isEqualTo("Effective Java (4th Edition)");
            assertThat(reader.numDocs()).isEqualTo(10); // no net change in doc count
        }
    }

    @Test
    @DisplayName("deleteDocuments(Term) removes every doc that matches the term")
    void delete_by_term() throws Exception {
        // Three sample books are in category "Search".
        writer.deleteDocuments(new Term("category", "Search"));
        writer.commit();

        try (DirectoryReader reader = DirectoryReader.open(dir)) {
            assertThat(reader.numDocs()).isEqualTo(7);
            IndexSearcher searcher = new IndexSearcher(reader);
            assertThat(searcher.search(new TermQuery(new Term("category", "Search")), 10)
                .totalHits.value()).isZero();
        }
    }

    @Test
    @DisplayName("deleteDocuments(Query) removes matches of an arbitrary Query")
    void delete_by_query() throws Exception {
        writer.deleteDocuments(new TermQuery(new Term("category", "Architecture")));
        writer.commit();

        try (DirectoryReader reader = DirectoryReader.open(dir)) {
            assertThat(reader.numDocs()).isEqualTo(8);
        }
    }

    @Test
    @DisplayName("deleteAll() empties the index")
    void delete_all() throws Exception {
        writer.deleteAll();
        writer.commit();

        try (DirectoryReader reader = DirectoryReader.open(dir)) {
            assertThat(reader.numDocs()).isZero();
            IndexSearcher searcher = new IndexSearcher(reader);
            assertThat(searcher.search(MatchAllDocsQuery.INSTANCE, 10).totalHits.value()).isZero();
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
