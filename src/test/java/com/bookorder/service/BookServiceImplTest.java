package com.bookorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookorder.common.BusinessException;
import com.bookorder.entity.Book;
import com.bookorder.mapper.BookMapper;
import com.bookorder.service.impl.BookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    private BookServiceImpl bookService;

    @Mock
    private BookMapper bookMapper;

    private Book sampleBook;

    @BeforeEach
    void setUp() {
        bookService = new BookServiceImpl();
        // 手动注入 mock 的 mapper 到 ServiceImpl 的 baseMapper 字段
        ReflectionTestUtils.setField(bookService, "baseMapper", bookMapper);

        sampleBook = new Book();
        sampleBook.setId(1L);
        sampleBook.setTitle("百年孤独");
        sampleBook.setAuthor("加西亚·马尔克斯");
        sampleBook.setIsbn("9787544253468");
        sampleBook.setPrice(new BigDecimal("39.50"));
        sampleBook.setStock(50);
        sampleBook.setCategoryId(1L);
        sampleBook.setStatus(1);
        sampleBook.setCreateTime(LocalDateTime.now());
        sampleBook.setUpdateTime(LocalDateTime.now());
    }

    // ========== pageBooks 分页查询 ==========

    @Test
    @DisplayName("分页查询-无条件：返回所有正常图书")
    void pageBooks_noFilter_returnsAllActiveBooks() {
        Page<Book> mockPage = new Page<>(1, 10);
        mockPage.setRecords(List.of(sampleBook));
        mockPage.setTotal(1);

        when(bookMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        IPage<Book> result = bookService.pageBooks(1, 10, null, null);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(1, result.getTotal());
        verify(bookMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("分页查询-关键词搜索：按书名/作者/ISBN模糊匹配")
    void pageBooks_withKeyword_filtersBooks() {
        Page<Book> mockPage = new Page<>(1, 10);
        mockPage.setRecords(List.of(sampleBook));
        mockPage.setTotal(1);

        when(bookMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        IPage<Book> result = bookService.pageBooks(1, 10, "百年", null);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    @DisplayName("分页查询-按分类过滤：只返回指定分类的图书")
    void pageBooks_withCategoryId_filtersByCategory() {
        Page<Book> mockPage = new Page<>(1, 10);
        mockPage.setRecords(List.of(sampleBook));
        mockPage.setTotal(1);

        when(bookMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        IPage<Book> result = bookService.pageBooks(1, 10, null, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getRecords().get(0).getCategoryId());
    }

    @Test
    @DisplayName("分页查询-空结果：返回空列表")
    void pageBooks_emptyResult_returnsEmptyPage() {
        Page<Book> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(List.of());
        emptyPage.setTotal(0);

        when(bookMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(emptyPage);

        IPage<Book> result = bookService.pageBooks(1, 10, "不存在的书", null);

        assertNotNull(result);
        assertEquals(0, result.getRecords().size());
        assertEquals(0, result.getTotal());
    }

    // ========== getActiveById 根据ID获取 ==========

    @Test
    @DisplayName("根据ID获取-正常：返回活跃图书")
    void getActiveById_exists_returnsBook() {
        when(bookMapper.selectById(1L)).thenReturn(sampleBook);

        Book result = bookService.getActiveById(1L);

        assertNotNull(result);
        assertEquals("百年孤独", result.getTitle());
        assertEquals(1, result.getStatus());
    }

    @Test
    @DisplayName("根据ID获取-不存在：抛出 BusinessException 404")
    void getActiveById_notFound_throwsException() {
        when(bookMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookService.getActiveById(999L));

        assertEquals(404, ex.getCode());
        assertEquals("图书不存在", ex.getMessage());
    }

    @Test
    @DisplayName("根据ID获取-已删除(status=0)：抛出 BusinessException 404")
    void getActiveById_deleted_throwsException() {
        Book deletedBook = new Book();
        deletedBook.setId(2L);
        deletedBook.setTitle("已删除的书");
        deletedBook.setStatus(0);

        when(bookMapper.selectById(2L)).thenReturn(deletedBook);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookService.getActiveById(2L));

        assertEquals(404, ex.getCode());
    }

    // ========== createBook 创建图书 ==========

    @Test
    @DisplayName("创建图书：自动设置 status=1 并保存")
    void createBook_setsStatusAndSaves() {
        Book newBook = new Book();
        newBook.setTitle("新书");
        newBook.setAuthor("作者");
        newBook.setPrice(new BigDecimal("29.99"));

        when(bookMapper.insert(any(Book.class))).thenReturn(1);

        bookService.createBook(newBook);

        assertEquals(1, newBook.getStatus());
        verify(bookMapper).insert(newBook);
    }

    // ========== updateBook 更新图书 ==========

    @Test
    @DisplayName("更新图书：先校验存在再更新")
    void updateBook_existingBook_updatesSuccessfully() {
        when(bookMapper.selectById(1L)).thenReturn(sampleBook);
        when(bookMapper.updateById(any(Book.class))).thenReturn(1);

        sampleBook.setTitle("更新后的书名");
        bookService.updateBook(sampleBook);

        verify(bookMapper).selectById(1L);
        verify(bookMapper).updateById(sampleBook);
    }

    @Test
    @DisplayName("更新图书-不存在：抛出 BusinessException")
    void updateBook_notFound_throwsException() {
        Book nonExist = new Book();
        nonExist.setId(999L);

        when(bookMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> bookService.updateBook(nonExist));
        verify(bookMapper, never()).updateById(any());
    }

    // ========== deleteBook 软删除 ==========

    @Test
    @DisplayName("删除图书-软删除：将 status 设为 0")
    void deleteBook_setsStatusToZero() {
        when(bookMapper.selectById(1L)).thenReturn(sampleBook);
        when(bookMapper.updateById(any(Book.class))).thenReturn(1);

        bookService.deleteBook(1L);

        assertEquals(0, sampleBook.getStatus());
        verify(bookMapper).updateById(sampleBook);
    }

    @Test
    @DisplayName("删除图书-不存在：抛出 BusinessException")
    void deleteBook_notFound_throwsException() {
        when(bookMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> bookService.deleteBook(999L));
        verify(bookMapper, never()).updateById(any());
    }
}
