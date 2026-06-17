package com.bookorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bookorder.common.BusinessException;
import com.bookorder.entity.Book;
import com.bookorder.mapper.BookMapper;
import com.bookorder.service.BookService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    @Override
    public IPage<Book> pageBooks(int pageNum, int pageSize, String keyword, Long categoryId) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<Book>()
                .eq(Book::getStatus, 1);

        if (categoryId != null) {
            wrapper.eq(Book::getCategoryId, categoryId);
        }

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(Book::getTitle, keyword)
                    .or().like(Book::getAuthor, keyword)
                    .or().like(Book::getIsbn, keyword));
        }

        wrapper.orderByDesc(Book::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Book getActiveById(Long id) {
        Book book = getById(id);
        if (book == null || book.getStatus() != 1) {
            throw new BusinessException(404, "图书不存在");
        }
        return book;
    }

    @Override
    public void createBook(Book book) {
        book.setStatus(1);
        save(book);
    }

    @Override
    public void updateBook(Book book) {
        getActiveById(book.getId());
        updateById(book);
    }

    @Override
    public void deleteBook(Long id) {
        Book book = getActiveById(id);
        book.setStatus(0);
        updateById(book);
    }
}
