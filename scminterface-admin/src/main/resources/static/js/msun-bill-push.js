/**
 * 众阳 HIS 单据推送页：出库(201) / 退库(401) 分 Tab 测试
 */
const BP_PAGE_SIZE = 20;
var bpProbeInited = false;
var bpStandalonePage = false;

function bpApiBase() {
    if (typeof msunHospitalApi === 'function') {
        return msunHospitalApi() + '/spd/bill-push';
    }
    return '/api/vendor/msun/hospitals/zaoqiang-tcm-001/spd/bill-push';
}

var BP_PUSH_LABELS = { '0': '未推送', '1': '推送中', '2': '成功', '3': '失败' };

/** @type {Record<string, {pageNum:number,total:number,rows:Array,selected:Set}>} */
var bpState = {
    '201': { pageNum: 1, total: 0, rows: [], selected: new Set() },
    '401': { pageNum: 1, total: 0, rows: [], selected: new Set() }
};

function bpCheckLogin() {
    if (!getToken()) {
        window.location.href = '/login.html?redirect=' + encodeURIComponent('/msun-bill-push.html');
        return false;
    }
    return true;
}

function bpTabState(tab) {
    return bpState[tab] || bpState['201'];
}

function bpSetMeta(tab, text) {
    var el = document.getElementById('metaInfo' + tab);
    if (el) el.textContent = text;
}

function bpQueryParams(tab) {
    var p = tab === '401' ? 'q401_' : 'q201_';
    return {
        billNo: document.getElementById(p + 'billNo').value.trim(),
        materialName: document.getElementById(p + 'materialName').value.trim(),
        materialSpeci: document.getElementById(p + 'materialSpeci').value.trim(),
        departmentName: document.getElementById(p + 'departmentName').value.trim(),
        warehouseName: document.getElementById(p + 'warehouseName').value.trim(),
        billType: tab,
        hisPushStatus: document.getElementById(p + 'hisPushStatus').value,
        pageNum: bpTabState(tab).pageNum,
        pageSize: BP_PAGE_SIZE
    };
}

function bpBuildQuery(params) {
    var parts = [];
    Object.keys(params).forEach(function (key) {
        var val = params[key];
        if (val !== null && val !== undefined && String(val).trim() !== '') {
            parts.push(encodeURIComponent(key) + '=' + encodeURIComponent(val));
        }
    });
    return parts.length ? '?' + parts.join('&') : '';
}

function bpPushTag(status) {
    var s = status == null || status === '' ? '0' : String(status);
    var label = BP_PUSH_LABELS[s] || s;
    return '<span class="tag tag-' + s + '">' + label + '</span>';
}

function bpEscape(s) {
    return String(s == null ? '' : s)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function bpRenderTable(tab) {
    var st = bpTabState(tab);
    var tbody = document.getElementById('dataBody' + tab);
    if (!tbody) return;
    if (!st.rows.length) {
        tbody.innerHTML = '<tr><td colspan="' + (tab === '401' ? 11 : 10) + '" style="text-align:center;color:#9ca3af;">无数据</td></tr>';
        return;
    }
    var html = '';
    st.rows.forEach(function (row) {
        var billId = row.bill_id;
        var checked = st.selected.has(String(billId)) ? ' checked' : '';
        html += '<tr>';
        html += '<td><input type="checkbox" data-bill-id="' + billId + '"' + checked + ' onchange="bpOnCheck(\'' + tab + '\', this)"></td>';
        html += '<td>' + bpEscape(row.bill_no) + '</td>';
        html += '<td>' + bpEscape(row.department_name) + '</td>';
        html += '<td>' + bpEscape(row.warehouse_name) + '</td>';
        html += '<td>' + bpEscape(row.material_name) + '</td>';
        html += '<td>' + bpEscape(row.material_speci) + '</td>';
        html += '<td>' + bpEscape(row.qty) + '</td>';
        html += '<td>' + bpPushTag(row.his_push_status) + '</td>';
        html += '<td>' + bpPushTag(row.bill_his_push_status) + '</td>';
        if (tab === '401') {
            html += '<td>' + bpEscape(row.his_pharmacy_stock_id || '—') + '</td>';
        }
        var btnClass = tab === '401' ? 'btn-return' : 'btn-warn';
        var btnLabel = tab === '401' ? '推送退库' : '推送出库';
        html += '<td><button type="button" class="btn ' + btnClass + '" style="padding:4px 8px;font-size:11px;" onclick="bpPushOne(\'' + tab + '\',' + billId + ')">' + btnLabel + '</button></td>';
        html += '</tr>';
    });
    tbody.innerHTML = html;
    var chkAll = document.getElementById('chkAll' + tab);
    if (chkAll) chkAll.checked = false;
}

function bpUpdatePager(tab) {
    var st = bpTabState(tab);
    var pages = Math.max(1, Math.ceil(st.total / BP_PAGE_SIZE));
    var pager = document.getElementById('pagerInfo' + tab);
    if (pager) {
        pager.textContent = '第 ' + st.pageNum + ' / ' + pages + ' 页，共 ' + st.total + ' 条明细';
    }
}

function bpUpdateBatchBtn(tab) {
    var btn = document.getElementById('btnBatchPush' + tab);
    if (btn) btn.disabled = bpTabState(tab).selected.size === 0;
}

function bpOnCheck(tab, el) {
    var st = bpTabState(tab);
    var id = String(el.getAttribute('data-bill-id'));
    if (el.checked) {
        st.selected.add(id);
    } else {
        st.selected.delete(id);
    }
    bpUpdateBatchBtn(tab);
}

function bpToggleAll(tab, el) {
    var boxes = document.querySelectorAll('#dataBody' + tab + ' input[type=checkbox]');
    boxes.forEach(function (box) {
        box.checked = el.checked;
        bpOnCheck(tab, box);
    });
}

async function bpSearch(tab, page) {
    if (!bpCheckLogin()) return;
    var st = bpTabState(tab);
    if (page) st.pageNum = page;
    bpSetMeta(tab, '查询中…');
    var qs = bpBuildQuery(bpQueryParams(tab));
    var res = await get(bpApiBase() + '/entries' + qs);
    if (!res || res.code !== 200) {
        bpSetMeta(tab, '查询失败：' + (res && res.msg ? res.msg : '未知错误'));
        return;
    }
    var data = res.data || {};
    st.rows = data.rows || [];
    st.total = data.total != null ? data.total : 0;
    bpRenderTable(tab);
    bpUpdatePager(tab);
    var typeLabel = tab === '401' ? '退库' : '出库';
    bpSetMeta(tab, typeLabel + '查询完成 tenant=' + (res.tenantId || '') + ' env=' + (res.activeEnv || ''));
}

function bpReset(tab) {
    var p = tab === '401' ? 'q401_' : 'q201_';
    ['billNo', 'materialName', 'materialSpeci', 'departmentName', 'warehouseName'].forEach(function (f) {
        document.getElementById(p + f).value = '';
    });
    document.getElementById(p + 'hisPushStatus').value = '';
    var st = bpTabState(tab);
    st.pageNum = 1;
    st.selected.clear();
    bpUpdateBatchBtn(tab);
    bpSearch(tab, 1);
}

function bpPrevPage(tab) {
    var st = bpTabState(tab);
    if (st.pageNum > 1) bpSearch(tab, st.pageNum - 1);
}

function bpNextPage(tab) {
    var st = bpTabState(tab);
    var pages = Math.ceil(st.total / BP_PAGE_SIZE);
    if (st.pageNum < pages) bpSearch(tab, st.pageNum + 1);
}

function bpJsonPretty(obj) {
    if (obj === null || obj === undefined) return '';
    if (typeof obj === 'string') return obj;
    try {
        return JSON.stringify(obj, null, 2);
    } catch (e) {
        return String(obj);
    }
}

function bpFormatRequestBodyRaw(raw) {
    if (raw === null || raw === undefined) return '';
    var s = String(raw);
    try {
        return JSON.stringify(JSON.parse(s), null, 2);
    } catch (e) {
        return s;
    }
}

function bpCopyBlock(preId, btn) {
    var el = document.getElementById(preId);
    if (!el) return;
    var text = el.textContent || '';
    var done = function () {
        var old = btn.textContent;
        btn.textContent = '已复制';
        setTimeout(function () { btn.textContent = old; }, 1500);
    };
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(done).catch(function () {
            bpCopyFallback(text);
            done();
        });
    } else {
        bpCopyFallback(text);
        done();
    }
}

function bpCopyFallback(text) {
    var ta = document.createElement('textarea');
    ta.value = text;
    ta.style.position = 'fixed';
    ta.style.left = '-9999px';
    document.body.appendChild(ta);
    ta.select();
    try { document.execCommand('copy'); } catch (e) { /* ignore */ }
    document.body.removeChild(ta);
}

function bpRenderDebugBlock(title, content, blockId) {
    var empty = !content || !String(content).trim();
    var preClass = 'push-debug-pre' + (empty ? ' empty' : '');
    var body = empty ? '（无数据）' : bpEscape(content);
    return '<div class="push-debug-block">' +
        '<div class="push-debug-head"><span>' + bpEscape(title) + '</span>' +
        '<button type="button" class="btn-copy" onclick="bpCopyBlock(\'' + blockId + '\', this)">复制</button></div>' +
        '<pre class="' + preClass + '" id="' + blockId + '">' + body + '</pre></div>';
}

function bpBuildSpdWriteback(r) {
    if (!r) return null;
    return {
        status: r.status,
        success: r.success,
        skipped: r.skipped,
        billId: r.billId,
        billNo: r.billNo,
        billType: r.billType,
        pushedEntryCount: r.pushedEntryCount,
        entryTotal: r.entryTotal,
        alreadyPushedEntryCount: r.alreadyPushedEntryCount,
        traceId: r.traceId,
        message: r.message,
        spdTables: r.skipped
            ? '未回写（明细 his_push_status 已为成功）'
            : (r.success
                ? 'stk_io_bill.his_push_status / stk_io_bill_entry.his_push_status（出库另回写 stk_dep_inventory HIS 键）'
                : '推送失败，已标记 his_push_status=失败')
    };
}

function bpBuildPushResultHtml(tab, res) {
    var html = [];
    if (!res) {
        html.push('<p class="hint">无响应</p>');
        return html.join('');
    }

    html.push('<div class="inline-resp-label">排错：Headers / 入参 / HIS回参 / SPD落库</div>');

    var summary = {
        code: res.code,
        msg: res.msg,
        tenantId: res.tenantId,
        activeEnv: res.activeEnv,
        hospitalName: res.hospitalName
    };
    if (res.data) {
        summary.data = {
            total: res.data.total,
            pushedCount: res.data.pushedCount,
            skipCount: res.data.skipCount,
            successCount: res.data.successCount,
            failCount: res.data.failCount,
            message: res.data.message
        };
    }
    html.push(bpRenderDebugBlock('推送汇总', bpJsonPretty(summary), 'bpSum' + tab));

    var results = res.data && res.data.results;
    if (!results || !results.length) {
        html.push(bpRenderDebugBlock('完整外层回参', bpJsonPretty(res), 'bpFull' + tab));
        return html.join('');
    }

    results.forEach(function (r, idx) {
        if (!r) return;
        var prefix = 'bp' + tab + 'r' + idx;
        var apiCode = (r.billType === 401 ? '2.5.42' : '2.5.41');
        var titleCls = r.success === false ? 'push-inv-title fail' : 'push-inv-title';
        var title = apiCode + ' · billId=' + (r.billId != null ? r.billId : '—')
            + ' · ' + (r.billNo || '—');
        if (r.status) title += ' · ' + r.status;
        html.push('<div class="' + titleCls + '">' + bpEscape(title) + '</div>');

        var inv = r.hisInvoke;
        if (inv) {
            if (inv.url || inv.method) {
                html.push('<p class="hint">' + bpEscape((inv.method || 'POST') + ' ' + (inv.url || '')
                    + (inv.httpStatus != null ? ' · HTTP ' + inv.httpStatus : '')) + '</p>');
            }
            if (inv.note) {
                html.push('<p class="hint">' + bpEscape(inv.note) + '</p>');
            }
            html.push(bpRenderDebugBlock(
                '请求头 Headers（众阳 sign/license 已脱敏）',
                bpJsonPretty(inv.requestHeaders || {}),
                prefix + 'Hdr'));
            html.push(bpRenderDebugBlock(
                '入参 Request Body',
                inv.requestBodyRaw != null ? bpFormatRequestBodyRaw(inv.requestBodyRaw) : bpJsonPretty(inv.requestBody),
                prefix + 'Req'));
            var resp = inv.hisBody != null ? inv.hisBody : inv.responseRaw;
            html.push(bpRenderDebugBlock(
                '回参 Response（HIS hisBody）',
                bpJsonPretty(resp),
                prefix + 'Resp'));
        } else if (r.skipped) {
            html.push('<p class="hint">未调用 HIS：' + bpEscape(r.message || '明细均已推送成功') + '</p>');
        } else if (r.success === false) {
            html.push('<p class="hint">HIS 未返回或调用前失败，请查看 SPD 落库块中的 message</p>');
        }

        html.push(bpRenderDebugBlock(
            'SPD 落库回写（stk_io_bill / entry his_push_status）',
            bpJsonPretty(bpBuildSpdWriteback(r)),
            prefix + 'Spd'));
    });

    return html.join('');
}

function bpShowPushResult(tab, res) {
    var el = document.getElementById('pushResult' + tab);
    if (!el) return;
    el.style.display = 'block';
    el.innerHTML = bpBuildPushResultHtml(tab, res);
}

async function bpPushOne(tab, billId) {
    if (!bpCheckLogin()) return;
    var typeLabel = tab === '401' ? '退库单' : '出库单';
    if (!confirm('确认推送' + typeLabel + ' id=' + billId + ' 至 HIS？')) return;
    bpSetMeta(tab, '推送中 billId=' + billId + '…');
    var res = await post(bpApiBase() + '/push/' + billId, {});
    bpShowPushResult(tab, res);
    if (res && res.code === 200) {
        bpSetMeta(tab, res.msg || (typeLabel + '推送完成'));
        bpSearch(tab, bpTabState(tab).pageNum);
    } else {
        bpSetMeta(tab, '推送失败：' + (res && res.msg ? res.msg : ''));
    }
}

async function bpBatchPush(tab) {
    if (!bpCheckLogin()) return;
    var st = bpTabState(tab);
    var ids = Array.from(st.selected).map(function (s) { return Number(s); });
    if (!ids.length) return;
    var typeLabel = tab === '401' ? '退库单' : '出库单';
    if (!confirm('确认推送选中的 ' + ids.length + ' 张' + typeLabel + '？')) return;
    bpSetMeta(tab, '批量推送中…');
    var res = await post(bpApiBase() + '/push', { billIds: ids });
    bpShowPushResult(tab, res);
    if (res && res.code === 200) {
        st.selected.clear();
        bpUpdateBatchBtn(tab);
        bpSetMeta(tab, res.msg || '批量推送完成');
        bpSearch(tab, st.pageNum);
    } else {
        bpSetMeta(tab, '批量推送失败：' + (res && res.msg ? res.msg : ''));
    }
}

/** 探针页「出退库推送」主 Tab 首次打开时加载（出库在上、退库在下，同时初始化） */
function bpInitBillPushTab() {
    if (bpProbeInited) return;
    bpProbeInited = true;
    if (bpCheckLogin()) {
        bpSearch('201', 1);
        bpSearch('401', 1);
    }
}

document.addEventListener('DOMContentLoaded', function () {
    bpStandalonePage = !!document.getElementById('bill-push-standalone');
    if (!bpStandalonePage && document.getElementById('tab-bill-push')) {
        return;
    }
    if (bpCheckLogin()) {
        bpSearch('201', 1);
        bpSearch('401', 1);
    }
});
