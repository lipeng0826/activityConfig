package com.bookorder.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bookorder.entity.BookCategory;

import java.util.List;

public interface BookCategoryService extends IService<BookCategory> {

    List<BookCategory> listActive();

    BookCategory getActiveById(Long id);

    void createCategory(BookCategory category);

    void updateCategory(BookCategory category);

    void deleteCategory(Long id);
}
