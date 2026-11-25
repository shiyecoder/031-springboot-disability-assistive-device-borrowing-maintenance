package com.rehab.aid.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rehab.aid.common.BizException;
import com.rehab.aid.common.CurrentUser;
import com.rehab.aid.common.R;
import com.rehab.aid.dto.PageDTO;
import com.rehab.aid.dto.TransitionDTO;
import com.rehab.aid.entity.*;
import com.rehab.aid.mapper.*;
import com.rehab.aid.service.BorrowService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/** 借用维护单全流程接口 */
@RestController
@RequestMapping("/api/borrow")
public class BorrowController {

    private final BorrowService borrowService;
    private final BorrowApplicationMapper appMapper;
    private final BorrowExtensionMapper extMapper;
    private final DeviceFeedbackMapper fbMapper;
    private final AdaptationAssessmentMapper assessMapper;

    public BorrowController(BorrowService s, BorrowApplicationMapper a, BorrowExtensionMapper e,
                            DeviceFeedbackMapper f, AdaptationAssessmentMapper am) {
        this.borrowService = s; this.appMapper = a; this.extMapper = e; this.fbMapper = f; this.assessMapper = am;
    }

    @GetMapping("/page")
    public R<Page<BorrowApplication>> page(PageDTO q) {
        return R.ok(borrowService.page(q));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(borrowService.detail(id));
    }

    @PostMapping
    public R<BorrowApplication> create(@RequestBody BorrowApplication body) {
        return R.ok(borrowService.create(body));
    }

    /** 状态流转：approve / reject / return / cancel / return_back */
    @PostMapping("/{id}/transition")
    public R<BorrowApplication> transition(@PathVariable Long id, @RequestBody TransitionDTO dto) {
        return R.ok(borrowService.transition(id, dto.getAction(), dto.getReason()));
    }

    @PostMapping("/{id}/assessment")
    public R<Void> assessment(@PathVariable Long id, @RequestBody AdaptationAssessment body) {
        borrowService.submitAssessment(id, body);
        return R.ok();
    }

    @PostMapping("/{id}/handover")
    public R<Void> handover(@PathVariable Long id, @RequestBody HandoverRecord body) {
        borrowService.handover(id, body);
        return R.ok();
    }

    @PostMapping("/{id}/return")
    public R<Void> returnInspection(@PathVariable Long id, @RequestBody ReturnInspection body) {
        borrowService.returnInspection(id, body);
        return R.ok();
    }

    @PostMapping("/{id}/extension")
    public R<Void> extension(@PathVariable Long id, @RequestBody BorrowExtension body) {
        borrowService.applyExtension(id, body);
        return R.ok();
    }

    @GetMapping("/extension/page")
    public R<Page<BorrowExtension>> extensionPage(PageDTO q) {
        LambdaQueryWrapper<BorrowExtension> w = new LambdaQueryWrapper<>();
        if (q.getStatus() != null && !q.getStatus().isBlank() && !"ALL".equals(q.getStatus()))
            w.eq(BorrowExtension::getStatus, q.getStatus());
        if (q.getKeyword() != null && !q.getKeyword().isBlank())
            w.and(x -> x.like(BorrowExtension::getExtNo, q.getKeyword().trim())
                    .or().like(BorrowExtension::getAppNo, q.getKeyword().trim()));
        w.orderByDesc(BorrowExtension::getApplyTime);
        return R.ok(extMapper.selectPage(new Page<>(q.getPageNum(), q.getPageSize()), w));
    }

    @PostMapping("/extension/{id}/approve")
    public R<Void> approveExtension(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean pass = Boolean.TRUE.equals(body.get("pass")) || "true".equals(String.valueOf(body.get("pass")));
        borrowService.approveExtension(id, pass, String.valueOf(body.getOrDefault("remark", "")));
        return R.ok();
    }

    @PostMapping("/{id}/feedback")
    public R<Void> feedback(@PathVariable Long id, @RequestBody DeviceFeedback body) {
        borrowService.feedback(id, body);
        return R.ok();
    }

    @GetMapping("/feedback/page")
    public R<Page<DeviceFeedback>> feedbackPage(PageDTO q) {
        LambdaQueryWrapper<DeviceFeedback> w = new LambdaQueryWrapper<>();
        if (q.getKeyword() != null && !q.getKeyword().isBlank())
            w.and(x -> x.like(DeviceFeedback::getApplicantName, q.getKeyword().trim())
                    .or().like(DeviceFeedback::getAppNo, q.getKeyword().trim()));
        w.orderByDesc(DeviceFeedback::getFeedbackTime);
        return R.ok(fbMapper.selectPage(new Page<>(q.getPageNum(), q.getPageSize()), w));
    }

    @GetMapping("/assessment/page")
    public R<Page<AdaptationAssessment>> assessmentPage(PageDTO q) {
        LambdaQueryWrapper<AdaptationAssessment> w = new LambdaQueryWrapper<>();
        if (q.getKeyword() != null && !q.getKeyword().isBlank())
            w.and(x -> x.like(AdaptationAssessment::getAppNo, q.getKeyword().trim())
                    .or().like(AdaptationAssessment::getAssessorName, q.getKeyword().trim()));
        w.orderByDesc(AdaptationAssessment::getAssessTime);
        return R.ok(assessMapper.selectPage(new Page<>(q.getPageNum(), q.getPageSize()), w));
    }
}
