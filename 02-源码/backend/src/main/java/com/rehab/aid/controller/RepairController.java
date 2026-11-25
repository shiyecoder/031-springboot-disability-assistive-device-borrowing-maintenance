package com.rehab.aid.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rehab.aid.common.BizException;
import com.rehab.aid.common.CurrentUser;
import com.rehab.aid.common.R;
import com.rehab.aid.dto.PageDTO;
import com.rehab.aid.entity.*;
import com.rehab.aid.mapper.*;
import com.rehab.aid.service.ScoreService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** 维修工单 + 创新点（规则配置、计算、留痕、人工修正、效果统计） */
@RestController
@RequestMapping("/api")
public class RepairController {

    private final RepairOrderMapper repairMapper;
    private final AssistiveDeviceMapper deviceMapper;
    private final AidRuleMapper ruleMapper;
    private final AidScoreMapper scoreMapper;
    private final BorrowApplicationMapper appMapper;
    private final ScoreService scoreService;

    public RepairController(RepairOrderMapper r, AssistiveDeviceMapper d, AidRuleMapper ru,
                            AidScoreMapper s, BorrowApplicationMapper a, ScoreService ss) {
        this.repairMapper = r; this.deviceMapper = d; this.ruleMapper = ru;
        this.scoreMapper = s; this.appMapper = a; this.scoreService = ss;
    }

    // ---------------- 维修工单 ----------------
    @GetMapping("/repair/page")
    public R<Page<RepairOrder>> page(PageDTO q) {
        LambdaQueryWrapper<RepairOrder> w = new LambdaQueryWrapper<>();
        if (q.getKeyword() != null && !q.getKeyword().isBlank()) {
            String k = q.getKeyword().trim();
            w.and(x -> x.like(RepairOrder::getOrderNo, k).or().like(RepairOrder::getDeviceNo, k)
                    .or().like(RepairOrder::getFaultDesc, k));
        }
        if (q.getStatus() != null && !q.getStatus().isBlank() && !"ALL".equals(q.getStatus()))
            w.eq(RepairOrder::getStatus, q.getStatus());
        w.orderByDesc(RepairOrder::getReportTime);
        return R.ok(repairMapper.selectPage(new Page<>(q.getPageNum(), q.getPageSize()), w));
    }

    @PostMapping("/repair")
    public R<Void> create(@RequestBody RepairOrder body) {
        if (body.getDeviceId() == null) throw new BizException("请选择器具");
        AssistiveDevice d = deviceMapper.selectById(body.getDeviceId());
        if (d == null) throw new BizException("器具不存在");
        if (body.getFaultDesc() == null || body.getFaultDesc().trim().length() < 5) throw new BizException("故障描述不少于 5 个字");
        body.setId(null);
        body.setDeviceNo(d.getDeviceNo());
        body.setDeviceName(d.getName());
        body.setOrderNo("RP-" + LocalDate.now().getYear() + "-" + String.format("%04d", 300 + (int) (Math.random() * 599)));
        body.setStatus("PENDING");
        body.setReportTime(LocalDateTime.now());
        body.setCreateTime(LocalDateTime.now());
        body.setRepairerName("李维修");
        repairMapper.insert(body);
        d.setStatus("TO_REPAIR");
        d.setCurrentCondition("故障报修：" + body.getFaultDesc());
        deviceMapper.updateById(d);
        return R.ok();
    }

    /** 工单流转：accept → repairing → check → done / scrap */
    @PostMapping("/repair/{id}/transition")
    public R<Void> transition(@PathVariable Long id, @RequestBody Map<String, String> body) {
        RepairOrder r = repairMapper.selectById(id);
        if (r == null) throw new BizException("工单不存在");
        String action = body.getOrDefault("action", "");
        String remark = body.getOrDefault("remark", "");
        switch (action) {
            case "accept" -> {
                if (!"PENDING".equals(r.getStatus())) throw new BizException("只有待接单的工单可以接单");
                r.setStatus("REPAIRING");
                r.setRepairerName(CurrentUser.get().getRealName());
            }
            case "finish" -> {
                if (!"REPAIRING".equals(r.getStatus())) throw new BizException("只有维修中的工单可以完工");
                if (body.get("repairMeasure") != null) r.setRepairMeasure(body.get("repairMeasure"));
                if (body.get("partsReplaced") != null) r.setPartsReplaced(body.get("partsReplaced"));
                if (body.get("cost") != null && !body.get("cost").isBlank()) r.setCost(new BigDecimal(body.get("cost")));
                r.setStatus("CHECKING");
                r.setFinishTime(LocalDateTime.now());
            }
            case "done" -> {
                if (!"CHECKING".equals(r.getStatus())) throw new BizException("请先提交完工信息");
                r.setStatus("DONE");
                r.setCheckResult(body.getOrDefault("checkResult", "承重与功能复检通过"));
                r.setScrapSuggest(0);
                AssistiveDevice d = deviceMapper.selectById(r.getDeviceId());
                if (d != null) {
                    d.setStatus("TO_DISINFECT");
                    d.setCurrentCondition("维修完成，待消毒");
                    d.setLastMaintainDate(LocalDate.now());
                    d.setMaintainCount((d.getMaintainCount() == null ? 0 : d.getMaintainCount()) + 1);
                    deviceMapper.updateById(d);
                }
            }
            case "scrap" -> {
                if (remark.trim().length() < 5) throw new BizException("报废必须填写不少于 5 个字的原因");
                r.setStatus("SCRAPPED");
                r.setScrapSuggest(1);
                r.setCheckResult("复检不合格，建议报废：" + remark);
                AssistiveDevice d = deviceMapper.selectById(r.getDeviceId());
                if (d != null) { d.setStatus("SCRAPPED"); d.setCurrentCondition("已报废：" + remark); deviceMapper.updateById(d); }
            }
            default -> throw new BizException("不支持的操作");
        }
        repairMapper.updateById(r);
        return R.ok();
    }

    // ---------------- 创新点：规则 ----------------
    @GetMapping("/rule/list")
    public R<List<AidRule>> ruleList() {
        return R.ok(ruleMapper.selectList(new LambdaQueryWrapper<AidRule>().orderByDesc(AidRule::getId)));
    }

    @GetMapping("/rule/current")
    public R<AidRule> ruleCurrent() {
        return R.ok(scoreService.currentRule());
    }

    /** 发布新版本：权重合计必须等于 100%，记录版本与生效时间 */
    @PostMapping("/rule")
    public R<AidRule> publish(@RequestBody AidRule body) {
        if (body.getWeights() == null || body.getWeights().isBlank()) throw new BizException("权重不能为空");
        double sum = 0;
        try {
            Map<String, Object> m = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(body.getWeights(), new com.fasterxml.jackson.core.type.TypeReference<LinkedHashMap<String, Object>>() {});
            for (Object v : m.values()) sum += Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            throw new BizException("权重格式错误，应为 {\"demand\":25,...}");
        }
        if (Math.abs(sum - 100) > 0.01) throw new BizException("权重合计必须等于 100%，当前为 " + sum + "%");
        long cnt = ruleMapper.selectCount(null);
        body.setId(null);
        body.setVersion("V" + (cnt + 1));
        body.setStatus(1);
        body.setEffectTime(LocalDateTime.now());
        body.setCreateTime(LocalDateTime.now());
        body.setCreator(CurrentUser.get().getRealName());
        if (body.getThresholds() == null || body.getThresholds().isBlank())
            body.setThresholds("{\"A\":85,\"B\":70,\"C\":55}");
        // 旧版本置为历史，仅影响新计算（已保存的结果保留原版本号）
        ruleMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AidRule>()
                .eq(AidRule::getStatus, 1).set(AidRule::getStatus, 0));
        ruleMapper.insert(body);
        return R.ok(body);
    }

    @GetMapping("/score/page")
    public R<Page<AidScore>> scorePage(PageDTO q) {
        LambdaQueryWrapper<AidScore> w = new LambdaQueryWrapper<>();
        if (q.getKeyword() != null && !q.getKeyword().isBlank())
            w.and(x -> x.like(AidScore::getAppNo, q.getKeyword().trim()).or().like(AidScore::getScoreNo, q.getKeyword().trim()));
        if (q.getStatus() != null && !q.getStatus().isBlank() && !"ALL".equals(q.getStatus()))
            w.eq(AidScore::getLevel, q.getStatus());
        w.orderByDesc(AidScore::getCalcTime);
        return R.ok(scoreMapper.selectPage(new Page<>(q.getPageNum(), q.getPageSize()), w));
    }

    @GetMapping("/score/{id}")
    public R<AidScore> scoreDetail(@PathVariable Long id) {
        AidScore s = scoreMapper.selectById(id);
        if (s == null) throw new BizException("计算记录不存在");
        return R.ok(s);
    }

    /** 对指定借用单执行一次计算（返回完整明细用于结果解释） */
    @PostMapping("/borrow/{id}/evaluate")
    public R<AidScore> evaluate(@PathVariable Long id) {
        BorrowApplication app = appMapper.selectById(id);
        if (app == null) throw new BizException("借用单不存在");
        return R.ok(scoreService.evaluate(id, CurrentUser.get().getRealName()));
    }

    @GetMapping("/score/by-app/{appId}")
    public R<List<AidScore>> byApp(@PathVariable Long appId) {
        return R.ok(scoreMapper.selectList(new LambdaQueryWrapper<AidScore>()
                .eq(AidScore::getAppId, appId).orderByDesc(AidScore::getId)));
    }

    /** 人工修正：原始结果不得覆盖，必须填写原因 */
    @PostMapping("/score/{id}/adjust")
    public R<Void> adjust(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal v = new BigDecimal(String.valueOf(body.getOrDefault("score", "0")));
        scoreService.adjust(id, v, String.valueOf(body.getOrDefault("reason", "")), CurrentUser.get().getRealName());
        return R.ok();
    }

    @GetMapping("/score/effect")
    public R<Map<String, Object>> effect() {
        return R.ok(scoreService.effect());
    }
}
