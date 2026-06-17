package com.bookorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bookorder.common.BusinessException;
import com.bookorder.entity.Book;
import com.bookorder.entity.BorrowRecord;
import com.bookorder.mapper.BookMapper;
import com.bookorder.mapper.BorrowRecordMapper;
import com.bookorder.service.BorrowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BorrowServiceImpl extends ServiceImpl<BorrowRecordMapper, BorrowRecord> implements BorrowService {

    private static final int MAX_ACTIVE_BORROWS = 5;
    private static final int MAX_RENEW_COUNT = 1;
    private static final int RENEW_DAYS = 15;
    private static final int DEFAULT_BORROW_DAYS = 30;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private BookMapper bookMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BorrowRecord borrow(Long userId, Long bookId) {
        // 1. 检查图书是否存在且可用
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getStatus() != 1) {
            throw new BusinessException(404, "图书不存在");
        }

        // 2. 检查库存
        if (book.getAvailableCopies() == null || book.getAvailableCopies() <= 0) {
            throw new BusinessException(400, "该图书暂无可借库存");
        }

        // 3. 检查用户是否有逾期未还
        long overdueCount = count(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getUserId, userId)
                .eq(BorrowRecord::getStatus, 0)
                .lt(BorrowRecord::getDueTime, LocalDateTime.now()));
        if (overdueCount > 0) {
            throw new BusinessException(400, "您有逾期未还的图书，请先归还后再借");
        }

        // 4. 检查在借数量上限
        long activeCount = count(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getUserId, userId)
                .eq(BorrowRecord::getStatus, 0));
        if (activeCount >= MAX_ACTIVE_BORROWS) {
            throw new BusinessException(400, "在借图书已达上限（" + MAX_ACTIVE_BORROWS + "本），请先归还后再借");
        }

        // 5. 检查同一本书是否已在借
        long duplicateCount = count(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getUserId, userId)
                .eq(BorrowRecord::getBookId, bookId)
                .eq(BorrowRecord::getStatus, 0));
        if (duplicateCount > 0) {
            throw new BusinessException(400, "您已借阅该图书，请勿重复借阅");
        }

        // 6. 创建借阅记录
        LocalDateTime now = LocalDateTime.now();
        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setBookId(bookId);
        record.setBorrowTime(now);
        record.setDueTime(now.plusDays(DEFAULT_BORROW_DAYS));
        record.setRenewCount(0);
        record.setStatus(0);
        borrowRecordMapper.insert(record);

        // 7. 扣减库存
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookMapper.updateById(book);

        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BorrowRecord returnBook(Long userId, Long recordId) {
        BorrowRecord record = getById(recordId);
        if (record == null) {
            throw new BusinessException(404, "借阅记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作此借阅记录");
        }
        if (record.getStatus() != 0) {
            throw new BusinessException(400, "该图书已归还，请勿重复操作");
        }

        LocalDateTime now = LocalDateTime.now();
        record.setReturnTime(now);

        // 判断是否逾期
        if (now.isAfter(record.getDueTime())) {
            record.setStatus(2); // 逾期归还
        } else {
            record.setStatus(1); // 正常归还
        }
        borrowRecordMapper.updateById(record);

        // 恢复库存
        Book book = bookMapper.selectById(record.getBookId());
        if (book != null) {
            book.setAvailableCopies((book.getAvailableCopies() == null ? 0 : book.getAvailableCopies()) + 1);
            bookMapper.updateById(book);
        }

        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BorrowRecord renew(Long userId, Long recordId) {
        BorrowRecord record = getById(recordId);
        if (record == null) {
            throw new BusinessException(404, "借阅记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作此借阅记录");
        }
        if (record.getStatus() != 0) {
            throw new BusinessException(400, "该图书已归还，无法续借");
        }
        if (record.getRenewCount() >= MAX_RENEW_COUNT) {
            throw new BusinessException(400, "已达续借上限（" + MAX_RENEW_COUNT + "次），无法继续续借");
        }
        if (LocalDateTime.now().isAfter(record.getDueTime())) {
            throw new BusinessException(400, "图书已逾期，请先归还后再续借");
        }

        record.setDueTime(record.getDueTime().plusDays(RENEW_DAYS));
        record.setRenewCount(record.getRenewCount() + 1);
        borrowRecordMapper.updateById(record);

        return record;
    }

    @Override
    public List<BorrowRecord> myBorrows(Long userId) {
        return list(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getUserId, userId)
                .orderByDesc(BorrowRecord::getCreateTime));
    }
}
