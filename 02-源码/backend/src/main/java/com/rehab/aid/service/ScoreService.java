package com.rehab.aid.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehab.aid.common.BizException;
import com.rehab.aid.entity.*;
import com.rehab.aid.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 创新点：适配度与维护周期联合提醒。
 * 不替用户做不可逆决定，只输出可解释的评分与建议，并完整留痕：
 * 输入快照 → 规则版本 → 各因素得分 → 总分/等级 → 文字解释 → 人工修正记录。
 */
@Service
public class ScoreService {

    private static final Map<String, String> F_LABEL = new LinkedHashMap<>();
    static {
        F_LABEL.put("demand", "需求类别");
        F_LABEL.put("condition", "使用者条件");
        F_LABEL.put("spec", "器具规格");
        F_LABEL.put("days", "累计借用天数");
        F_LABEL.put("fault", "历史故障");
        F_LABEL.put("maintain", "最近维护日期");
    }

    private final AidRuleMapper ruleMapper;
    private final AidScoreMapper scoreMapper;
    private final BorrowApplicationMapper appMapper;
    private final AssistiveDeviceMapper deviceMapper;
    private final RepairOrderMapper repairMapper;
    private final AdaptationAssessmentMapper assessMapper;
    private final ObjectMapper json = new ObjectMapper();

    public ScoreService(AidRuleMapper r, AidScoreMapper s, BorrowApplicationMapper a,
                        AssistiveDeviceMapper d, RepairOrderMapper rp, AdaptationAssessmentMapper am) {
        this.ruleMapper = r; this.scoreMapper = s; this.appMapper = a;
        this.deviceMapper = d; this.repairMapper = rp; this.assessMapper = am;
    }

    public AidRule currentRule() {
        AidRule r = ruleMapper.selectOne(new LambdaQueryWrapper<AidRule>().eq(AidRule::getStatus, 1)
                .orderByDesc(AidRule::getId).last("limit 1"));
        if (r == null) throw new BizException("未配置适配度规则，请先在规则配置页发布版本");
        return r;
    }

    private Map<String, Double> parseWeights(AidRule rule) {
        try {
            Map<String, Double> m = json.readValue(rule.getWeights(), new TypeReference<LinkedHashMap<String, Double>>() {});
            double sum = m.values().stream().mapToDouble(Double::doubleValue).sum();
            if (Math.abs(sum - 100) > 0.01) throw new BizException("权重合计必须等于 100%，当前为 " + sum + "%");
            return m;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("规则权重格式错误");
        }
    }

    private Map<String, Integer> parseThresholds(AidRule rule) {
        try {
            return json.readValue(rule.getThresholds(), new TypeReference<LinkedHashMap<String, Integer>>() {});
        } catch (Exception e) {
            return Map.of("A", 85, "B", 70, "C", 55);
        }
    }

    /**
     * 执行一次计算：先取输入，再按权重算分，最后生成可读解释并落库留痕。
     */
    @Transactional
    public AidScore evaluate(Long appId, String operator) {
        BorrowApplication app = appMapper.selectById(appId);
        if (app == null) throw new BizException("借用单不存在");
        AidRule rule = currentRule();
        Map<String, Double> weights = parseWeights(rule);

        AssistiveDevice device = app.getDeviceId() != null ? deviceMapper.selectById(app.getDeviceId()) : null;
        AdaptationAssessment assess = assessMapper.selectOne(new LambdaQueryWrapper<AdaptationAssessment>()
                .eq(AdaptationAssessment::getAppId, appId).orderByDesc(AdaptationAssessment::getId).last("limit 1"));
        List<RepairOrder> repairs = device == null ? List.of() : repairMapper.selectList(
                new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getDeviceId, device.getId()));

        // ---- 输入快照（缺失项不参与计算并在解释里提示补录） ----
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("demandCategory", app.getDeviceCategory());
        snapshot.put("suggestModel", assess != null ? assess.getSuggestModel() : null);
        snapshot.put("userCondition", app.getUserCondition());
        snapshot.put("deviceSpec", device != null ? device.getSpec() : null);
        snapshot.put("deviceCategory", device != null ? device.getName() : null);
        snapshot.put("borrowDays", app.getBorrowDays() != null ? app.getBorrowDays() : daysBetween(app.getExpectStart(), app.getExpectEnd()));
        snapshot.put("faultCount", repairs.size());
        snapshot.put("lastMaintain", device != null ? String.valueOf(device.getLastMaintainDate()) : null);
        snapshot.put("missing", new ArrayList<String>());

        Map<String, Integer> s = new LinkedHashMap<>();
        List<String> advice = new ArrayList<>();
        List<String> missing = (List<String>) snapshot.get("missing");

        // 1 需求类别：申请类别与评估建议型号是否一致
        int demand;
        if (assess == null || assess.getSuggestModel() == null) {
            missing.add("需求类别（未完成适配评估，建议补录）");
            demand = 60;
        } else if (assess.getSuggestModel().contains(app.getDeviceCategory())) {
            demand = 95;
        } else {
            demand = 72;
            advice.add("评估建议型号与申请类别不完全一致，出借前请与使用人确认");
        }
        s.put("demand", demand);

        // 2 使用者条件：重体力/卧床类需求与护理床、移位机匹配度更高
        int condition = 85;
        String cond = Optional.ofNullable(app.getUserCondition()).orElse("");
        boolean heavy = cond.contains("卧床") || cond.contains("无负重") || cond.contains("偏瘫");
        if (heavy && ("护理床".equals(app.getDeviceCategory()) || "移位机".equals(app.getDeviceCategory()))) condition = 95;
        else if (heavy) { condition = 68; advice.add("使用人卧床或失能程度较高，当前器具类别支撑能力偏弱，建议评估护理床/移位机"); }
        else if (cond.contains("高龄") || cond.contains("平衡")) { condition = 78; advice.add("使用者平衡能力下降，建议优先选择带护栏或四脚支撑型号"); }
        s.put("condition", condition);

        // 3 器具规格：载重与适用人群
        int spec = 80;
        if (device != null) {
            if (device.getLoadCapacity() != null && device.getLoadCapacity().contains("1")) spec = 90;
            if (heavy && device.getLoadCapacity() != null && device.getLoadCapacity().contains("15")) spec = 96;
            if (device.getMaintainCount() != null && device.getMaintainCount() >= 4) {
                spec -= 10;
                advice.add("该器具累计维护 " + device.getMaintainCount() + " 次，规格可靠性下降，建议优先分配新器具");
            }
        } else {
            spec = 70;
        }
        s.put("spec", Math.max(40, spec));

        // 4 累计借用天数：越接近上限分越低
        int days = app.getBorrowDays() != null ? app.getBorrowDays() : daysBetween(app.getExpectStart(), app.getExpectEnd());
        int dayScore;
        if (days <= 30) dayScore = 95;
        else if (days <= 60) dayScore = 78;
        else if (days <= 90) dayScore = 62;
        else { dayScore = 45; advice.add("借用期限 " + days + " 天偏长，建议拆分为两次借用并中途复检"); }
        s.put("days", dayScore);

        // 5 历史故障
        int fault = Math.max(40, 100 - repairs.size() * 15);
        if (repairs.size() >= 2) advice.add("该器具近 6 个月维修 " + repairs.size() + " 次，建议缩短巡检周期");
        s.put("fault", fault);

        // 6 最近维护日期
        int maintain;
        if (device == null || device.getLastMaintainDate() == null) {
            missing.add("最近维护日期（器具未登记维护记录，建议补录）");
            maintain = 60;
        } else {
            long since = ChronoUnit.DAYS.between(device.getLastMaintainDate(), LocalDate.now());
            if (since <= 30) maintain = 95;
            else if (since <= 90) maintain = 80;
            else if (since <= 180) maintain = 62;
            else { maintain = 40; advice.add("距离上次维护已 " + since + " 天，出借前必须完成检测与消毒"); }
        }
        s.put("maintain", maintain);

        // ---- 加权总分 ----
        double total = 0;
        for (String k : s.keySet()) total += s.get(k) * weights.getOrDefault(k, 0.0) / 100.0;
        BigDecimal score = BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP);

        Map<String, Integer> th = parseThresholds(rule);
        String level = total >= th.getOrDefault("A", 85) ? "A" : total >= th.getOrDefault("B", 70) ? "B"
                : total >= th.getOrDefault("C", 55) ? "C" : "D";
        String maintainLevel = switch (level) {
            case "A" -> "A 级 · 60 天巡检";
            case "B" -> "B 级 · 30 天巡检";
            case "C" -> "C 级 · 15 天巡检";
            default -> "D 级 · 7 天巡检";
        };
        String suggest = switch (level) {
            case "A" -> "适配良好，可按申请期限出借并安排 60 天巡检";
            case "B" -> "基本适配，建议 30 天巡检并关注使用反馈";
            case "C" -> "存在适配风险，建议缩短借用期至 30 天内并 15 天巡检";
            default -> "不推荐直接出借，需重新评估型号或改用其他器具";
        };
        if (!missing.isEmpty()) advice.add("以下因素数据缺失，未参与本次计算：" + String.join("、", missing));

        AidScore sc = new AidScore();
        sc.setScoreNo("SC-" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + (int) (Math.random() * 900 + 100));
        sc.setAppId(appId);
        sc.setAppNo(app.getAppNo());
        sc.setRuleVersion(rule.getVersion());
        sc.setInputSnapshot(writeJson(snapshot));
        sc.setDetailJson(writeJson(s));
        sc.setTotalScore(score);
        sc.setLevel(level);
        sc.setMaintainLevel(maintainLevel);
        sc.setSuggestText(suggest);
        sc.setAdviceJson(writeJson(advice));
        sc.setCalcTime(LocalDateTime.now());
        scoreMapper.insert(sc);
        return sc;
    }

    private int daysBetween(LocalDate a, LocalDate b) {
        if (a == null || b == null) return 30;
        long d = ChronoUnit.DAYS.between(a, b);
        return (int) Math.max(1, d);
    }

    private String writeJson(Object o) {
        try { return json.writeValueAsString(o); } catch (Exception e) { return "{}"; }
    }

    /** 人工修正：原始计算结果保留不变，只写 manual_* 字段并要求填写原因 */
    @Transactional
    public void adjust(Long scoreId, BigDecimal newScore, String reason, String operator) {
        if (reason == null || reason.trim().length() < 5) throw new BizException("人工修正必须填写不少于 5 个字的原因");
        AidScore sc = scoreMapper.selectById(scoreId);
        if (sc == null) throw new BizException("计算记录不存在");
        if (newScore == null || newScore.doubleValue() < 0 || newScore.doubleValue() > 100)
            throw new BizException("修正后的分数必须在 0-100 之间");
        AidRule rule = currentRule();
        Map<String, Integer> th = parseThresholds(rule);
        double v = newScore.doubleValue();
        String level = v >= th.getOrDefault("A", 85) ? "A" : v >= th.getOrDefault("B", 70) ? "B"
                : v >= th.getOrDefault("C", 55) ? "C" : "D";
        sc.setManualScore(newScore);
        sc.setManualLevel(level);
        sc.setManualReason(reason.trim());
        sc.setManually(operator);
        sc.setManualTime(LocalDateTime.now());
        scoreMapper.updateById(sc);
    }

    /** 看板：推荐采纳率与人工调整率 */
    public Map<String, Object> effect() {
        List<AidScore> all = scoreMapper.selectList(null);
        long total = all.size();
        long adjusted = all.stream().filter(x -> x.getManualScore() != null).count();
        Map<String, Object> m = new HashMap<>();
        m.put("total", total);
        m.put("adjusted", adjusted);
        m.put("adoptRate", total == 0 ? 0 : Math.round((total - adjusted) * 1000.0 / total) / 10.0);
        m.put("adjustRate", total == 0 ? 0 : Math.round(adjusted * 1000.0 / total) / 10.0);
        Map<String, Long> dist = new LinkedHashMap<>();
        for (String lv : List.of("A", "B", "C", "D")) {
            dist.put(lv, all.stream().filter(x -> lv.equals(x.getLevel())).count());
        }
        m.put("dist", dist);
        return m;
    }

    public Map<String, Integer> labelMap() {
        Map<String, Integer> m = new LinkedHashMap<>();
        F_LABEL.forEach((k, v) -> m.put(v, 0));
        return m;
    }
}
