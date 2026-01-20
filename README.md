<!-- Improved compatibility of back to top link -->

<a id="readme-top"></a>

[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![MIT License][license-shield]][license-url]

<br />
<div align="center">
  <h3 align="center">Handmade PostgreSQL</h3>

  <p align="center">
    Educational relational DBMS implemented from scratch in Java
    <br />
    <a href="#about-the-project"><strong>Explore the docs »</strong></a>
    <br />
    <br />
    <a href="#usage">View Demo</a>
    ·
    <a href="https://github.com/your_username/handmade-postgresql/issues">Report Bug</a>
    ·
    <a href="https://github.com/your_username/handmade-postgresql/issues">Request Feature</a>
  </p>
</div>

<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
      <ul>
        <li><a href="#built-with">Built With</a></li>
      </ul>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#roadmap">Roadmap</a></li>
    <li><a href="#architecture">Architecture</a></li>
    <li><a href="#testing">Testing</a></li>
    <li><a href="#license">License</a></li>
  </ol>
</details>

---

## About The Project

**Handmade PostgreSQL** is an educational implementation of a relational database management system inspired by PostgreSQL internals. The project incrementally implements core DBMS subsystems such as disk storage, buffer management, system catalogs, SQL processing pipeline, query optimization, and indexing.

The main goal is **to deeply understand how modern relational databases work internally**, not to build a production-ready database.

### Implemented Features

* Page-based storage engine (8KB Heap Pages)
* Buffer Pool Manager with LRU and Clock replacement policies
* System Catalog (tables / columns / data types)
* SQL pipeline: Lexer → Parser → Semantic Analyzer
* Planner → Optimizer → Executor
* Supported statements: CREATE, INSERT, SELECT
* Hash Index (Linear Hashing)
* B+Tree Index (range queries)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Built With

* [![Java][Java-shield]][Java-url]
* [![Gradle][Gradle-shield]][Gradle-url]

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## Getting Started

### Prerequisites

* Java 17 or newer
* Gradle

### Installation

1. Clone the repository

   ```sh
   git clone https://github.com/your_username/handmade-postgresql.git
   cd handmade-postgresql
   ```
2. Build the project
   ```
   ./gradlew clean build
   ```
4. Run
   ```
   src/main/java/system/cli/ServerMain.java
   ```
   at the same time run
   ```
   src/main/java/system/cli/ClientMain.java
   ```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## Usage

Example SQL session:

```sql
CREATE TABLE users (id INT, name VARCHAR);

INSERT INTO users VALUES (1, 'Alice');
INSERT INTO users VALUES (2, 'Bob');

SELECT name FROM users WHERE id > 1;
```

Result:

```
Bob
```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## Architecture

```
SQL
 ↓
Lexer
 ↓
Parser (AST)
 ↓
Semantic Analyzer
 ↓
Planner (Logical Plan)
 ↓
Optimizer (Physical Plan)
 ↓
Executors
 ↓
Result
```

### Storage Layer

* HeapPage (slot directory + variable-length records)
* PageFileManager (position-based disk I/O)

### Buffer Pool

* Fixed-size buffer pool
* Pin / Unpin mechanism
* Dirty page tracking
* Background flushing and checkpoints

### Indexes

* Hash Index — equality lookups (O(1) average)
* B+Tree Index — range scans (O(log n + k))

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## Testing

* Unit tests for each subsystem
* Negative and edge-case tests
* End-to-end SQL execution tests

```sh
./gradlew test
```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## Roadmap

* [x] Heap Storage Engine
* [x] Buffer Pool Manager
* [x] Catalog Manager
* [x] SQL Processing Pipeline
* [x] Query Execution Engine
* [x] Hash Index
* [x] B+Tree Index
* [ ] Join operators
* [ ] Aggregations
* [ ] Transactions & MVCC

---

## License

Distributed under the MIT License. See `LICENSE` for more information.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- MARKDOWN LINKS -->

[contributors-shield]: https://img.shields.io/github/contributors/your_username/handmade-postgresql.svg?style=for-the-badge
[contributors-url]: https://github.com/your_username/handmade-postgresql/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/your_username/handmade-postgresql.svg?style=for-the-badge
[forks-url]: https://github.com/your_username/handmade-postgresql/network/members
[stars-shield]: https://img.shields.io/github/stars/your_username/handmade-postgresql.svg?style=for-the-badge
[stars-url]: https://github.com/your_username/handmade-postgresql/stargazers
[issues-shield]: https://img.shields.io/github/issues/your_username/handmade-postgresql.svg?style=for-the-badge
[issues-url]: https://github.com/your_username/handmade-postgresql/issues
[license-shield]: https://img.shields.io/github/license/your_username/handmade-postgresql.svg?style=for-the-badge
[license-url]: https://github.com/your_username/handmade-postgresql/blob/main/LICENSE
[Java-shield]: https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white
[Java-url]: https://openjdk.org/
[Gradle-shield]: https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white
[Gradle-url]: https://gradle.org/
