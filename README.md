# Apache Lucene — Getting Started

![Java](https://img.shields.io/badge/Java-17-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Apache Lucene](https://img.shields.io/badge/Apache%20Lucene-9.12.0-D22128?style=for-the-badge&logo=apache&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.8+-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![License](https://img.shields.io/badge/License-Apache%202.0-0F80C1?style=for-the-badge&logo=apache&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-Cross--Platform-4EAA25?style=for-the-badge&logo=linux&logoColor=white)
![Status](https://img.shields.io/badge/Status-Learning%20Project-0A66C2?style=for-the-badge&logo=readthedocs&logoColor=white)

![GitHub last commit](https://img.shields.io/github/last-commit/gsaini/apache-lucene-getting-started?style=flat-square)
![GitHub repo size](https://img.shields.io/github/repo-size/gsaini/apache-lucene-getting-started?style=flat-square)
![GitHub stars](https://img.shields.io/github/stars/gsaini/apache-lucene-getting-started?style=social)

A hands-on learning project that walks through the core features of
[Apache Lucene](https://lucene.apache.org/) one self-contained module at a time.

Everything runs entirely in memory against a tiny built-in book catalogue, so
you can read a module, run it, tweak it, and immediately see how the output
changes — no external services, no setup.

---

## Table of contents

- [Requirements](#requirements)
- [Running](#running)
- [What each module covers](#what-each-module-covers)
- [Architecture](#architecture)
  - [End-to-end pipeline](#end-to-end-pipeline)
  - [Indexing pipeline (write path)](#indexing-pipeline-write-path)
  - [Searching pipeline (read path)](#searching-pipeline-read-path)
  - [Inverted index — the core data structure](#inverted-index--the-core-data-structure)
  - [Field-type decision matrix](#field-type-decision-matrix)
  - [Project module map](#project-module-map)
- [Three rules to remember](#three-rules-to-remember)
- [Where to go next](#where-to-go-next)

---

## Requirements

- Java 17+
- Maven 3.8+
- Lucene 9.12.0 (declared in [pom.xml](pom.xml), pulled by Maven)

## Running

Run every module in order:

```bash
mvn -q compile exec:java
```

Run a single module by number (1–10):

```bash
mvn -q compile exec:java -Dexec.args=3
```

Run a few modules in sequence:

```bash
mvn -q compile exec:java -Dexec.args="1 3 7"
```

## What each module covers

| #  | Module | What you'll learn |
|----|--------|-------------------|
| 1  | [Module01_HelloLucene.java](src/main/java/com/example/lucene/Module01_HelloLucene.java) | Directory, Analyzer, IndexWriter, IndexSearcher, TermQuery — the minimum viable pipeline. |
| 2  | [Module02_FieldsAndAnalyzers.java](src/main/java/com/example/lucene/Module02_FieldsAndAnalyzers.java) | StringField vs TextField vs StoredField vs Point vs DocValues; how analyzers produce different tokens. |
| 3  | [Module03_QueryTypes.java](src/main/java/com/example/lucene/Module03_QueryTypes.java) | TermQuery, PhraseQuery, BooleanQuery (MUST / SHOULD / MUST_NOT / FILTER), WildcardQuery, PrefixQuery, FuzzyQuery, RegexpQuery, numeric range queries. |
| 4  | [Module04_QueryParser.java](src/main/java/com/example/lucene/Module04_QueryParser.java) | Lucene's classic query-string syntax, including MultiFieldQueryParser with per-field boosts. |
| 5  | [Module05_Highlighting.java](src/main/java/com/example/lucene/Module05_Highlighting.java) | Generating snippet fragments with matched terms wrapped in HTML tags. |
| 6  | [Module06_Faceting.java](src/main/java/com/example/lucene/Module06_Faceting.java) | Sidebar-style facet counts using FacetField + Taxonomy index. |
| 7  | [Module07_SortingAndScoring.java](src/main/java/com/example/lucene/Module07_SortingAndScoring.java) | Sort by doc-values fields; FunctionScoreQuery to blend BM25 with a numeric signal. |
| 8  | [Module08_UpdatesAndDeletes.java](src/main/java/com/example/lucene/Module08_UpdatesAndDeletes.java) | updateDocument by primary key, deleteDocuments by Term and Query, deleteAll. |
| 9  | [Module09_CustomAnalyzer.java](src/main/java/com/example/lucene/Module09_CustomAnalyzer.java) | Building an Analyzer pipeline with stop-words, synonyms, stemming, edge n-grams, ASCII folding. |
| 10 | [Module10_Suggester.java](src/main/java/com/example/lucene/Module10_Suggester.java) | AnalyzingInfixSuggester for fast autocomplete. |

---

## Architecture

### End-to-end pipeline

The big picture: a domain object enters on the left, an index is built in the middle, and
queries flow back through the right.

```
   ┌──────────────────────────────────────────────────────────────────────────┐
   │                          WRITE PATH (indexing)                           │
   └──────────────────────────────────────────────────────────────────────────┘

   ┌────────────┐    field      ┌────────────┐   analyze    ┌────────────┐
   │  Domain    │ ───mapping──▶ │  Document  │ ───tokens──▶ │  Analyzer  │
   │  object    │               │  + Fields  │              │   chain    │
   │ (Book,     │               │            │              │            │
   │  Product…) │               └─────┬──────┘              └─────┬──────┘
   └────────────┘                     │                           │
                                      ▼                           ▼
                              ┌──────────────────┐       ┌──────────────────┐
                              │   IndexWriter    │◀──────│   Token stream   │
                              │ (transactional,  │       │  + attributes    │
                              │  one per index)  │       └──────────────────┘
                              └────────┬─────────┘
                                       │ flush / commit
                                       ▼
                              ┌──────────────────┐
                              │     Directory    │   (FSDirectory, MMapDirectory,
                              │  ┌────────────┐  │    ByteBuffersDirectory, …)
                              │  │ segment_1  │  │
                              │  │ segment_2  │  │   ── segments are immutable
                              │  │ segment_3  │  │      and merged in the background
                              │  └────────────┘  │
                              └────────┬─────────┘
                                       │
   ┌───────────────────────────────────┼──────────────────────────────────────┐
   │                                   ▼                READ PATH (searching) │
   └───────────────────────────────────┬──────────────────────────────────────┘
                                       │
                              ┌────────▼─────────┐
                              │  IndexReader     │   point-in-time snapshot
                              │ (DirectoryReader │   of all segments
                              │  .open(dir))     │
                              └────────┬─────────┘
                                       │
                              ┌────────▼─────────┐       ┌────────────────────┐
                              │  IndexSearcher   │◀──────│      Query         │
                              │ (BM25Similarity, │       │ (TermQuery,        │
                              │  collectors,     │       │  BooleanQuery,     │
                              │  rewrites)       │       │  PhraseQuery, …)   │
                              └────────┬─────────┘       └────────────────────┘
                                       │
                                       ▼
                              ┌──────────────────┐
                              │     TopDocs      │   ranked ScoreDoc[]
                              │  (scores + ids)  │   + optional facets,
                              │                  │     highlights, sorts
                              └──────────────────┘
```

### Indexing pipeline (write path)

Inside `IndexWriter.addDocument(...)`, each `Field` flows through the analyzer chain, and the
resulting tokens are recorded in postings, doc-values, points and stored fields — depending on
which `FieldType` flags were set.

```
   Document
   ├── StringField "id"       ─────▶ exact-term postings        (no analysis)
   ├── TextField "title"      ─────▶ Analyzer ─▶ tokens ─▶ postings
   ├── TextField "description"─────▶ Analyzer ─▶ tokens ─▶ postings
   ├── IntPoint  "year"       ─────▶ BKD tree                    (range queries)
   ├── DoubleDocValuesField   ─────▶ columnar doc-values         (sort / facet / function)
   ├── SortedDocValuesField   ─────▶ columnar doc-values         (sort / facet)
   ├── FacetField "Category"  ─────▶ Taxonomy index              (facet counts)
   └── StoredField "raw"      ─────▶ stored-fields blob          (retrieval only)

                                  Analyzer chain
                                  ───────────────
   raw text ──▶ Tokenizer ──▶ TokenFilter ──▶ TokenFilter ──▶ … ──▶ indexed tokens
                  ▲              ▲              ▲
                  │              │              │
                  │       LowerCaseFilter  StopFilter   SynonymGraphFilter   PorterStemFilter
                  │
              e.g. StandardTokenizer (Unicode word breaks)
```

### Searching pipeline (read path)

```
   user input ──▶ QueryParser ──▶ Query tree ──▶ rewrite ──▶ Weight ──▶ Scorer
                                                                          │
                                                  per-segment iteration ──┘
                                                                          │
                                          BM25 score + Similarity         │
                                                                          ▼
                                                                  TopDocsCollector
                                                                          │
                                                                          ▼
                                            ┌─────────────────────────────┴───┐
                                            │ TopDocs (ScoreDoc[] + totalHits)│
                                            └─────────────────────────────────┘
                                                          │
                            ┌─────────────────────────────┼─────────────────────────────┐
                            ▼                             ▼                             ▼
                  StoredFields (retrieval)        Highlighter (snippets)        Facets (counts)
```

### Inverted index — the core data structure

This is the idea every Lucene feature is built on: instead of storing "doc → words", Lucene
flips it to "word → docs". Looking up a term is then an O(1) hash/Trie lookup followed by a
walk over its postings list.

```
                           Forward (what we wrote)
                           ───────────────────────
   docId=1   "Lucene in Action"
   docId=2   "Effective Java"
   docId=3   "Java Concurrency in Practice"

                           Inverted (what Lucene stores)
                           ─────────────────────────────
   term            postings list (docId → freq, positions, offsets)
   ─────────       ───────────────────────────────────────────────
   action     ──▶  [ (1, freq=1, pos=[2]) ]
   concurrency──▶  [ (3, freq=1, pos=[1]) ]
   effective  ──▶  [ (2, freq=1, pos=[0]) ]
   in         ──▶  [ (1, freq=1, pos=[1]), (3, freq=1, pos=[2]) ]
   java       ──▶  [ (2, freq=1, pos=[1]), (3, freq=1, pos=[0]) ]
   lucene     ──▶  [ (1, freq=1, pos=[0]) ]
   practice   ──▶  [ (3, freq=1, pos=[3]) ]

   ▲ stored in segment files: .tim/.tip (term dictionary), .doc/.pos (postings)
```

A `TermQuery("java")` walks the postings list under `java` → docs `[2, 3]`. A `PhraseQuery`
also walks positions to verify words appear adjacent. BM25 scoring uses the frequency and
length normalisation from this same index.

### Field-type decision matrix

A quick reference for which field type to pick for which purpose:

| Need                        | Use this field                                                          |
| --------------------------- | ----------------------------------------------------------------------- |
| Exact-match on an ID/code   | `StringField`                                                           |
| Full-text search            | `TextField` (with the right Analyzer)                                   |
| Just return it with the hit | `StoredField`                                                           |
| Numeric range query         | `IntPoint` / `LongPoint` / `DoublePoint`                                |
| Sort or facet               | `SortedDocValuesField`, `NumericDocValuesField`, `DoubleDocValuesField` |
| Facet counts (taxonomy)     | `FacetField` (+ `FacetsConfig.build(...)`)                              |
| Autocomplete                | feed source into `AnalyzingInfixSuggester`                              |

> One logical field often becomes 2–3 Lucene fields. For example, `year` is usually
> `IntPoint` (range query) + `NumericDocValuesField` (sort) + `StoredField` (retrieval).

### Project module map

How the 10 modules fit on the architecture diagram:

```
                ┌─────────────────────────────────────────────────────────────┐
                │                       INDEX BUILDING                        │
                │                                                             │
                │   Module 1  Hello Lucene        Module 8  Update/Delete    │
                │   Module 2  Field types         Module 9  Custom Analyzer  │
                │   Module 6  Facet indexing                                  │
                └────────────────────────┬────────────────────────────────────┘
                                         │
                                         ▼
                ┌─────────────────────────────────────────────────────────────┐
                │                          QUERYING                           │
                │                                                             │
                │   Module 3  Query types         Module 4  QueryParser      │
                │   Module 7  Sort / function     Module 10 Suggester        │
                └────────────────────────┬────────────────────────────────────┘
                                         │
                                         ▼
                ┌─────────────────────────────────────────────────────────────┐
                │                    POST-PROCESSING                          │
                │                                                             │
                │   Module 5  Highlighting        Module 6  Facet counts     │
                └─────────────────────────────────────────────────────────────┘
```

---

## Three rules to remember

1. **Field type decides what queries are possible.** A field that is not indexed
   cannot be searched. A field that is not stored cannot be retrieved.
   Sorting and faceting need a doc-values flavour of the field.
2. **Use the same Analyzer for indexing and searching.** Otherwise your query
   terms won't match the tokens you wrote to the index. Module 2 makes this
   obvious by showing the token output of four analyzers side-by-side.
3. **Documents are immutable.** "Update" means delete + add, keyed off a unique
   field. See Module 8.

## Where to go next

- The official [Lucene demo](https://lucene.apache.org/core/9_12_0/demo/index.html)
  shows indexing of real files from disk.
- [Lucene's MIGRATE.md](https://github.com/apache/lucene/blob/main/lucene/MIGRATE.md)
  is the best place to see what changes between major versions.
- Real-world systems built on Lucene worth studying: Elasticsearch, OpenSearch,
  Solr — they reuse the APIs you've practised in this project.
