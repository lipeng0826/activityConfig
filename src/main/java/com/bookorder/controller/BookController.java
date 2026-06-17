package com.bookorder.controller;

import com.bookorder.annotation.OpLog;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bookorder.common.Result;
import com.bookorder.entity.Book;
import com.bookorder.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books")
public class BookController {

    @Autowired
    private BookService bookService;

    @GetMapping
    public Result<IPage<Book>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        return Result.success(bookService.pageBooks(pageNum, pageSize, keyword, categoryId));
    }

    @GetMapping("/{id}")
    public Result<Book> getById(@PathVariable Long id) {
        return Result.success(bookService.getActiveById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @OpLog(module = "book", operation = "CREATE")
    public Result<Book> create(@RequestBody Book book) {
        bookService.createBook(book);
        return Result.success(book);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @OpLog(module = "book", operation = "UPDATE")
    public Result<Book> update(@PathVariable Long id, @RequestBody Book book) {
        book.setId(id);
        bookService.updateBook(book);
        return Result.success(book);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @OpLog(module = "book", operation = "DELETE")
    public Result<Void> delete(@PathVariable Long id) {
        bookService.deleteBook(id);
        return Result.success();
    }
}
