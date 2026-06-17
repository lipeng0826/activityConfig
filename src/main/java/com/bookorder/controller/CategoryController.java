package com.bookorder.controller;

import com.bookorder.annotation.OpLog;
import com.bookorder.common.Result;
import com.bookorder.entity.BookCategory;
import com.bookorder.service.BookCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private BookCategoryService categoryService;

    @GetMapping
    public Result<List<BookCategory>> list() {
        return Result.success(categoryService.listActive());
    }

    @GetMapping("/{id}")
    public Result<BookCategory> getById(@PathVariable Long id) {
        return Result.success(categoryService.getActiveById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @OpLog(module = "category", operation = "CREATE")
    public Result<BookCategory> create(@RequestBody BookCategory category) {
        categoryService.createCategory(category);
        return Result.success(category);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @OpLog(module = "category", operation = "UPDATE")
    public Result<BookCategory> update(@PathVariable Long id, @RequestBody BookCategory category) {
        category.setId(id);
        categoryService.updateCategory(category);
        return Result.success(category);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @OpLog(module = "category", operation = "DELETE")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }
}
