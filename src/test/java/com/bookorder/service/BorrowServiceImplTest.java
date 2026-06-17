package com.bookorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookorder.common.BusinessException;
import com.bookorder.entity.Book;
import com.bookorder.entity.BorrowRecord;
import com.bookorder.mapper.BookMapper;
import com.bookorder.mapper.BorrowRecordMapper;
import com.bookorder.service.impl.BorrowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowServiceImplTest {

    private BorrowServiceImpl borrowService;

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Mock
    private BookMapper bookMapper;

    private Book sampleBook;
    private BorrowRecord sampleRecord;

    @BeforeEach
    void setUp() {
        borrowService = new BorrowServiceImpl();
        ReflectionTestUtils.setField(borrowService, "baseMapper", borrowRecordMapper);
        ReflectionTestUtils.setField(borrowService, "borrowRecordMapper", borrowRecordMapper);
        ReflectionTestUtils.setField(borrowService, "bookMapper", bookMapper);

        sampleBook = new Book();
        sampleBook.setId(1L);
        sampleBook.setTitle("百年孤独");
        sampleBook.setStatus(1);
        sampleBook.setAvailableCopies(10);
        sampleBook.setStock(50);

        sampleRecord = new BorrowRecord();
        sampleRecord.setId(1L);
        sampleRecord.setUserId(100L);
        sampleRecord.setBookId(1L);
        sampleRecord.setBorrowTime(LocalDateTime.now().minusDays(5));
        sampleRecord.setDueTime(LocalDateTime.now().plusDays(25));
        sampleRecord.setRenewCount(0);
        sampleRecord.setStatus(0);
    }

    // ========== 借书测试 ==========

    @Test
    @DisplayName("正常借书：创建记录并扣减库存")
    void borrow_normal_success() {
        when(bookMapper.selectById(1L)).thenReturn(sampleBook);
        // mock count queries: overdue=0, active=0, duplicate=0
        when(borrowRecordMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L).thenReturn(0L).thenReturn(0L);
        when(borrowRecordMapper.insert(any(BorrowRecord.class))).thenReturn(1);
        when(bookMapper.updateById(any(Book.class))).thenReturn(1);

        BorrowRecord result = borrowService.borrow(100L, 1L);

        assertNotNull(result);
        assertEquals(0, result.getStatus());
        assertEquals(0, result.getRenewCount());
        assertEquals(9, sampleBook.getAvailableCopies());
        verify(borrowRecordMapper).insert(any(BorrowRecord.class));
    }

    @Test
    @DisplayName("借书-无库存：抛出异常")
    void borrow_noStock_throwsException() {
        sampleBook.setAvailableCopies(0);
        when(bookMapper.selectById(1L)).thenReturn(sampleBook);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> borrowService.borrow(100L, 1L));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("库存"));
    }

    @Test
    @DisplayName("借书-有逾期未还：抛出异常")
    void borrow_hasOverdue_throwsException() {
        when(bookMapper.selectById(1L)).thenReturn(sampleBook);
        // overdue count = 1
        when(borrowRecordMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> borrowService.borrow(100L, 1L));
        assertTrue(ex.getMessage().contains("逾期"));
    }

    @Test
    @DisplayName("借书-超上限(5本)：抛出异常")
    void borrow_exceedLimit_throwsException() {
        when(bookMapper.selectById(1L)).thenReturn(sampleBook);
        // overdue=0, active=5
        when(borrowRecordMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L).thenReturn(5L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> borrowService.borrow(100L, 1L));
        assertTrue(ex.getMessage().contains("上限"));
    }

    @Test
    @DisplayName("借书-重复借同一本书：抛出异常")
    void borrow_duplicate_throwsException() {
        when(bookMapper.selectById(1L)).thenReturn(sampleBook);
        // overdue=0, active=1, duplicate=1
        when(borrowRecordMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L).thenReturn(1L).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> borrowService.borrow(100L, 1L));
        assertTrue(ex.getMessage().contains("重复"));
    }

    // ========== 还书测试 ==========

    @Test
    @DisplayName("正常还书：标记已归还并恢复库存")
    void returnBook_normal_success() {
        when(borrowRecordMapper.selectById(1L)).thenReturn(sampleRecord);
        when(borrowRecordMapper.updateById(any(BorrowRecord.class))).thenReturn(1);
        when(bookMapper.selectById(1L)).thenReturn(sampleBook);
        when(bookMapper.updateById(any(Book.class))).thenReturn(1);

        BorrowRecord result = borrowService.returnBook(100L, 1L);

        assertEquals(1, result.getStatus()); // 正常归还
        assertNotNull(result.getReturnTime());
        assertEquals(11, sampleBook.getAvailableCopies());
    }

    @Test
    @DisplayName("逾期还书：标记 status=2")
    void returnBook_overdue_marksStatus2() {
        sampleRecord.setDueTime(LocalDateTime.now().minusDays(1)); // 已逾期
        when(borrowRecordMapper.selectById(1L)).thenReturn(sampleRecord);
        when(borrowRecordMapper.updateById(any(BorrowRecord.class))).thenReturn(1);
        when(bookMapper.selectById(1L)).thenReturn(sampleBook);
        when(bookMapper.updateById(any(Book.class))).thenReturn(1);

        BorrowRecord result = borrowService.returnBook(100L, 1L);

        assertEquals(2, result.getStatus()); // 逾期归还
    }

    @Test
    @DisplayName("还书-已归还：抛出异常")
    void returnBook_alreadyReturned_throwsException() {
        sampleRecord.setStatus(1);
        when(borrowRecordMapper.selectById(1L)).thenReturn(sampleRecord);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> borrowService.returnBook(100L, 1L));
        assertTrue(ex.getMessage().contains("已归还"));
    }

    // ========== 续借测试 ==========

    @Test
    @DisplayName("正常续借：dueTime+15天，renewCount+1")
    void renew_normal_success() {
        LocalDateTime oldDueTime = sampleRecord.getDueTime();
        when(borrowRecordMapper.selectById(1L)).thenReturn(sampleRecord);
        when(borrowRecordMapper.updateById(any(BorrowRecord.class))).thenReturn(1);

        BorrowRecord result = borrowService.renew(100L, 1L);

        assertEquals(1, result.getRenewCount());
        assertEquals(oldDueTime.plusDays(15), result.getDueTime());
    }

    @Test
    @DisplayName("续借-已达上限：抛出异常")
    void renew_exceedLimit_throwsException() {
        sampleRecord.setRenewCount(1);
        when(borrowRecordMapper.selectById(1L)).thenReturn(sampleRecord);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> borrowService.renew(100L, 1L));
        assertTrue(ex.getMessage().contains("续借上限"));
    }
}
