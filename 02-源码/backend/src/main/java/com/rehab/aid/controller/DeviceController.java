package com.rehab.aid.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rehab.aid.common.BizException;
import com.rehab.aid.common.CurrentUser;
import com.rehab.aid.common.R;
import com.rehab.aid.dto.PageDTO;
import com.rehab.aid.entity.*;
import com.rehab.aid.mapper.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** 器具台账、检测消毒、库存预留与看板 */
@RestController
@RequestMapping("/api")
public class DeviceController {

    private final AssistiveDeviceMapper deviceMapper;
    private final DeviceInspectionMapper inspectionMapper;
    private final DeviceReservationMapper reserveMapper;
    private final BorrowApplicationMapper appMapper;
    private final RepairOrderMapper repairMapper;
    private final DeviceFeedbackMapper fbMapper;
    private final SysDictMapper dictMapper;
    private final AidScoreMapper scoreMapper;

    public DeviceController(AssistiveDeviceMapper d, DeviceInspectionMapper i, DeviceReservationMapper r,
                            BorrowApplicationMapper a, RepairOrderMapper rp, DeviceFeedbackMapper f,
                            SysDictMapper di, AidScoreMapper s) {
        this.deviceMapper = d; this.inspectionMapper = i; this.reserveMapper = r;
        this.appMapper = a; this.repairMapper = rp; this.fbMapper = f; this.dictMapper = di; this.scoreMapper = s;
    }

    @GetMapping("/device/page")
    public R<Page<AssistiveDevice>> page(PageDTO q) {
        LambdaQueryWrapper<AssistiveDevice> w = new LambdaQueryWrapper<>();
        w.eq(AssistiveDevice::getDeleted, 0);
        if (q.getKeyword() != null && !q.getKeyword().isBlank()) {
            String k = q.getKeyword().trim();
            w.and(x -> x.like(AssistiveDevice::getDeviceNo, k).or().like(AssistiveDevice::getName, k)
                    .or().like(AssistiveDevice::getSpec, k));
        }
        if (q.getCategory() != null && !q.getCategory().isBlank() && !"ALL".equals(q.getCategory()))
            w.eq(AssistiveDevice::getName, q.getCategory());
        if (q.getStatus() != null && !q.getStatus().isBlank() && !"ALL".equals(q.getStatus()))
            w.eq(AssistiveDevice::getStatus, q.getStatus());
        if (q.getLocation() != null && !q.getLocation().isBlank()) w.like(AssistiveDevice::getLocation, q.getLocation());
        if (q.getSuitPeople() != null && !q.getSuitPeople().isBlank() && !"ALL".equals(q.getSuitPeople()))
            w.like(AssistiveDevice::getSuitPeople, q.getSuitPeople());
        w.orderByAsc(AssistiveDevice::getDeviceNo);
        return R.ok(deviceMapper.selectPage(new Page<>(q.getPageNum(), q.getPageSize()), w));
    }

    @GetMapping("/device/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        AssistiveDevice d = deviceMapper.selectById(id);
        if (d == null) throw new BizException("器具不存在");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("device", d);
        m.put("inspections", inspectionMapper.selectList(new LambdaQueryWrapper<DeviceInspection>()
                .eq(DeviceInspection::getDeviceId, id).orderByDesc(DeviceInspection::getInspectTime)));
        m.put("repairs", repairMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getDeviceId, id).orderByDesc(RepairOrder::getReportTime)));
        m.put("reservations", reserveMapper.selectList(new LambdaQueryWrapper<DeviceReservation>()
                .eq(DeviceReservation::getDeviceId, id).orderByDesc(DeviceReservation::getId)));
        return R.ok(m);
    }

    @PostMapping("/device")
    public R<AssistiveDevice> create(@RequestBody AssistiveDevice body) {
        if (body.getDeviceNo() == null || body.getDeviceNo().isBlank()) throw new BizException("器具编号必填");
        if (body.getName() == null || body.getName().isBlank()) throw new BizException("器具名称必填");
        if (body.getSpec() == null || body.getSpec().isBlank()) throw new BizException("规格型号必填");
        if (body.getSuitPeople() == null || body.getSuitPeople().isBlank()) throw new BizException("适用人群必填");
        if (body.getDeviceNo().trim().length() > 50) throw new BizException("器具编号不能超过 50 个字符");
        body.setDeviceNo(body.getDeviceNo().trim());
        if (deviceMapper.selectCount(new LambdaQueryWrapper<AssistiveDevice>()
                .eq(AssistiveDevice::getDeviceNo, body.getDeviceNo())) > 0)
            throw new BizException("器具编号已存在，请更换");
        body.setId(null);
        body.setStatus(body.getStatus() == null ? "AVAILABLE" : body.getStatus());
        body.setMaintainCount(0);
        body.setDeleted(0);
        body.setVersion(1);
        body.setCreateBy(CurrentUser.get().getRealName());
        body.setCreateTime(LocalDateTime.now());
        body.setUpdateTime(LocalDateTime.now());
        if (body.getCurrentCondition() != null && body.getCurrentCondition().length() > 100)
            throw new BizException("当前状况不能超过 100 个字符");
        deviceMapper.insert(body);
        return R.ok(body);
    }

    /** 更新：version 校验并发，避免两人同时编辑互相覆盖 */
    @PutMapping("/device/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody AssistiveDevice body) {
        AssistiveDevice db = deviceMapper.selectById(id);
        if (db == null) throw new BizException("器具不存在");
        if (body.getVersion() != null && !body.getVersion().equals(db.getVersion()))
            throw new BizException("该器具已被他人更新，请刷新后重新编辑");
        body.setId(id);
        body.setDeviceNo(db.getDeviceNo());
        body.setVersion(db.getVersion());
        body.setUpdateTime(LocalDateTime.now());
        deviceMapper.updateById(body);
        return R.ok();
    }

    /** 登记检测消毒：合格时刷新最近维护日期并回写器具状态 */
    @PostMapping("/device/{id}/inspect")
    public R<Void> inspect(@PathVariable Long id, @RequestBody DeviceInspection body) {
        AssistiveDevice d = deviceMapper.selectById(id);
        if (d == null) throw new BizException("器具不存在");
        body.setId(null);
        body.setDeviceId(id);
        body.setDeviceNo(d.getDeviceNo());
        body.setCategory(d.getName());
        body.setInspectionNo("JC-" + LocalDate.now().getYear() + "-" + String.format("%04d", 300 + (int) (Math.random() * 599)));
        body.setInspector(CurrentUser.get().getRealName());
        body.setInspectTime(LocalDateTime.now());
        body.setNextDueDate(LocalDate.now().plusDays(30));
        inspectionMapper.insert(body);

        if ("PASS".equals(body.getResult())) {
            d.setLastMaintainDate(LocalDate.now());
            d.setMaintainCount((d.getMaintainCount() == null ? 0 : d.getMaintainCount()) + 1);
            if ("TO_DISINFECT".equals(d.getStatus())) d.setStatus("AVAILABLE");
            d.setCurrentCondition("检测合格，已消毒");
        } else if ("FAIL".equals(body.getResult())) {
            d.setStatus("TO_REPAIR");
            d.setCurrentCondition("检测不合格，待维修");
            RepairOrder rp = new RepairOrder();
            rp.setOrderNo("RP-" + LocalDate.now().getYear() + "-" + String.format("%04d", 200 + (int) (Math.random() * 399)));
            rp.setDeviceId(id);
            rp.setDeviceNo(d.getDeviceNo());
            rp.setDeviceName(d.getName());
            rp.setFaultType("STRUCTURE");
            rp.setFaultDesc("检测不合格：" + body.getCheckItems());
            rp.setStatus("PENDING");
            rp.setReportTime(LocalDateTime.now());
            rp.setCreateTime(LocalDateTime.now());
            rp.setRepairerName("李维修");
            repairMapper.insert(rp);
        }
        d.setUpdateTime(LocalDateTime.now());
        deviceMapper.updateById(d);
        return R.ok();
    }

    @GetMapping("/inspection/page")
    public R<Page<DeviceInspection>> inspectionPage(PageDTO q) {
        LambdaQueryWrapper<DeviceInspection> w = new LambdaQueryWrapper<>();
        if (q.getKeyword() != null && !q.getKeyword().isBlank())
            w.and(x -> x.like(DeviceInspection::getDeviceNo, q.getKeyword().trim())
                    .or().like(DeviceInspection::getInspectionNo, q.getKeyword().trim()));
        if (q.getStatus() != null && !q.getStatus().isBlank() && !"ALL".equals(q.getStatus()))
            w.eq(DeviceInspection::getResult, q.getStatus());
        if (q.getDateFrom() != null && !q.getDateFrom().isBlank()) w.ge(DeviceInspection::getInspectTime, q.getDateFrom() + " 00:00:00");
        if (q.getDateTo() != null && !q.getDateTo().isBlank()) w.le(DeviceInspection::getInspectTime, q.getDateTo() + " 23:59:59");
        w.orderByDesc(DeviceInspection::getInspectTime);
        return R.ok(inspectionMapper.selectPage(new Page<>(q.getPageNum(), q.getPageSize()), w));
    }

    @GetMapping("/reservation/page")
    public R<Page<DeviceReservation>> reservationPage(PageDTO q) {
        LambdaQueryWrapper<DeviceReservation> w = new LambdaQueryWrapper<>();
        if (q.getKeyword() != null && !q.getKeyword().isBlank())
            w.and(x -> x.like(DeviceReservation::getReserveNo, q.getKeyword().trim())
                    .or().like(DeviceReservation::getAppNo, q.getKeyword().trim()));
        if (q.getStatus() != null && !q.getStatus().isBlank() && !"ALL".equals(q.getStatus()))
            w.eq(DeviceReservation::getStatus, q.getStatus());
        w.orderByDesc(DeviceReservation::getLockTime);
        return R.ok(reserveMapper.selectPage(new Page<>(q.getPageNum(), q.getPageSize()), w));
    }

    /** 手动释放预留（超时未领用由管理员释放） */
    @PostMapping("/reservation/{id}/release")
    public R<Void> release(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        DeviceReservation r = reserveMapper.selectById(id);
        if (r == null) throw new BizException("预留记录不存在");
        if (!"LOCKED".equals(r.getStatus())) throw new BizException("该预留已结束，无需释放");
        r.setStatus("RELEASED");
        r.setReleaseReason(body != null && body.get("reason") != null ? body.get("reason") : "管理员手动释放");
        reserveMapper.updateById(r);
        AssistiveDevice d = deviceMapper.selectById(r.getDeviceId());
        if (d != null) { d.setStatus("AVAILABLE"); deviceMapper.updateById(d); }
        return R.ok();
    }

    @GetMapping("/device/options")
    public R<Map<String, Object>> options() {
        Map<String, Object> m = new LinkedHashMap<>();
        List<AssistiveDevice> all = deviceMapper.selectList(new LambdaQueryWrapper<AssistiveDevice>().eq(AssistiveDevice::getDeleted, 0));
        m.put("categories", all.stream().map(AssistiveDevice::getName).distinct().toList());
        m.put("dicts", dictMapper.selectList(null));
        m.put("available", all.stream().filter(d -> "AVAILABLE".equals(d.getStatus())).toList());
        return R.ok(m);
    }

    @GetMapping("/dict/{type}")
    public R<List<SysDict>> dict(@PathVariable String type) {
        return R.ok(dictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictType, type).orderByAsc(SysDict::getSort)));
    }

    /**
     * 辅具服务看板：6 个核心指标 + 6 个图表数据源。
     * 聚合在服务端一次完成，前端不做全表二次计算。
     */
    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard(@RequestParam(defaultValue = "30") String range) {
        List<AssistiveDevice> devices = deviceMapper.selectList(new LambdaQueryWrapper<AssistiveDevice>().eq(AssistiveDevice::getDeleted, 0));
        List<BorrowApplication> apps = appMapper.selectList(null);
        List<RepairOrder> repairs = repairMapper.selectList(null);
        List<AidScore> scores = scoreMapper.selectList(null);

        long total = devices.size();
        long available = devices.stream().filter(d -> "AVAILABLE".equals(d.getStatus())).count();
        long lent = devices.stream().filter(d -> "LENT".equals(d.getStatus())).count();
        long overdue = apps.stream().filter(a -> "IN_USE".equals(a.getStatus()) && a.getExpectEnd() != null
                && a.getExpectEnd().isBefore(LocalDate.now())).count();
        long pendingAssess = apps.stream().filter(a -> "PENDING_ASSESS".equals(a.getStatus())).count();
        long pendingApprove = apps.stream().filter(a -> "PENDING_APPROVE".equals(a.getStatus())).count();
        long doneRepair = repairs.stream().filter(r -> "DONE".equals(r.getStatus())).count();
        double maintainRate = repairs.isEmpty() ? 100 : Math.round(doneRepair * 1000.0 / repairs.size()) / 10.0;
        double avgDays = apps.stream().filter(a -> a.getBorrowDays() != null)
                .mapToInt(BorrowApplication::getBorrowDays).average().orElse(0);
        long adjusted = scores.stream().filter(s -> s.getManualScore() != null).count();
        double adjustRate = scores.isEmpty() ? 0 : Math.round(adjusted * 1000.0 / scores.size()) / 10.0;

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("availableRate", total == 0 ? 0 : Math.round(available * 1000.0 / total) / 10.0);
        kpi.put("lentCount", lent);
        kpi.put("avgDays", Math.round(avgDays * 10) / 10.0);
        kpi.put("overdueCount", overdue);
        kpi.put("maintainRate", maintainRate);
        kpi.put("adjustRate", adjustRate);
        kpi.put("total", total);
        kpi.put("available", available);
        kpi.put("pendingAssess", pendingAssess);
        kpi.put("pendingApprove", pendingApprove);
        kpi.put("appTotal", apps.size());

        // 图1 近 6 个月借用趋势
        List<String> months = new ArrayList<>();
        List<Long> applyCnt = new ArrayList<>(), doneCnt = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate m0 = now.minusMonths(i).withDayOfMonth(1);
            LocalDate m1 = m0.plusMonths(1);
            months.add(m0.getYear() + "-" + String.format("%02d", m0.getMonthValue()));
            applyCnt.add(apps.stream().filter(a -> a.getCreateTime() != null && !a.getCreateTime().toLocalDate().isBefore(m0)
                    && a.getCreateTime().toLocalDate().isBefore(m1)).count());
            doneCnt.add(apps.stream().filter(a -> "COMPLETED".equals(a.getStatus()) && a.getUpdateTime() != null
                    && !a.getUpdateTime().toLocalDate().isBefore(m0) && a.getUpdateTime().toLocalDate().isBefore(m1)).count());
        }

        // 图2 状态分布
        Map<String, Long> statusDist = new LinkedHashMap<>();
        for (String s : List.of("AVAILABLE", "RESERVED", "LENT", "TO_DISINFECT", "TO_REPAIR", "SCRAPPED")) {
            statusDist.put(s, devices.stream().filter(d -> s.equals(d.getStatus())).count());
        }

        // 图3 分类在册/可借
        Map<String, Long> catTotal = new LinkedHashMap<>(), catAvail = new LinkedHashMap<>();
        for (AssistiveDevice d : devices) {
            catTotal.merge(d.getName(), 1L, Long::sum);
            if ("AVAILABLE".equals(d.getStatus())) catAvail.merge(d.getName(), 1L, Long::sum);
        }

        // 图4 借用单阶段分布
        Map<String, Long> stageDist = new LinkedHashMap<>();
        for (String s : List.of("PENDING_ASSESS", "PENDING_APPROVE", "PENDING_HANDOVER", "IN_USE", "PENDING_RETURN", "COMPLETED", "RETURNED", "REJECTED")) {
            stageDist.put(s, apps.stream().filter(a -> s.equals(a.getStatus())).count());
        }

        // 图5 维修费用按月
        Map<String, Double> costByMonth = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate m0 = now.minusMonths(i).withDayOfMonth(1);
            LocalDate m1 = m0.plusMonths(1);
            String key = m0.getYear() + "-" + String.format("%02d", m0.getMonthValue());
            double c = repairs.stream().filter(r -> r.getReportTime() != null && !r.getReportTime().toLocalDate().isBefore(m0)
                    && r.getReportTime().toLocalDate().isBefore(m1)).mapToDouble(r -> r.getCost() == null ? 0 : r.getCost().doubleValue()).sum();
            costByMonth.put(key, Math.round(c * 100) / 100.0);
        }

        // 图6 适配等级分布 + 满意度
        Map<String, Long> levelDist = new LinkedHashMap<>();
        for (String lv : List.of("A", "B", "C", "D")) {
            levelDist.put(lv, scores.stream().filter(s -> lv.equals(s.getLevel())).count());
        }
        List<DeviceFeedback> fbs = fbMapper.selectList(null);
        double avgFit = fbs.stream().mapToInt(f -> f.getFitScore() == null ? 5 : f.getFitScore()).average().orElse(0);
        double avgEasy = fbs.stream().mapToInt(f -> f.getEasyScore() == null ? 5 : f.getEasyScore()).average().orElse(0);
        double avgSvc = fbs.stream().mapToInt(f -> f.getServiceScore() == null ? 5 : f.getServiceScore()).average().orElse(0);

        Map<String, Object> charts = new LinkedHashMap<>();
        charts.put("trend", Map.of("months", months, "apply", applyCnt, "done", doneCnt));
        charts.put("statusDist", statusDist);
        charts.put("catDist", Map.of("total", catTotal, "available", catAvail));
        charts.put("stageDist", stageDist);
        charts.put("cost", costByMonth);
        charts.put("levelDist", levelDist);
        charts.put("satisfaction", Map.of("fit", Math.round(avgFit * 10) / 10.0,
                "easy", Math.round(avgEasy * 10) / 10.0, "service", Math.round(avgSvc * 10) / 10.0));

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kpi", kpi);
        m.put("charts", charts);
        m.put("range", range);
        return R.ok(m);
    }
}
