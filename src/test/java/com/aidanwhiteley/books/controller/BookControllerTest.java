package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.repository.BookRepositoryTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BookControllerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookControllerTest.class);

    private static final String GENRE_TOO_LONG =
            "abcdefghjijklmnopqrstuvwxyz01234567890";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    public void createBook() {
        ResponseEntity<Book> response = postBookToServer();
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void tryToCreateInvalidBook() {
        // An empty book should fail
        Book emptyBook = new Book();
        HttpEntity<Book> request = new HttpEntity<>(emptyBook);
        ResponseEntity<Book> response = testRestTemplate
                .exchange("/api/books", HttpMethod.POST, request, Book.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        // Create a valid book and then exceed one of the max field sizes
        Book testBook = BookRepositoryTest.createTestBook();
        testBook.setGenre(GENRE_TOO_LONG);
        request = new HttpEntity<>(testBook);
        response = testRestTemplate
                .exchange("/api/books", HttpMethod.POST, request, Book.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void findBookById() {
        ResponseEntity<Book> response = postBookToServer();
        HttpHeaders headers = response.getHeaders();
        URI uri = headers.getLocation();

        Book book = testRestTemplate.getForObject(uri, Book.class);
        assertEquals(book.getId(), uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1));
    }

    @Test
    public void updateBook() {
        ResponseEntity<Book> response = postBookToServer();
        HttpHeaders headers = response.getHeaders();
        URI uri = headers.getLocation();

        Book book = testRestTemplate.getForObject(uri, Book.class);
        final String updatedTitle = "An updated book title";
        book.setTitle(updatedTitle);
        HttpEntity<Book> putData = new HttpEntity<>(book);

        ResponseEntity<Book> putResponse = testRestTemplate
                .exchange("/api/books", HttpMethod.PUT, putData, Book.class);
        assertEquals(putResponse.getStatusCode(), HttpStatus.NO_CONTENT);
        headers = response.getHeaders();
        uri = headers.getLocation();

        Book updatedBook = testRestTemplate.getForObject(uri, Book.class);
        assertEquals(updatedBook.getTitle(), updatedTitle);
    }

    @Test
    public void findByAuthor() {

        postBookToServer();

        ResponseEntity<String> response = testRestTemplate.exchange("/api/books?author=" + BookRepositoryTest.DR_ZEUSS, HttpMethod.GET,
                null, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        List<Book> books = JsonPath.read(response.getBody(), "$");
        LOGGER.debug("Retrieved JSON was: " + response.getBody());

        assertTrue("No books found", books.size() > 0);
    }

    private ResponseEntity<Book> postBookToServer() {
        Book testBook = BookRepositoryTest.createTestBook();
        HttpEntity<Book> request = new HttpEntity<>(testBook);

        return testRestTemplate
                .exchange("/api/books", HttpMethod.POST, request, Book.class);
    }

}