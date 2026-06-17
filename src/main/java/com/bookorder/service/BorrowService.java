package com.bookorder.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bookorder.entity.BorrowRecord;

import java.util.List;

public interface BorrowService extends IService<BorrowRecord> {

    /**
     * 借书
     * 检查：available_copies>0、用户无逾期、在借≤5、同一本书未在借
     */
    BorrowRecord borrow(Long userId, Long bookId);

    /**
     * 还书
     * 写 return_time，available_copies+1，标记逾期
     */
    BorrowRecord returnBook(Long userId, Long recordId);

    /**
     * 续借
     * renew_count≤1，due_time+15天
     */
    BorrowRecord renew(Long userId, Long recordId);

    /**
     * 我的借阅记录
     */
    List<BorrowRecord> myBorrows(Long userId);
}
