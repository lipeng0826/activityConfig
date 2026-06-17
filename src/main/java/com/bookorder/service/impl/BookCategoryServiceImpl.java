package com.bookorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bookorder.common.BusinessException;
import com.bookorder.entity.BookCategory;
import com.bookorder.mapper.BookCategoryMapper;
import com.bookorder.service.BookCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookCategoryServiceImpl extends ServiceImpl<BookCategoryMapper, BookCategory>
        implements BookCategoryService {

    @Override
    public List<BookCategory> listActive() {
        return list(new LambdaQueryWrapper<BookCategory>()
                .eq(BookCategory::getStatus, 1)
                .orderByAsc(BookCategory::getId));
    }

    @Override
    public BookCategory getActiveById(Long id) {
        BookCategory category = getById(id);
        if (category == null || category.getStatus() != 1) {
            throw new BusinessException(404, "分类不存在");
        }
        return category;
    }

    @Override
    public void createCategory(BookCategory category) {
        category.setStatus(1);
        save(category);
    }

    @Override
    public void updateCategory(BookCategory category) {
        getActiveById(category.getId());
        updateById(category);
    }

    @Override
    public void deleteCategory(Long id) {
        BookCategory category = getActiveById(id);
        category.setStatus(0);
        updateById(category);
    }
}
