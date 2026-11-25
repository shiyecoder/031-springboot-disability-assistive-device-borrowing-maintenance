# -*- coding: utf-8 -*-
"""生成 NEW-031 UI 原型（可点击、内置假数据）。"""
import os, json

BASE = os.path.dirname(os.path.abspath(__file__))
PAGES = os.path.join(BASE, 'pages')
os.makedirs(PAGES, exist_ok=True)

CSS = """
:root{--c1:#173B57;--c2:#287271;--c3:#2F6B8A;--bg:#F4F7F9;--ok:#287D4F;--dg:#B42318;--bd:#DCE3E8;--tx:#1C2B36;--mut:#5F707C}
*{box-sizing:border-box;margin:0;padding:0}
body{font-family:"PingFang SC","Microsoft YaHei",system-ui,sans-serif;background:var(--bg);color:var(--tx);font-size:14px}
a{color:var(--c3);text-decoration:none}
.side{position:fixed;left:0;top:0;bottom:0;width:220px;background:var(--c1);color:#D7E3EC;overflow:auto}
.side .brand{height:56px;display:flex;align-items:center;gap:10px;padding:0 18px;border-bottom:1px solid rgba(255,255,255,.12);font-weight:600;color:#fff;letter-spacing:.5px}
.side .grp{padding:16px 18px 6px;font-size:12px;color:#8FA6B6;letter-spacing:1px}
.side a{display:flex;align-items:center;gap:8px;padding:10px 18px;color:#D7E3EC;font-size:14px;border-left:3px solid transparent}
.side a:hover{background:rgba(255,255,255,.07)}
.side a.on{background:rgba(47,107,138,.45);border-left-color:#7EC8C7;color:#fff;font-weight:600}
.top{position:fixed;left:220px;right:0;top:0;height:56px;background:#fff;border-bottom:1px solid var(--bd);display:flex;align-items:center;justify-content:space-between;padding:0 24px;z-index:5}
.crumb{color:var(--mut);font-size:13px}
.crumb b{color:var(--tx);font-weight:600}
.top .r{display:flex;align-items:center;gap:16px;font-size:13px;color:var(--mut)}
.avatar{width:32px;height:32px;border-radius:50%;background:var(--c3);color:#fff;display:flex;align-items:center;justify-content:center;font-size:12px}
.main{margin-left:220px;padding:24px;margin-top:56px}
h1.page{font-size:20px;font-weight:600;margin-bottom:4px}
.sub{color:var(--mut);font-size:13px;margin-bottom:16px}
.card{background:#fff;border:1px solid var(--bd);border-radius:8px;padding:18px}
.card+.card{margin-top:16px}
.filters{display:flex;flex-wrap:wrap;gap:12px;align-items:flex-end;margin-bottom:16px}
.fld{display:flex;flex-direction:column;gap:6px}
.fld label{font-size:12px;color:var(--mut)}
.fld input,.fld select{height:34px;width:200px;border:1px solid var(--bd);border-radius:6px;padding:0 10px;font-size:13px;background:#fff;color:var(--tx)}
.fld input.wide{width:240px}
.btn{height:34px;padding:0 16px;border-radius:6px;border:1px solid var(--bd);background:#fff;color:var(--tx);cursor:pointer;font-size:13px}
.btn.pri{background:var(--c1);border-color:var(--c1);color:#fff}
.btn.sec{background:var(--c2);border-color:var(--c2);color:#fff}
.btn.dgr{background:#fff;border-color:var(--dg);color:var(--dg)}
.btn.sm{height:28px;padding:0 10px;font-size:12px}
table{width:100%;border-collapse:collapse;font-size:13px}
th{text-align:left;padding:10px 12px;background:#F7FAFB;color:#42535F;font-weight:600;border-bottom:1px solid var(--bd);white-space:nowrap}
td{padding:10px 12px;border-bottom:1px solid #EDF1F4;white-space:nowrap;font-variant-numeric:tabular-nums}
tr:hover td{background:#FAFCFD}
.tag{display:inline-block;padding:1px 8px;border-radius:10px;font-size:12px;line-height:20px}
.tag.g{background:#E6F4EC;color:var(--ok)}.tag.y{background:#FDF2E3;color:#8A5A0B}.tag.b{background:#E7F0F6;color:var(--c3)}
.tag.r{background:#FBE9E7;color:var(--dg)}.tag.n{background:#EEF2F5;color:#5F707C}
.kpis{display:grid;grid-template-columns:repeat(6,1fr);gap:14px;margin-bottom:16px}
.kpi{background:#fff;border:1px solid var(--bd);border-radius:8px;padding:14px 16px}
.kpi .lb{font-size:12px;color:var(--mut)}
.kpi .vl{font-size:24px;font-weight:600;color:var(--c1);margin-top:6px;font-variant-numeric:tabular-nums}
.kpi .dl{font-size:12px;color:var(--mut);margin-top:4px}
.grid2{display:grid;grid-template-columns:1fr 1fr;gap:16px}
.grid3{display:grid;grid-template-columns:repeat(3,1fr);gap:16px}
.chart{height:220px;border:1px dashed var(--bd);border-radius:6px;display:flex;align-items:center;justify-content:center;color:#9AAAB6;font-size:12px;background:#FBFDFE}
.ct{font-size:14px;font-weight:600;margin-bottom:12px;display:flex;justify-content:space-between;align-items:center}
.pager{display:flex;justify-content:flex-end;align-items:center;gap:8px;margin-top:14px;font-size:13px;color:var(--mut)}
.pager .pg{min-width:30px;height:30px;border:1px solid var(--bd);border-radius:6px;display:flex;align-items:center;justify-content:center;background:#fff}
.pager .pg.on{background:var(--c1);color:#fff;border-color:var(--c1)}
.tl{list-style:none}
.tl li{position:relative;padding:0 0 18px 22px;border-left:2px solid #E3EAEF}
.tl li:before{content:"";position:absolute;left:-5px;top:3px;width:8px;height:8px;border-radius:50%;background:var(--c3)}
.tl .t{font-size:12px;color:var(--mut)}
.tl .m{font-size:13px;margin-top:2px}
.dl2{display:grid;grid-template-columns:110px 1fr 110px 1fr;gap:10px 14px;font-size:13px}
.dl2 .k{color:var(--mut)}
.ph{height:110px;background:linear-gradient(135deg,#DCE7EE,#EEF4F8);border-radius:6px;display:flex;align-items:center;justify-content:center;color:#7E93A2;font-size:12px}
.login-wrap{min-height:100vh;display:grid;grid-template-columns:1.1fr .9fr;background:#fff}
.login-l{background:linear-gradient(155deg,#173B57,#287271);color:#fff;padding:56px;display:flex;flex-direction:column;justify-content:center}
.login-l h2{font-size:30px;line-height:1.35;font-weight:600}
.login-l p{margin-top:16px;color:#CFE0E9;line-height:1.9;font-size:14px}
.login-r{display:flex;align-items:center;justify-content:center;background:#F7FAFB}
.login-box{width:380px}
.login-box h3{font-size:22px;margin-bottom:6px}
.login-box .hint{color:var(--mut);font-size:13px;margin-bottom:24px}
.login-box .fld input{width:100%;height:42px;font-size:14px}
.login-box .btn{width:100%;height:44px;margin-top:20px;font-size:15px}
.role-tip{margin-top:18px;font-size:12px;color:var(--mut);line-height:1.9;background:#F1F5F8;padding:10px 12px;border-radius:6px}
"""

NAV = [
    ("工作台", [("/pages/dashboard.html", "辅具服务看板")]),
    ("器具资源", [("/pages/device-list.html", "器具资源目录"), ("/pages/inspection.html", "检测消毒记录"), ("/pages/reservation.html", "库存预留")]),
    ("借用业务", [("/pages/apply-list.html", "借用维护单"), ("/pages/apply-form.html", "新建借用申请"), ("/pages/assessment.html", "适配评估表"),
                ("/pages/approval.html", "借用审批台"), ("/pages/handover.html", "交接管理"), ("/pages/return.html", "归还检查")]),
    ("维护管理", [("/pages/repair.html", "故障维修工单"), ("/pages/lifecycle.html", "适配与维护提醒"), ("/pages/feedback.html", "回访评价")]),
    ("系统", [("/pages/rule-config.html", "规则配置"), ("/pages/user.html", "用户与角色"), ("/pages/profile.html", "个人中心")]),
]


def shell(title, crumb, body, active):
    nav = ""
    for grp, items in NAV:
        nav += f'<div class="grp">{grp}</div>'
        for href, txt in items:
            cls = " class=\"on\"" if href == active else ""
            nav += f'<a href="{href}"{cls}>{txt}</a>'
    return f"""<!DOCTYPE html><html lang="zh-CN"><head><meta charset="UTF-8"><title>{title} · 残障辅助器具借用与维护管理系统</title><link rel="stylesheet" href="style.css"></head>
<body><div class="side"><div class="brand">辅具驿站 · 管理平台</div>{nav}</div>
<div class="top"><div class="crumb">残障辅助器具 / <b>{crumb}</b></div><div class="r"><span>站内消息 3</span><span>帮助</span><div class="avatar">王</div><span>王仓管 · 仓管员</span></div></div>
<div class="main">{body}</div></body></html>"""


def table(headers, rows):
    th = "".join(f"<th>{h}</th>" for h in headers)
    tr = "".join("<tr>" + "".join(f"<td>{c}</td>" for c in r) + "</tr>" for r in rows)
    return f"<table><thead><tr>{th}</tr></thead><tbody>{tr}</tbody></table>"


def pager(total=86):
    return f'<div class="pager"><span>共 {total} 条</span><span class="pg">1</span><span class="pg on">2</span><span class="pg">3</span><span class="pg">…</span><span class="pg">9</span><span>10 条/页</span></div>'


def filters(fields, extra=""):
    h = '<div class="filters">'
    for lb, typ, opts in fields:
        if typ == "select":
            o = "".join(f"<option>{x}</option>" for x in opts)
            h += f'<div class="fld"><label>{lb}</label><select>{o}</select></div>'
        else:
            h += f'<div class="fld"><label>{lb}</label><input placeholder="{opts}"></div>'
    h += f'<button class="btn pri">查询</button><button class="btn">重置</button>{extra}</div>'
    return h


def write(name, html):
    with open(os.path.join(PAGES, name), "w", encoding="utf-8") as f:
        f.write(html)


with open(os.path.join(BASE, "assets", "style.css"), "w", encoding="utf-8") as f:
    f.write(CSS)
with open(os.path.join(PAGES, "style.css"), "w", encoding="utf-8") as f:
    f.write(CSS)

# 1 看板
kpis = [("器具可用率", "78.4%", "可借 61 / 在册 78"), ("在借器具", "23 台", "本周 +4"), ("平均借期", "47.2 天", "较上月 -3.1 天"),
        ("逾期未还", "3 单", "超期 7 天以上 1 单"), ("维护完成率", "91.6%", "本月工单 24 / 完成 22"),
        ("适配退换率", "6.8%", "评估后调整 5 单")]
kpi_html = "".join(f'<div class="kpi"><div class="lb">{a}</div><div class="vl">{b}</div><div class="dl">{c}</div></div>' for a, b, c in kpis)
body = f"""<h1 class="page">辅具服务看板</h1><div class="sub">统计区间 2026-10-01 至 2026-10-31 · 数据每 10 分钟刷新</div>
<div class="kpis">{kpi_html}</div>
<div class="grid2">
<div class="card"><div class="ct">借用趋势（近 30 天）<span class="tag b">折线</span></div><div class="chart">申请量 / 交接量 / 归还量三条折线</div></div>
<div class="card"><div class="ct">器具状态分布<span class="tag b">环图</span></div><div class="chart">可借 / 已预留 / 借出 / 待消毒 / 待维修 / 已报废</div></div>
</div>
<div class="grid3" style="margin-top:16px">
<div class="card"><div class="ct">分类借用 Top5</div><div class="chart">柱状图</div></div>
<div class="card"><div class="ct">维护时效对比</div><div class="chart">横向条形图</div></div>
<div class="card"><div class="ct">适配提醒等级分布</div><div class="chart">堆叠柱状图</div></div>
</div>
<div class="grid2" style="margin-top:16px">
<div class="card"><div class="ct">需求缺口（申请未满足）</div><div class="chart">折线 + 面积</div></div>
<div class="card"><div class="ct">满意度分布</div><div class="chart">雷达图</div></div>
</div>"""
write("dashboard.html", shell("辅具服务看板", "工作台", body, "/pages/dashboard.html"))

# 2 器具资源目录
rows = [
    ["AS-2026-0031", "多功能轮椅", "轮椅", "鱼跃 H062", "承重 100kg", "A 区 3 号架", '<span class="tag g">可借</span>', "2026-09-18 10:22:00", '<a href="#">查看</a> <a href="#">预留</a>'],
    ["AS-2026-0032", "铝合金腋拐", "拐杖", "可孚 C-02", "承重 120kg", "A 区 1 号架", '<span class="tag g">可借</span>', "2026-10-02 09:10:00", '<a href="#">查看</a> <a href="#">预留</a>'],
    ["AS-2026-0033", "四脚助行器", "助行器", "鱼跃 YU710", "承重 100kg", "B 区 2 号架", '<span class="tag b">已预留</span>', "2026-08-27 14:35:00", '<a href="#">查看</a> <a href="#">释放</a>'],
    ["AS-2026-0034", "电动护理床", "护理床", "迈德斯特 M3", "承重 150kg", "C 区 1 号架", '<span class="tag y">借出</span>', "2026-07-11 16:02:00", '<a href="#">查看</a> <a href="#">延期</a>'],
    ["AS-2026-0035", "坐便椅", "坐便椅", "雅德 YD-09", "承重 120kg", "B 区 4 号架", '<span class="tag y">待消毒</span>', "2026-09-05 11:44:00", '<a href="#">查看</a> <a href="#">消毒</a>'],
    ["AS-2026-0036", "移位机", "移位机", "鱼跃 HLY-01", "承重 135kg", "C 区 2 号架", '<span class="tag r">待维修</span>', "2026-06-19 08:20:00", '<a href="#">查看</a> <a href="#">工单</a>'],
    ["AS-2026-0037", "折叠助行架", "助行器", "可孚 KF-07", "承重 100kg", "B 区 1 号架", '<span class="tag n">已报废</span>', "2026-05-30 15:10:00", '<a href="#">查看</a>'],
    ["AS-2026-0038", "高靠背轮椅", "轮椅", "鱼跃 H065", "承重 110kg", "A 区 4 号架", '<span class="tag g">可借</span>', "2026-10-12 09:55:00", '<a href="#">查看</a> <a href="#">预留</a>'],
]
body = f"""<h1 class="page">器具资源目录</h1><div class="sub">在册 78 台 · 可借 61 台 · 检测过期 4 台</div>
<div class="card">{filters([("器具类别", "select", ["全部", "轮椅", "拐杖", "助行器", "护理床", "坐便椅", "移位机"]), ("适用人群", "select", ["全部", "成人", "儿童", "老年人"]), ("存放点", "input", "如 A 区 3 号架"), ("借用状态", "select", ["全部", "可借", "已预留", "借出", "待消毒", "待维修", "已报废"])], '<button class="btn sec">＋ 器具入库</button><button class="btn">导出当前结果</button>')}
{table(["器具编号", "器具名称", "类别", "品牌型号", "规格", "存放点", "状态", "入库时间", "操作"], rows)}{pager(78)}</div>"""
write("device-list.html", shell("器具资源目录", "器具资源 / 器具资源目录", body, "/pages/device-list.html"))

# 3 借用维护单列表
rows = [
    ["BR-2026-0412", "李桂芳", "轮椅", "2026-10-28 09:12:00", "适配评估", "张评估", '<span class="tag y">待评估</span>', '<a href="#">查看</a> <a href="#">评估</a>'],
    ["BR-2026-0411", "陈国强", "护理床", "2026-10-27 16:40:00", "审批出借", "刘管理", '<span class="tag y">待审批</span>', '<a href="#">查看</a> <a href="#">审批</a>'],
    ["BR-2026-0410", "赵敏", "助行器", "2026-10-27 10:05:00", "使用跟踪", "王仓管", '<span class="tag b">借用中</span>', '<a href="#">查看</a> <a href="#">延期</a>'],
    ["BR-2026-0409", "孙丽", "坐便椅", "2026-10-25 14:30:00", "归还检查", "王仓管", '<span class="tag y">待归还检查</span>', '<a href="#">查看</a> <a href="#">检查</a>'],
    ["BR-2026-0408", "周文华", "拐杖", "2026-10-24 11:18:00", "维护入库", "李维修", '<span class="tag g">已完成</span>', '<a href="#">查看</a>'],
    ["BR-2026-0407", "吴秀英", "轮椅", "2026-10-22 08:52:00", "使用跟踪", "王仓管", '<span class="tag r">已退回</span>', '<a href="#">查看</a>'],
    ["BR-2026-0406", "郑建军", "移位机", "2026-10-20 15:26:00", "使用跟踪", "王仓管", '<span class="tag r">已逾期</span>', '<a href="#">查看</a> <a href="#">催还</a>'],
    ["BR-2026-0405", "黄小燕", "助行器", "2026-10-18 09:40:00", "维护入库", "李维修", '<span class="tag g">已完成</span>', '<a href="#">查看</a>'],
]
tabs = '<div style="display:flex;gap:8px;margin-bottom:14px"><button class="btn sm pri">全部 128</button><button class="btn sm">待我处理 6</button><button class="btn sm">处理中 14</button><button class="btn sm">已完成 96</button><button class="btn sm">异常 12</button></div>'
body = f"""<h1 class="page">借用维护单</h1><div class="sub">默认按「待本人处理优先、更新时间倒序」排列</div>
<div class="card">{tabs}{filters([("业务编号", "input", "如 BR-2026-0412"), ("借用状态", "select", ["全部", "待评估", "待审批", "待交接", "借用中", "待归还检查", "已完成", "已退回"]), ("使用人", "input", "姓名或手机号")], '<button class="btn sec">＋ 新建借用申请</button>')}
{table(["业务编号", "使用人", "器具类别", "提交时间", "当前阶段", "责任人", "状态", "操作"], rows)}{pager(128)}</div>"""
write("apply-list.html", shell("借用维护单", "借用业务 / 借用维护单", body, "/pages/apply-list.html"))

# 4 新建申请
body = """<h1 class="page">新建借用申请</h1><div class="sub">带 * 为必填项 · 提交后进入适配评估</div>
<div class="grid2"><div class="card"><div class="ct">使用人信息</div>
<div class="fld" style="margin-bottom:12px"><label>使用人姓名 *</label><input value="李桂芳"></div>
<div class="fld" style="margin-bottom:12px"><label>联系电话 *</label><input value="138****6721"></div>
<div class="fld" style="margin-bottom:12px"><label>身体条件 *</label><select><option>下肢行动不便，可短距离行走</option><option>长期卧床</option><option>上肢力量不足</option></select></div>
<div class="fld"><label>代办人</label><input placeholder="无则留空" value="李强（子）"></div>
</div>
<div class="card"><div class="ct">需求信息</div>
<div class="fld" style="margin-bottom:12px"><label>器具类别 *</label><select><option>轮椅</option><option>护理床</option></select></div>
<div class="fld" style="margin-bottom:12px"><label>预计借用期限 *</label><input value="2026-11-01 至 2026-12-31"></div>
<div class="fld" style="margin-bottom:12px"><label>用途说明 *</label><input value="出院后居家康复代步"></div>
<div class="fld"><label>配送方式</label><select><option>自提</option><option>上门配送</option></select></div>
</div></div>
<div class="card"><div class="ct">需求证明与附件</div><div class="grid3"><div class="ph">点击上传 · 诊断或残疾证明</div><div class="ph">点击上传 · 身份证正面</div><div class="ph">点击上传 · 其他材料</div></div>
<div style="margin-top:14px;color:#5F707C;font-size:12px">支持 JPG/PNG/PDF，单个不超过 10MB；文件保存在服务端 uploads/ 目录</div></div>
<div style="margin-top:16px;display:flex;gap:10px"><button class="btn pri">提交申请</button><button class="btn">保存草稿</button><button class="btn">取消</button></div>"""
write("apply-form.html", shell("新建借用申请", "借用业务 / 新建借用申请", body, "/pages/apply-form.html"))

# 5 适配评估
rows = [
    ["BR-2026-0412", "李桂芳", "下肢行动不便", "轮椅", "建议 鱼跃 H062（高靠背）", '<span class="tag y">待评估</span>', '<a href="#">评估</a>'],
    ["BR-2026-0405", "黄小燕", "上肢力量不足", "助行器", "建议 四脚助行器 YU710", '<span class="tag g">已评估</span>', '<a href="#">查看</a>'],
    ["BR-2026-0401", "刘志远", "长期卧床", "护理床", "建议 电动护理床 M3", '<span class="tag g">已评估</span>', '<a href="#">查看</a>'],
]
body = f"""<h1 class="page">适配评估表</h1><div class="sub">评估员核验需求、适配型号、期限与使用注意事项</div>
<div class="card">{filters([("评估状态", "select", ["全部", "待评估", "已评估", "已退回"]), ("评估员", "select", ["全部", "张评估", "我"]), ("提交时间", "input", "2026-10-01 ~ 2026-10-31")])}
{table(["业务编号", "使用人", "身体条件", "申请类别", "建议型号", "状态", "操作"], rows)}{pager(32)}</div>
<div class="card"><div class="ct">评估表（抽屉预览）</div>
<div class="dl2"><div class="k">业务编号</div><div>BR-2026-0412</div><div class="k">当前状态</div><div><span class="tag y">待评估</span></div>
<div class="k">建议型号</div><div>鱼跃 H062 高靠背轮椅</div><div class="k">建议附件</div><div>防滑坐垫、安全带</div>
<div class="k">借用时长</div><div>60 天</div><div class="k">是否需保证金</div><div>否</div>
<div class="k">使用指导</div><div>每日检查刹车，坡道需陪护</div><div class="k">评估结论</div><div>适配通过</div></div></div>"""
write("assessment.html", shell("适配评估表", "借用业务 / 适配评估表", body, "/pages/assessment.html"))

# 6 审批台
body = """<h1 class="page">借用审批台</h1><div class="sub">顶部为单据摘要，中部为分组信息，底部为审批时间轴</div>
<div class="card"><div style="display:flex;justify-content:space-between;align-items:flex-start">
<div><div style="font-size:18px;font-weight:600">BR-2026-0411 · 陈国强</div><div style="color:#5F707C;font-size:13px;margin-top:6px">护理床借用申请 · 责任人 刘管理 · 创建 2026-10-27 16:40:00 · 更新 2026-10-28 09:02:11</div></div>
<div><span class="tag y">待审批</span></div></div></div>
<div class="grid2" style="margin-top:16px"><div class="card"><div class="ct">申请信息</div>
<div class="dl2"><div class="k">使用人</div><div>陈国强 / 72 岁</div><div class="k">身体条件</div><div>长期卧床</div>
<div class="k">器具类别</div><div>护理床</div><div class="k">借用期限</div><div>2026-11-01 至 2027-01-30</div>
<div class="k">配送方式</div><div>上门配送</div><div class="k">保证金</div><div>不需要</div></div></div>
<div class="card"><div class="ct">评估结论</div>
<div class="dl2"><div class="k">建议型号</div><div>电动护理床 M3</div><div class="k">评估员</div><div>张评估</div>
<div class="k">适配结论</div><div>适配通过</div><div class="k">维护等级</div><div><span class="tag y">B 级 · 30 天巡检</span></div>
<div class="k">库存匹配</div><div>AS-2026-0034（已预留中）</div><div class="k">检测状态</div><div><span class="tag g">合格 · 2026-10-20</span></div></div></div></div>
<div class="card"><div class="ct">流转时间轴</div><ul class="tl">
<li><div class="t">2026-10-27 16:40:00 · 陈国强</div><div class="m">提交借用申请</div></li>
<li><div class="t">2026-10-27 17:12:20 · 张评估</div><div class="m">完成适配评估，建议型号 M3，借用 90 天</div></li>
<li><div class="t">2026-10-28 09:02:11 · 系统</div><div class="m">锁定合格器具 AS-2026-0034，预留至 2026-10-30 23:59</div></li></ul>
<div style="display:flex;gap:10px;margin-top:12px"><button class="btn sec">审批通过</button><button class="btn dgr">退回补充</button><button class="btn">驳回</button></div></div>"""
write("approval.html", shell("借用审批台", "借用业务 / 借用审批台", body, "/pages/approval.html"))

# 7 交接
body = """<h1 class="page">交接管理</h1><div class="sub">记录器具状况、配件、押金线下状态与使用指导确认</div>
<div class="card"><div class="ct">待交接单据</div>
<table><thead><tr><th>业务编号</th><th>器具编号</th><th>使用人</th><th>预留到期</th><th>状态</th><th>操作</th></tr></thead><tbody>
<tr><td>BR-2026-0410</td><td>AS-2026-0033</td><td>赵敏</td><td>2026-10-29 23:59:00</td><td><span class="tag y">待交接</span></td><td><a href="#">办理交接</a></td></tr>
<tr><td>BR-2026-0408</td><td>AS-2026-0032</td><td>周文华</td><td>2026-10-26 23:59:00</td><td><span class="tag g">已交接</span></td><td><a href="#">查看</a></td></tr></tbody></table></div>
<div class="grid2" style="margin-top:16px"><div class="card"><div class="ct">交接检查项</div>
<div style="display:grid;gap:10px;font-size:13px">
<label><input type="checkbox" checked> 功能正常（刹车、轮组、升降）</label>
<label><input type="checkbox" checked> 外观无破损、无锈蚀</label>
<label><input type="checkbox" checked> 配件齐全（说明书、工具、防滑垫）</label>
<label><input type="checkbox" checked> 使用指导已口述并确认</label>
<label><input type="checkbox"> 押金已线下收取（¥200）</label></div></div>
<div class="card"><div class="ct">交接照片</div><div class="grid3"><div class="ph">器具整体照</div><div class="ph">配件与说明书</div><div class="ph">使用人签收</div></div></div></div>"""
write("handover.html", shell("交接管理", "借用业务 / 交接管理", body, "/pages/handover.html"))

# 8 归还检查
body = """<h1 class="page">归还检查</h1><div class="sub">检查功能、外观、配件、清洁与损坏情况，正常器具进入待消毒</div>
<div class="card"><div class="ct">BR-2026-0409 · 孙丽 · 坐便椅 AS-2026-0035</div>
<div class="grid2"><div><div class="dl2" style="grid-template-columns:110px 1fr">
<div class="k">借用开始</div><div>2026-09-01 10:00:00</div><div class="k">应还日期</div><div>2026-10-25 10:00:00</div>
<div class="k">实际归还</div><div>2026-10-25 14:30:00</div><div class="k">逾期</div><div><span class="tag g">未逾期</span></div>
<div class="k">功能检查</div><div><select style="height:32px;border:1px solid #DCE3E8;border-radius:6px"><option>正常</option><option>异常</option></select></div>
<div class="k">外观检查</div><div><select style="height:32px;border:1px solid #DCE3E8;border-radius:6px"><option>轻微磨损</option><option>明显损坏</option><option>完好</option></select></div>
<div class="k">配件</div><div>齐全</div><div class="k">清洁度</div><div><select style="height:32px;border:1px solid #DCE3E8;border-radius:6px"><option>需清洁</option><option>已清洁</option></select></div>
<div class="k">损坏责任</div><div><select style="height:32px;border:1px solid #DCE3E8;border-radius:6px"><option>无损坏</option><option>使用人责任</option><option>自然损耗</option></select></div>
<div class="k">处理结论</div><div><span class="tag y">转待消毒</span></div></div>
<div style="display:flex;gap:10px;margin-top:16px"><button class="btn pri">提交检查结果</button><button class="btn dgr">转维修</button></div></div>
<div><div class="grid3"><div class="ph">归还外观 1</div><div class="ph">归还外观 2</div><div class="ph">配件照</div></div></div></div></div>"""
write("return.html", shell("归还检查", "借用业务 / 归还检查", body, "/pages/return.html"))

# 9 检测消毒
rows = [
    ["JC-2026-0211", "AS-2026-0035", "坐便椅", "功能检查 / 消毒擦拭", "2026-10-25 15:20:00", "王仓管", '<span class="tag g">合格</span>'],
    ["JC-2026-0210", "AS-2026-0036", "移位机", "承重测试 / 液压检查", "2026-10-24 10:05:00", "李维修", '<span class="tag r">不合格</span>'],
    ["JC-2026-0209", "AS-2026-0032", "拐杖", "胶垫磨损 / 消毒喷洒", "2026-10-22 09:30:00", "王仓管", '<span class="tag g">合格</span>'],
    ["JC-2026-0208", "AS-2026-0034", "护理床", "结构紧固 / 电气安全", "2026-10-20 14:12:00", "李维修", '<span class="tag g">合格</span>'],
]
body = f"""<h1 class="page">检测消毒记录</h1><div class="sub">按器具类别配置检查项、消毒方式与周期；过期器具不可预留</div>
<div class="card">{filters([("器具编号", "input", "如 AS-2026-0035"), ("检查结果", "select", ["全部", "合格", "不合格", "待复检"]), ("检测日期", "input", "2026-10-01 ~ 2026-10-31")], '<button class="btn sec">＋ 登记检测</button>')}
{table(["检测单号", "器具编号", "类别", "检查项", "检测时间", "执行人", "结论"], rows)}{pager(54)}</div>
<div class="card"><div class="ct">检测周期配置</div>
{table(["器具类别", "检查项", "消毒方式", "周期（天）", "下次到期"], [["轮椅", "刹车/轮组/坐垫", "含氯消毒液擦拭", "30", "2026-11-19"], ["护理床", "结构/电气/护栏", "紫外线照射 30min", "60", "2026-12-19"], ["助行器", "胶垫/螺丝", "酒精喷洒", "45", "2026-12-04"]])}</div>"""
write("inspection.html", shell("检测消毒记录", "器具资源 / 检测消毒记录", body, "/pages/inspection.html"))

# 10 库存预留
rows = [
    ["RS-2026-0088", "BR-2026-0410", "AS-2026-0033", "四脚助行器", "2026-10-28 10:00:00", "2026-10-29 23:59:00", '<span class="tag y">预留中</span>'],
    ["RS-2026-0087", "BR-2026-0408", "AS-2026-0032", "铝合金腋拐", "2026-10-25 09:00:00", "2026-10-26 23:59:00", '<span class="tag g">已交接</span>'],
    ["RS-2026-0086", "BR-2026-0402", "AS-2026-0038", "高靠背轮椅", "2026-10-20 11:00:00", "2026-10-21 23:59:00", '<span class="tag n">已释放</span>'],
]
body = f"""<h1 class="page">库存预留</h1><div class="sub">审批通过后锁定一台「检测合格且已消毒」器具，超时未领用自动释放</div>
<div class="card">{table(["预留单号", "业务编号", "器具编号", "器具名称", "锁定时间", "到期时间", "状态"], rows)}{pager(26)}</div>
<div class="card"><div class="ct">可预留库存（按类别）</div>
{table(["类别", "在册", "可借", "预留中", "借出", "建议补货"], [["轮椅", "24", "9", "3", "10", "否"], ["护理床", "12", "2", "1", "8", "<span class='tag r'>建议补 2 台</span>"], ["助行器", "18", "14", "2", "2", "否"], ["移位机", "6", "1", "0", "4", "<span class='tag r'>建议补 1 台</span>"]])}</div>"""
write("reservation.html", shell("库存预留", "器具资源 / 库存预留", body, "/pages/reservation.html"))

# 11 故障维修
rows = [
    ["RP-2026-0061", "AS-2026-0036", "移位机", "液压杆渗油，升降卡顿", "更换液压杆", "¥420.00", "2026-10-22", '<span class="tag y">维修中</span>'],
    ["RP-2026-0060", "AS-2026-0031", "轮椅", "右后轮轴承异响", "更换轴承", "¥85.00", "2026-10-19", '<span class="tag g">已完成</span>'],
    ["RP-2026-0059", "AS-2026-0037", "助行架", "横杆焊接处断裂", "不可修复", "¥0.00", "2026-10-15", '<span class="tag n">建议报废</span>'],
]
body = f"""<h1 class="page">故障维修工单</h1><div class="sub">维修员接收工单、维修、更换零件、完工检测并给出报废建议</div>
<div class="card">{filters([("工单号", "input", "如 RP-2026-0061"), ("工单状态", "select", ["全部", "待接单", "维修中", "待检测", "已完成", "已报废"]), ("维修员", "select", ["全部", "李维修", "我"])], '<button class="btn sec">＋ 新建工单</button>')}
{table(["工单号", "器具编号", "类别", "故障描述", "维修措施", "费用", "报修日期", "状态"], rows)}{pager(61)}</div>
<div class="grid2" style="margin-top:16px"><div class="card"><div class="ct">维修费用趋势</div><div class="chart">近 6 个月柱状图</div></div>
<div class="card"><div class="ct">故障类型分布</div><div class="chart">饼图</div></div></div>"""
write("repair.html", shell("故障维修工单", "维护管理 / 故障维修工单", body, "/pages/repair.html"))

# 12 生命周期 / 创新点
body = """<h1 class="page">适配与维护周期联合提醒</h1><div class="sub">BR-2026-0412 · 李桂芳 · 轮椅 · 规则版本 V3（2026-10-01 生效）</div>
<div class="grid3">
<div class="card"><div class="ct">计算结果</div><div style="font-size:34px;font-weight:600;color:#173B57">82.5</div>
<div style="margin-top:6px"><span class="tag g">推荐出借 · 维护等级 B</span></div>
<div style="font-size:12px;color:#5F707C;margin-top:10px">生成时间 2026-10-28 09:12:30 · 计算编号 SC-2026-0188</div>
<div style="margin-top:12px;font-size:13px;line-height:1.9">推荐结论：<b>适配良好，建议 60 天借用期并安排 30 天巡检</b></div></div>
<div class="card"><div class="ct">主要影响因素</div>
<div style="display:grid;gap:10px;font-size:13px">
<div>需求类别匹配 <b>95</b>（权重 25%）<div style="height:6px;background:#EEF2F5;border-radius:3px;margin-top:4px"><div style="width:95%;height:6px;background:#287271;border-radius:3px"></div></div></div>
<div>使用者条件 <b>88</b>（权重 20%）<div style="height:6px;background:#EEF2F5;border-radius:3px;margin-top:4px"><div style="width:88%;height:6px;background:#287271;border-radius:3px"></div></div></div>
<div>器具规格 <b>80</b>（权重 15%）<div style="height:6px;background:#EEF2F5;border-radius:3px;margin-top:4px"><div style="width:80%;height:6px;background:#287271;border-radius:3px"></div></div></div>
<div>累计借用天数 <b>62</b>（权重 15%）<div style="height:6px;background:#EEF2F5;border-radius:3px;margin-top:4px"><div style="width:62%;height:6px;background:#B42318;border-radius:3px"></div></div></div>
<div>历史故障 <b>70</b>（权重 15%）<div style="height:6px;background:#EEF2F5;border-radius:3px;margin-top:4px"><div style="width:70%;height:6px;background:#8A5A0B;border-radius:3px"></div></div></div>
<div>最近维护 <b>90</b>（权重 10%）<div style="height:6px;background:#EEF2F5;border-radius:3px;margin-top:4px"><div style="width:90%;height:6px;background:#287271;border-radius:3px"></div></div></div></div></div>
<div class="card"><div class="ct">未满足条件 / 可改进项</div>
<ul style="font-size:13px;line-height:2;padding-left:18px;color:#42535F">
<li>累计借用天数已达 62 天，超过 60 天阈值</li>
<li>该器具近 6 个月维修 2 次，建议缩短巡检周期至 21 天</li>
<li>未上传近期康复评估表，建议补充以提升适配置信度</li>
<li>可改进：优先分配 2026 年入库的新轮椅</li></ul>
<div style="margin-top:12px"><button class="btn">人工调整结果</button></div></div></div>
<div class="card" style="margin-top:16px"><div class="ct">人工调整记录</div>
<table><thead><tr><th>时间</th><th>操作人</th><th>原结果</th><th>调整后</th><th>原因</th></tr></thead><tbody>
<tr><td>2026-10-28 10:02:11</td><td>刘管理</td><td>82.5 / B</td><td>85.0 / A</td><td>使用人为独居老人，提高巡检等级</td></tr>
<tr><td>2026-10-21 15:40:02</td><td>刘管理</td><td>64.0 / C</td><td>60.0 / C</td><td>器具维修记录未同步，人工下调</td></tr></tbody></table></div>"""
write("lifecycle.html", shell("适配与维护周期联合提醒", "维护管理 / 适配与维护提醒", body, "/pages/lifecycle.html"))

# 13 回访评价
rows = [
    ["FB-2026-0142", "BR-2026-0408", "周文华", "轮椅", "5", "5", "4", "器具好用，配送及时", "2026-10-24 16:20:00"],
    ["FB-2026-0141", "BR-2026-0405", "黄小燕", "助行器", "4", "4", "5", "希望增加夜间上门回收", "2026-10-18 18:05:00"],
    ["FB-2026-0140", "BR-2026-0396", "吴秀英", "坐便椅", "3", "4", "3", "清洁度一般，已现场处理", "2026-10-10 11:12:00"],
]
body = f"""<h1 class="page">回访评价</h1><div class="sub">归还后评价器具适用性、使用便利、服务质量与遗留问题</div>
<div class="card">{filters([("评价区间", "input", "2026-10-01 ~ 2026-10-31"), ("满意度", "select", ["全部", "5 星", "4 星", "3 星及以下"])])}
{table(["评价编号", "业务编号", "使用人", "器具", "适用性", "便利性", "服务", "建议与问题", "评价时间"], rows)}{pager(48)}</div>"""
write("feedback.html", shell("回访评价", "维护管理 / 回访评价", body, "/pages/feedback.html"))

# 14 规则配置
body = """<h1 class="page">规则配置 · 适配度与维护周期联合提醒</h1><div class="sub">权重合计必须等于 100%，修改后记录版本与生效时间</div>
<div class="grid2"><div class="card"><div class="ct">权重配置（当前版本 V3）</div>
<table><thead><tr><th>输入因素</th><th>数据来源</th><th>权重</th><th>缺失处理</th></tr></thead><tbody>
<tr><td>需求类别</td><td>借用申请</td><td><input value="25" style="width:80px;height:30px;border:1px solid #DCE3E8;border-radius:6px;padding:0 8px">%</td><td>不参与计算并提示补录</td></tr>
<tr><td>使用者条件</td><td>使用人档案</td><td><input value="20" style="width:80px;height:30px;border:1px solid #DCE3E8;border-radius:6px;padding:0 8px">%</td><td>不参与计算并提示补录</td></tr>
<tr><td>器具规格</td><td>器具台账</td><td><input value="15" style="width:80px;height:30px;border:1px solid #DCE3E8;border-radius:6px;padding:0 8px">%</td><td>取同类别均值</td></tr>
<tr><td>累计借用天数</td><td>历史单据</td><td><input value="15" style="width:80px;height:30px;border:1px solid #DCE3E8;border-radius:6px;padding:0 8px">%</td><td>按 0 天计</td></tr>
<tr><td>历史故障</td><td>维修工单</td><td><input value="15" style="width:80px;height:30px;border:1px solid #DCE3E8;border-radius:6px;padding:0 8px">%</td><td>按无故障计</td></tr>
<tr><td>最近维护日期</td><td>检测记录</td><td><input value="10" style="width:80px;height:30px;border:1px solid #DCE3E8;border-radius:6px;padding:0 8px">%</td><td>不参与计算并提示补录</td></tr>
</tbody></table>
<div style="margin-top:12px;display:flex;gap:10px;align-items:center"><b>合计 100%</b><button class="btn pri">保存并发布新版本</button><button class="btn">重置</button></div></div>
<div class="card"><div class="ct">版本历史</div>
<table><thead><tr><th>版本</th><th>生效时间</th><th>修改人</th><th>说明</th></tr></thead><tbody>
<tr><td>V3</td><td>2026-10-01 00:00:00</td><td>刘管理</td><td>提高需求类别权重至 25%</td></tr>
<tr><td>V2</td><td>2026-08-01 00:00:00</td><td>刘管理</td><td>新增最近维护日期因素</td></tr>
<tr><td>V1</td><td>2026-06-01 00:00:00</td><td>系统</td><td>初始版本</td></tr></tbody></table>
<div class="ct" style="margin-top:18px">效果统计</div>
<div class="grid2"><div class="kpi"><div class="lb">推荐采纳率</div><div class="vl">87.3%</div></div><div class="kpi"><div class="lb">人工调整率</div><div class="vl">12.7%</div></div></div></div></div>"""
write("rule-config.html", shell("规则配置", "系统 / 规则配置", body, "/pages/rule-config.html"))

# 15 用户
body = f"""<h1 class="page">用户与角色</h1><div class="sub">按角色控制菜单、按钮与数据范围</div>
<div class="card">{filters([("账号/姓名", "input", "如 wangcg"), ("角色", "select", ["全部", "借用人", "评估员", "仓管员", "维修员", "机构管理员"])], '<button class="btn sec">＋ 新增用户</button>')}
{table(["账号", "姓名", "角色", "所属机构", "数据范围", "状态", "最后登录"], [
["admin", "刘管理", "机构管理员", "阳光社区康复驿站", "全部", '<span class="tag g">启用</span>', "2026-10-31 09:12:00"],
["zhangpg", "张评估", "评估员", "阳光社区康复驿站", "本站点", '<span class="tag g">启用</span>', "2026-10-31 08:40:00"],
["wangcg", "王仓管", "仓管员", "阳光社区康复驿站", "本站点", '<span class="tag g">启用</span>', "2026-10-31 07:55:00"],
["liwx", "李维修", "维修员", "设备维修组", "维修相关", '<span class="tag g">启用</span>', "2026-10-30 17:20:00"],
["ligf", "李桂芳", "借用人", "阳光社区", "本人", '<span class="tag g">启用</span>', "2026-10-28 09:05:00"]])}
</div>
<div class="card"><div class="ct">角色权限</div>
{table(["角色", "菜单权限", "按钮权限", "数据范围"], [["机构管理员", "全部（隐藏菜单管理）", "全部", "全机构"], ["评估员", "评估/审批/台账查看", "评估、退回", "本站点"], ["仓管员", "交接/归还/消毒/预留", "交接、归还检查、消毒", "本站点"], ["维修员", "维修工单/器具查看", "接单、完工、报废建议", "维修相关"], ["借用人", "我的借用/延期/报修/评价", "提交、延期、报修", "本人"]])}</div>"""
write("user.html", shell("用户与角色", "系统 / 用户与角色", body, "/pages/user.html"))

# 16 个人中心
body = """<h1 class="page">个人中心</h1><div class="sub">修改资料、密码与头像，头像保存后右上角同步更新</div>
<div class="grid2"><div class="card"><div class="ct">基本资料</div>
<div style="display:flex;gap:18px;align-items:flex-start"><div class="avatar" style="width:72px;height:72px;font-size:22px">王</div>
<div style="flex:1"><div class="fld" style="margin-bottom:12px"><label>姓名</label><input value="王仓管"></div>
<div class="fld" style="margin-bottom:12px"><label>手机号</label><input value="139****2233"></div>
<div class="fld"><label>头像</label><button class="btn">上传新头像</button></div></div></div>
<div style="margin-top:16px"><button class="btn pri">保存资料</button></div></div>
<div class="card"><div class="ct">修改密码</div>
<div class="fld" style="margin-bottom:12px"><label>原密码</label><input type="password" value="......"></div>
<div class="fld" style="margin-bottom:12px"><label>新密码</label><input type="password"></div>
<div class="fld"><label>确认新密码</label><input type="password"></div>
<div style="margin-top:16px"><button class="btn pri">更新密码</button></div>
<div style="margin-top:14px;font-size:12px;color:#5F707C">密码使用 BCrypt 保存，连续 5 次失败锁定 10 分钟</div></div></div>"""
write("profile.html", shell("个人中心", "系统 / 个人中心", body, "/pages/profile.html"))

# Login
login = """<!DOCTYPE html><html lang="zh-CN"><head><meta charset="UTF-8"><title>登录 · 残障辅助器具借用与维护管理系统</title><link rel="stylesheet" href="style.css"></head><body>
<div class="login-wrap"><div class="login-l"><div style="font-size:13px;letter-spacing:3px;color:#9FC3CE">COMMUNITY REHAB STATION</div>
<h2 style="margin-top:18px">让每一件辅具<br>都送到真正需要的人手里</h2>
<p>器具台账 · 检测消毒 · 适配评估 · 借用交接<br>归还检查 · 故障维修 · 全流程可追溯</p>
<div style="margin-top:44px;display:flex;gap:36px;font-size:13px;color:#BCD5DF"><div><div style="font-size:24px;color:#fff;font-weight:600">78</div>在册器具</div><div><div style="font-size:24px;color:#fff;font-weight:600">128</div>累计借用</div><div><div style="font-size:24px;color:#fff;font-weight:600">96.1%</div>归还完好率</div></div></div>
<div class="login-r"><div class="login-box"><h3>登录管理平台</h3><div class="hint">请使用机构分配的账号登录</div>
<div class="fld" style="margin-bottom:14px"><label>账号</label><input value="admin"></div>
<div class="fld" style="margin-bottom:14px"><label>密码</label><input type="password" value="admin123"></div>
<button class="btn pri">登 录</button>
<div class="role-tip">演示账号：admin / admin123（机构管理员）<br>zhangpg / 123456（评估员）· wangcg / 123456（仓管员）<br>liwx / 123456（维修员）· ligf / 123456（借用人）</div></div></div></div></body></html>"""
write("login.html", login)

print("pages:", len(os.listdir(PAGES)))
