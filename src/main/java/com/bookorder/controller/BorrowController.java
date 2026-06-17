package com.bookorder.controller;

import com.bookorder.annotation.OpLog;
import com.bookorder.common.Result;
import com.bookorder.entity.BorrowRecord;
import com.bookorder.security.SysUserDetails;
import com.bookorder.service.BorrowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/borrows")
public class BorrowController {

    @Autowired
    private BorrowService borrowService;

    @PostMapping
    @OpLog(module = "borrow", operation = "CREATE")
    public Result<BorrowRecord> borrow(
            @AuthenticationPrincipal SysUserDetails userDetails,
            @RequestBody Map<String, Long> body) {
        Long bookId = body.get("bookId");
        return Result.success(borrowService.borrow(userDetails.getId(), bookId));
    }

    @PutMapping("/{id}/return")
    @OpLog(module = "borrow", operation = "RETURN")
    public Result<BorrowRecord> returnBook(
            @AuthenticationPrincipal SysUserDetails userDetails,
            @PathVariable Long id) {
        return Result.success(borrowService.returnBook(userDetails.getId(), id));
    }

    @PutMapping("/{id}/renew")
    @OpLog(module = "borrow", operation = "RENEW")
    public Result<BorrowRecord> renew(
            @AuthenticationPrincipal SysUserDetails userDetails,
            @PathVariable Long id) {
        return Result.success(borrowService.renew(userDetails.getId(), id));
    }

    @GetMapping("/my")
    public Result<List<BorrowRecord>> myBorrows(
            @AuthenticationPrincipal SysUserDetails userDetails) {
        return Result.success(borrowService.myBorrows(userDetails.getId()));
    }
}
