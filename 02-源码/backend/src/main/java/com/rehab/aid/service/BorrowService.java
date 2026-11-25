package com.rehab.aid.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rehab.aid.common.BizException;
import com.rehab.aid.common.CurrentUser;
import com.rehab.aid.dto.PageDTO;
import com.rehab.aid.entity.*;
import com.rehab.aid.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 借用维护单状态机与关联单据编排。
 * 主流程：需求申请 → 适配评估 → 审批出借 → 使用跟踪 → 归还检查 → 维护入库。
 * 所有状态变更都由后端校验，前端按钮只做展示。
 */
@Service
public class BorrowService {

    private final BorrowApplicationMapper appMapper;
    private final AdaptationAssessmentMapper assessMapper;
    private final DeviceReservationMapper reserveMapper;
    private final HandoverRecordMapper handoverMapper;
    private final ReturnInspectionMapper returnMapper;
    private final RepairOrderMapper repairMapper;
    private final BorrowExtensionMapper extMapper;
    private final DeviceFeedbackMapper fbMapper;
    private final AssistiveDeviceMapper deviceMapper;
    private final DeviceInspectionMapper inspectionMapper;
    private final AidScoreMapper scoreMapper;
    private final SysLogMapper logMapper;
    private final SysMessageMapper msgMapper;

    public BorrowService(BorrowApplicationMapper a, AdaptationAssessmentMapper as, DeviceReservationMapper r,
                         HandoverRecordMapper h, ReturnInspectionMapper rt, RepairOrderMapper rp,
                         BorrowExtensionMapper ex, DeviceFeedbackMapper fb, AssistiveDeviceMapper d,
                         DeviceInspectionMapper di, AidScoreMapper sc, SysLogMapper lg, SysMessageMapper ms) {
        this.appMapper = a; this.assessMapper = as; this.reserveMapper = r; this.handoverMapper = h;
        this.returnMapper = rt; this.repairMapper = rp; this.extMapper = ex; this.fbMapper = fb;
        this.deviceMapper = d; this.inspectionMapper = di; this.scoreMapper = sc;
        this.logMapper = lg; this.msgMapper = ms;
    }

    public Page<BorrowApplication> page(PageDTO q) {
        LambdaQueryWrapper<BorrowApplication> w = new LambdaQueryWrapper<>();
        if (q.getKeyword() != null && !q.getKeyword().isBlank()) {
            String k = q.getKeyword().trim();
            w.and(x -> x.like(BorrowApplication::getAppNo, k).or().like(BorrowApplication::getApplicantName, k)
                    .or().like(BorrowApplication::getApplicantPhone, k));
        }
        if (q.getStatus() != null && !q.getStatus().isBlank() && !"ALL".equals(q.getStatus())) {
            w.eq(BorrowApplication::getStatus, q.getStatus());
        }
        if (q.getCategory() != null && !q.getCategory().isBlank()) {
            w.eq(BorrowApplication::getDeviceCategory, q.getCategory());
        }
        if (q.getDateFrom() != null && !q.getDateFrom().isBlank()) {
            w.ge(BorrowApplication::getCreateTime, q.getDateFrom() + " 00:00:00");
        }
        if (q.getDateTo() != null && !q.getDateTo().isBlank()) {
            w.le(BorrowApplication::getCreateTime, q.getDateTo() + " 23:59:59");
        }
        // 待本人处理优先 + 更新时间倒序
        w.last("ORDER BY FIELD(status,'PENDING_ASSESS','PENDING_APPROVE','PENDING_HANDOVER','PENDING_RETURN','IN_USE','COMPLETED','RETURNED','REJECTED','CANCELLED') ASC, update_time DESC");
        return appMapper.selectPage(new Page<>(q.getPageNum(), q.getPageSize()), w);
    }

    @Transactional
    public BorrowApplication create(BorrowApplication body) {
        if (body.getApplicantName() == null || body.getApplicantName().isBlank()) throw new BizException("使用人姓名必填");
        if (body.getDeviceCategory() == null || body.getDeviceCategory().isBlank()) throw new BizException("器具类别必填");
        if (body.getApplicantPhone() != null && body.getApplicantPhone().length() > 20) throw new BizException("联系电话过长");
        if (body.getExpectStart() != null && body.getExpectEnd() != null && body.getExpectEnd().isBefore(body.getExpectStart()))
            throw new BizException("期望结束日期不能早于开始日期");
        if (body.getProofUrl() == null || body.getProofUrl().isBlank()) throw new BizException("请上传需求证明（通过上传组件）");
        body.setId(null);
        body.setAppNo("BR-" + LocalDate.now().getYear() + "-" + String.format("%04d", 400 + (int) (Math.random() * 599)));
        body.setStatus("PENDING_ASSESS");
        body.setCurrentStage("需求申请");
        body.setOwnerName("张评估");
        body.setOverdue(0);
        body.setVersion(1);
        body.setCreateBy(CurrentUser.id());
        body.setCreateByName(CurrentUser.get().getRealName());
        body.setCreateTime(LocalDateTime.now());
        body.setUpdateTime(LocalDateTime.now());
        if (body.getBorrowDays() == null && body.getExpectStart() != null && body.getExpectEnd() != null) {
            body.setBorrowDays((int) java.time.temporal.ChronoUnit.DAYS.between(body.getExpectStart(), body.getExpectEnd()));
        }
        appMapper.insert(body);
        log(CurrentUser.get().getRealName(), "创建借用申请", body.getAppNo(), body.getApplicantName() + " · " + body.getDeviceCategory());
        msg("新的借用申请待评估", body.getAppNo() + " · " + body.getApplicantName(), body.getAppNo(), "/borrow/detail/" + body.getId(), "ASSESSOR");
        return body;
    }

    /** 适配评估：评估员填写建议型号、期限与注意事项后提交到审批 */
    @Transactional
    public void submitAssessment(Long appId, AdaptationAssessment body) {
        BorrowApplication app = mustGet(appId);
        if (!"PENDING_ASSESS".equals(app.getStatus())) throw new BizException("当前状态不允许提交评估，请刷新页面");
        if (body.getSuggestModel() == null || body.getSuggestModel().isBlank()) throw new BizException("建议型号必填");
        body.setId(null);
        body.setAppId(appId);
        body.setAppNo(app.getAppNo());
        body.setAssessorName(CurrentUser.get().getRealName());
        body.setAssessTime(LocalDateTime.now());
        assessMapper.insert(body);
        if (body.getBorrowDays() != null) app.setBorrowDays(body.getBorrowDays());
        app.setAssessorName(CurrentUser.get().getRealName());
        app.setCurrentStage("适配评估");
        app.setStatus("PENDING_APPROVE");
        app.setOwnerName("刘管理");
        app.setUpdateTime(LocalDateTime.now());
        appMapper.updateById(app);
        log(CurrentUser.get().getRealName(), "提交适配评估", app.getAppNo(), "建议型号 " + body.getSuggestModel());
        msg("借用申请待审批", app.getAppNo() + " · " + app.getApplicantName(), app.getAppNo(), "/borrow/detail/" + appId, "ADMIN");
    }

    /**
     * 统一状态流转入口。
     * action: approve / reject / handover / return / confirm_return / cancel / return_back
     */
    @Transactional
    public BorrowApplication transition(Long appId, String action, String reason) {
        BorrowApplication app = mustGet(appId);
        String st = app.getStatus();
        if (Set.of("reject", "return_back", "cancel").contains(action)) {
            if (reason == null || reason.trim().length() < 5) throw new BizException("请填写不少于 5 个字的原因");
        }
        switch (action) {
            case "approve" -> {
                require(st, "PENDING_APPROVE", "审批");
                reserve(app, true);
                app.setStatus("PENDING_HANDOVER");
                app.setCurrentStage("审批出借");
                app.setOwnerName("王仓管");
            }
            case "reject" -> {
                require(st, "PENDING_APPROVE", "驳回");
                release(app.getId(), "审批驳回：" + reason);
                app.setStatus("REJECTED");
                app.setRejectReason(reason.trim());
                app.setCurrentStage("审批出借");
            }
            case "handover" -> {
                require(st, "PENDING_HANDOVER", "交接");
                AssistiveDevice dev = deviceMapper.selectById(app.getDeviceId());
                if (dev == null) throw new BizException("预留器具不存在，请重新审批");
                dev.setStatus("LENT");
                deviceMapper.updateById(dev);
                reserveMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DeviceReservation>()
                        .eq(DeviceReservation::getAppId, appId).eq(DeviceReservation::getStatus, "LOCKED")
                        .set(DeviceReservation::getStatus, "HANDOVER"));
                app.setStatus("IN_USE");
                app.setCurrentStage("使用跟踪");
            }
            case "return" -> {
                require(st, "IN_USE", "发起归还");
                app.setStatus("PENDING_RETURN");
                app.setCurrentStage("归还检查");
                app.setOwnerName("王仓管");
            }
            case "confirm_return" -> {
                require(st, "PENDING_RETURN", "归还确认");
                app.setStatus("COMPLETED");
                app.setCurrentStage("维护入库");
                app.setOwnerName("李维修");
            }
            case "return_back" -> {
                if (st.endsWith("COMPLETED") || "IN_USE".equals(st)) throw new BizException("该状态不允许退回");
                release(appId, "退回：" + reason);
                app.setStatus("RETURNED");
                app.setRejectReason(reason.trim());
                app.setCurrentStage("适配评估");
            }
            case "cancel" -> {
                if (Set.of("COMPLETED", "CANCELLED").contains(st)) throw new BizException("已完成的单据不能取消");
                release(appId, "取消：" + reason);
                app.setStatus("CANCELLED");
                app.setRejectReason(reason.trim());
            }
            default -> throw new BizException("不支持的操作：" + action);
        }
        app.setUpdateTime(LocalDateTime.now());
        app.setVersion(app.getVersion() == null ? 1 : app.getVersion() + 1);
        appMapper.updateById(app);
        log(CurrentUser.get().getRealName(), "状态流转 " + action, app.getAppNo(),
                "→ " + app.getStatus() + (reason != null ? "（" + reason + "）" : ""));
        return app;
    }

    private void require(String current, String expect, String op) {
        if (!expect.equals(current)) {
            throw new BizException("当前状态为「" + current + "」，不允许执行「" + op + "」，请刷新后重试");
        }
    }

    /**
     * 库存预留：锁定一台「检测合格且已消毒」的可借器具。
     * 无合格器具时给出明确缺口提示，不静默通过。
     */
    private void reserve(BorrowApplication app, boolean strict) {
        List<AssistiveDevice> candidates = deviceMapper.selectList(new LambdaQueryWrapper<AssistiveDevice>()
                .eq(AssistiveDevice::getStatus, "AVAILABLE")
                .eq(AssistiveDevice::getName, app.getDeviceCategory())
                .eq(AssistiveDevice::getDeleted, 0)
                .orderByAsc(AssistiveDevice::getMaintainCount));
        AssistiveDevice picked = null;
        for (AssistiveDevice d : candidates) {
            DeviceInspection last = inspectionMapper.selectOne(new LambdaQueryWrapper<DeviceInspection>()
                    .eq(DeviceInspection::getDeviceId, d.getId()).eq(DeviceInspection::getResult, "PASS")
                    .orderByDesc(DeviceInspection::getInspectTime).last("limit 1"));
            // 检测过期（超过 90 天）或从未检测的器具不可预留
            if (last == null || last.getInspectTime().isBefore(LocalDateTime.now().minusDays(90))) continue;
            picked = d;
            break;
        }
        if (picked == null) {
            if (strict) throw new BizException("「" + app.getDeviceCategory() + "」暂无可预留器具：需同时满足可借状态与 90 天内检测合格，请先入库或完成检测消毒");
            return;
        }
        picked.setStatus("RESERVED");
        deviceMapper.updateById(picked);
        DeviceReservation r = new DeviceReservation();
        r.setReserveNo("RS-" + LocalDate.now().getYear() + "-" + String.format("%04d", 900 + (int) (Math.random() * 99)));
        r.setAppId(app.getId());
        r.setAppNo(app.getAppNo());
        r.setDeviceId(picked.getId());
        r.setDeviceNo(picked.getDeviceNo());
        r.setDeviceName(picked.getName());
        r.setLockTime(LocalDateTime.now());
        r.setExpireTime(LocalDateTime.now().plusHours(48));
        r.setStatus("LOCKED");
        r.setCreateTime(LocalDateTime.now());
        reserveMapper.insert(r);
        app.setDeviceId(picked.getId());
        app.setDeviceNo(picked.getDeviceNo());
        msg("器具已预留，请办理交接", app.getAppNo() + " · " + picked.getDeviceNo(), app.getAppNo(), "/borrow/handover", "WAREHOUSE");
    }

    private void release(Long appId, String reason) {
        DeviceReservation r = reserveMapper.selectOne(new LambdaQueryWrapper<DeviceReservation>()
                .eq(DeviceReservation::getAppId, appId).eq(DeviceReservation::getStatus, "LOCKED").last("limit 1"));
        if (r == null) return;
        r.setStatus("RELEASED");
        r.setReleaseReason(reason.length() > 190 ? reason.substring(0, 190) : reason);
        reserveMapper.updateById(r);
        AssistiveDevice d = deviceMapper.selectById(r.getDeviceId());
        if (d != null && "RESERVED".equals(d.getStatus())) {
            d.setStatus("AVAILABLE");
            deviceMapper.updateById(d);
        }
    }

    /** 交接办理 */
    @Transactional
    public void handover(Long appId, HandoverRecord body) {
        BorrowApplication app = mustGet(appId);
        require(app.getStatus(), "PENDING_HANDOVER", "办理交接");
        body.setId(null);
        body.setAppId(appId);
        body.setAppNo(app.getAppNo());
        body.setDeviceId(app.getDeviceId());
        body.setDeviceNo(app.getDeviceNo());
        body.setHandoverNo("HO-" + LocalDate.now().getYear() + "-" + String.format("%04d", 700 + (int) (Math.random() * 199)));
        body.setOperatorName(CurrentUser.get().getRealName());
        body.setHandoverTime(LocalDateTime.now());
        handoverMapper.insert(body);
        transition(appId, "handover", null);
    }

    /** 归还检查：正常 → 待消毒；损坏 → 生成维修工单 */
    @Transactional
    public void returnInspection(Long appId, ReturnInspection body) {
        BorrowApplication app = mustGet(appId);
        require(app.getStatus(), "IN_USE", "归还检查");
        AssistiveDevice dev = deviceMapper.selectById(app.getDeviceId());
        boolean damaged = body.getDamageLevel() != null && !"无损坏".equals(body.getDamageLevel()) && !"NONE".equals(body.getDamageLevel());
        String conclusion = damaged ? "TO_REPAIR" : "TO_DISINFECT";
        body.setId(null);
        body.setAppId(appId);
        body.setAppNo(app.getAppNo());
        body.setDeviceId(app.getDeviceId());
        body.setDeviceNo(app.getDeviceNo());
        body.setReturnNo("RT-" + LocalDate.now().getYear() + "-" + String.format("%04d", 400 + (int) (Math.random() * 199)));
        body.setActualReturnTime(body.getActualReturnTime() == null ? LocalDateTime.now() : body.getActualReturnTime());
        body.setConclusion(conclusion);
        body.setInspector(CurrentUser.get().getRealName());
        body.setInspectTime(LocalDateTime.now());
        returnMapper.insert(body);

        if (dev != null) {
            dev.setStatus(conclusion);
            dev.setCurrentCondition(damaged ? "归还检查发现损坏，待维修" : "归还检查正常，待消毒");
            deviceMapper.updateById(dev);
        }
        if (damaged) {
            RepairOrder rp = new RepairOrder();
            rp.setOrderNo("RP-" + LocalDate.now().getYear() + "-" + String.format("%04d", 100 + (int) (Math.random() * 599)));
            rp.setDeviceId(app.getDeviceId());
            rp.setDeviceNo(app.getDeviceNo());
            rp.setDeviceName(dev != null ? dev.getName() : app.getDeviceCategory());
            rp.setAppId(appId);
            rp.setAppNo(app.getAppNo());
            rp.setFaultType("STRUCTURE");
            rp.setFaultDesc("归还检查发现：" + body.getDamageLevel() + " / 责任：" + body.getDamageDuty());
            rp.setStatus("PENDING");
            rp.setReportTime(LocalDateTime.now());
            rp.setCreateTime(LocalDateTime.now());
            rp.setRepairerName("李维修");
            repairMapper.insert(rp);
            msg("归还发现损坏，已生成维修工单", app.getAppNo() + " · " + rp.getOrderNo(), app.getAppNo(), "/repair/list", "REPAIRER");
        }
        // 归还检查提交即视为进入待归还检查节点，再执行确认归档，避免状态机误判
        app.setStatus("PENDING_RETURN");
        app.setCurrentStage("归还检查");
        app.setUpdateTime(LocalDateTime.now());
        appMapper.updateById(app);
        transition(appId, "confirm_return", null);
        log(CurrentUser.get().getRealName(), "提交归还检查", app.getAppNo(), conclusion);
    }

    /** 延期申请：不得与后续预留冲突 */
    @Transactional
    public void applyExtension(Long appId, BorrowExtension body) {
        BorrowApplication app = mustGet(appId);
        if (!"IN_USE".equals(app.getStatus())) throw new BizException("只有借用中的单据可以申请延期");
        LocalDate oldEnd = app.getExpectEnd();
        if (body.getNewEnd() == null || body.getNewEnd().isBefore(oldEnd)) throw new BizException("新到期日必须晚于当前到期日");
        if (body.getReason() == null || body.getReason().trim().length() < 5) throw new BizException("延期原因不少于 5 个字");
        // 冲突校验：同一器具在延期区间内是否已有其他预留或交接
        List<DeviceReservation> conflicts = reserveMapper.selectList(new LambdaQueryWrapper<DeviceReservation>()
                .eq(DeviceReservation::getDeviceId, app.getDeviceId()).eq(DeviceReservation::getStatus, "LOCKED"));
        boolean busy = conflicts.stream().anyMatch(c -> c.getAppId() != null && !c.getAppId().equals(appId) && c.getExpireTime() != null);
        if (busy) throw new BizException("该器具后续已被其他单据预留，延期待前序单据释放后再申请");
        body.setId(null);
        body.setAppId(appId);
        body.setAppNo(app.getAppNo());
        body.setOldEnd(oldEnd);
        body.setStatus("PENDING");
        body.setApplyTime(LocalDateTime.now());
        body.setExtNo("EX-" + LocalDate.now().getYear() + "-" + String.format("%04d", 200 + (int) (Math.random() * 199)));
        extMapper.insert(body);
        log(CurrentUser.get().getRealName(), "提交延期申请", app.getAppNo(), "至 " + body.getNewEnd());
    }

    @Transactional
    public void approveExtension(Long extId, boolean pass, String remark) {
        BorrowExtension e = extMapper.selectById(extId);
        if (e == null) throw new BizException("延期申请不存在");
        if (!"PENDING".equals(e.getStatus())) throw new BizException("该延期申请已处理");
        e.setStatus(pass ? "APPROVED" : "REJECTED");
        e.setApproveTime(LocalDateTime.now());
        e.setApprover(CurrentUser.get().getRealName());
        e.setApproveRemark(remark);
        extMapper.updateById(e);
        if (pass) {
            BorrowApplication app = mustGet(e.getAppId());
            app.setExpectEnd(e.getNewEnd());
            app.setBorrowDays((int) java.time.temporal.ChronoUnit.DAYS.between(app.getExpectStart(), e.getNewEnd()));
            app.setUpdateTime(LocalDateTime.now());
            appMapper.updateById(app);
        }
    }

    @Transactional
    public void feedback(Long appId, DeviceFeedback body) {
        BorrowApplication app = mustGet(appId);
        if (!"COMPLETED".equals(app.getStatus())) throw new BizException("归还完成后才能评价");
        if (fbMapper.selectCount(new LambdaQueryWrapper<DeviceFeedback>().eq(DeviceFeedback::getAppId, appId)) > 0)
            throw new BizException("该单据已评价，请勿重复提交");
        body.setId(null);
        body.setAppId(appId);
        body.setAppNo(app.getAppNo());
        body.setApplicantName(app.getApplicantName());
        body.setDeviceCategory(app.getDeviceCategory());
        body.setFeedbackNo("FB-" + LocalDate.now().getYear() + "-" + String.format("%04d", 300 + (int) (Math.random() * 199)));
        body.setFeedbackTime(LocalDateTime.now());
        fbMapper.insert(body);
    }

    /** 详情：基础信息 + 关联单据 + 时间轴 */
    public Map<String, Object> detail(Long id) {
        BorrowApplication app = mustGet(id);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("app", app);
        m.put("assessment", assessMapper.selectOne(new LambdaQueryWrapper<AdaptationAssessment>()
                .eq(AdaptationAssessment::getAppId, id).orderByDesc(AdaptationAssessment::getId).last("limit 1")));
        m.put("reservation", reserveMapper.selectOne(new LambdaQueryWrapper<DeviceReservation>()
                .eq(DeviceReservation::getAppId, id).orderByDesc(DeviceReservation::getId).last("limit 1")));
        m.put("handover", handoverMapper.selectOne(new LambdaQueryWrapper<HandoverRecord>()
                .eq(HandoverRecord::getAppId, id).orderByDesc(HandoverRecord::getId).last("limit 1")));
        m.put("returnInspection", returnMapper.selectOne(new LambdaQueryWrapper<ReturnInspection>()
                .eq(ReturnInspection::getAppId, id).orderByDesc(ReturnInspection::getId).last("limit 1")));
        m.put("repairs", repairMapper.selectList(new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getAppId, id)));
        m.put("extensions", extMapper.selectList(new LambdaQueryWrapper<BorrowExtension>()
                .eq(BorrowExtension::getAppId, id).orderByDesc(BorrowExtension::getId)));
        m.put("feedback", fbMapper.selectOne(new LambdaQueryWrapper<DeviceFeedback>()
                .eq(DeviceFeedback::getAppId, id).orderByDesc(DeviceFeedback::getId).last("limit 1")));
        m.put("scores", scoreMapper.selectList(new LambdaQueryWrapper<AidScore>()
                .eq(AidScore::getAppId, id).orderByDesc(AidScore::getId)));
        m.put("timeline", buildTimeline(app));
        return m;
    }

    private List<Map<String, Object>> buildTimeline(BorrowApplication app) {
        List<Map<String, Object>> tl = new ArrayList<>();
        tl.add(ev(app.getCreateTime(), app.getCreateByName(), "提交借用申请"));
        AdaptationAssessment as = assessMapper.selectOne(new LambdaQueryWrapper<AdaptationAssessment>()
                .eq(AdaptationAssessment::getAppId, app.getId()).orderByDesc(AdaptationAssessment::getId).last("limit 1"));
        if (as != null) tl.add(ev(as.getAssessTime(), as.getAssessorName(), "完成适配评估，建议型号 " + as.getSuggestModel()));
        DeviceReservation rs = reserveMapper.selectOne(new LambdaQueryWrapper<DeviceReservation>()
                .eq(DeviceReservation::getAppId, app.getId()).orderByDesc(DeviceReservation::getId).last("limit 1"));
        if (rs != null) tl.add(ev(rs.getLockTime(), "系统", "锁定器具 " + rs.getDeviceNo() + "，预留至 " + rs.getExpireTime()));
        HandoverRecord ho = handoverMapper.selectOne(new LambdaQueryWrapper<HandoverRecord>()
                .eq(HandoverRecord::getAppId, app.getId()).orderByDesc(HandoverRecord::getId).last("limit 1"));
        if (ho != null) tl.add(ev(ho.getHandoverTime(), ho.getOperatorName(), "完成交接，使用指导已确认"));
        ReturnInspection rt = returnMapper.selectOne(new LambdaQueryWrapper<ReturnInspection>()
                .eq(ReturnInspection::getAppId, app.getId()).orderByDesc(ReturnInspection::getId).last("limit 1"));
        if (rt != null) tl.add(ev(rt.getInspectTime(), rt.getInspector(), "归还检查：" + rt.getConclusion()));
        if (app.getRejectReason() != null) tl.add(ev(app.getUpdateTime(), app.getOwnerName(), "退回/驳回：" + app.getRejectReason()));
        tl.sort(Comparator.comparing(x -> String.valueOf(x.get("time"))));
        return tl;
    }

    private Map<String, Object> ev(LocalDateTime t, String who, String text) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("time", t);
        m.put("who", who);
        m.put("text", text);
        return m;
    }

    private BorrowApplication mustGet(Long id) {
        BorrowApplication app = appMapper.selectById(id);
        if (app == null) throw new BizException("借用单不存在或已删除");
        return app;
    }

    private void log(String op, String action, String bizNo, String detail) {
        SysLog l = new SysLog();
        l.setOperator(op);
        l.setAction(action);
        l.setBizNo(bizNo);
        l.setDetail(detail != null && detail.length() > 480 ? detail.substring(0, 480) : detail);
        l.setIp("127.0.0.1");
        l.setCreateTime(LocalDateTime.now());
        logMapper.insert(l);
    }

    private void msg(String title, String content, String bizNo, String link, String role) {
        SysMessage m = new SysMessage();
        m.setTitle(title);
        m.setContent(content);
        m.setBizNo(bizNo);
        m.setLink(link);
        m.setReceiverRole(role);
        m.setIsRead(0);
        m.setCreateTime(LocalDateTime.now());
        msgMapper.insert(m);
    }
}
