package com.bookorder.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bookorder.entity.Book;

public interface BookService extends IService<Book> {

    /**
     * 分页查询图书（仅 status=1）
     * @param pageNum  页码（从1开始）
     * @param pageSize 每页条数
     * @param keyword  关键词（书名/作者/ISBN），可为 null
     * @param categoryId 分类ID，可为 null
     */
    IPage<Book> pageBooks(int pageNum, int pageSize, String keyword, Long categoryId);

    Book getActiveById(Long id);

    void createBook(Book book);

    void updateBook(Book book);

    void deleteBook(Long id);
}
