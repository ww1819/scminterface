/**
 * 众阳 HIS 单据推送页：出库(201) / 退库(401) 分 Tab 测试
 */
const BP_API = '/api/vendor/msun/hospitals/zaoqiang-tcm-001/spd/bill-push';
const BP_PAGE_SIZE = 20;

var BP_PUSH_LABELS = { '0': '未推送', '1': '推送中', '2': '成功', '3': '失败' };

var bpActiveTab = '201';

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

function bpSwitchTab(tab) {
    bpActiveTab = tab;
    document.querySelectorAll('.bill-tab-btn').forEach(function (btn) {
        btn.classList.toggle('active', btn.getAttribute('data-tab') === tab);
    });
    document.querySelectorAll('.bill-tab-panel').forEach(function (panel) {
        panel.classList.toggle('active', panel.id === 'tab-' + tab);
    });
    var st = bpTabState(tab);
    if (!st.rows.length && st.total === 0) {
        bpSearch(tab, 1);
    }
}

async function bpSearch(tab, page) {
    if (!bpCheckLogin()) return;
    var st = bpTabState(tab);
    if (page) st.pageNum = page;
    bpSetMeta(tab, '查询中…');
    var qs = bpBuildQuery(bpQueryParams(tab));
    var res = await get(BP_API + '/entries' + qs);
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

function bpShowPushResult(tab, res) {
    var el = document.getElementById('pushResult' + tab);
    if (!el) return;
    el.style.display = 'block';
    try {
        el.textContent = JSON.stringify(res, null, 2);
    } catch (e) {
        el.textContent = String(res);
    }
}

async function bpPushOne(tab, billId) {
    if (!bpCheckLogin()) return;
    var typeLabel = tab === '401' ? '退库单' : '出库单';
    if (!confirm('确认推送' + typeLabel + ' id=' + billId + ' 至 HIS？')) return;
    bpSetMeta(tab, '推送中 billId=' + billId + '…');
    var res = await post(BP_API + '/push/' + billId, {});
    bpShowPushResult(tab, res);
    if (res && res.code === 200) {
        bpSetMeta(tab, typeLabel + '推送完成');
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
    var res = await post(BP_API + '/push', { billIds: ids });
    bpShowPushResult(tab, res);
    if (res && res.code === 200) {
        st.selected.clear();
        bpUpdateBatchBtn(tab);
        bpSetMeta(tab, '批量推送完成');
        bpSearch(tab, st.pageNum);
    } else {
        bpSetMeta(tab, '批量推送失败：' + (res && res.msg ? res.msg : ''));
    }
}

document.addEventListener('DOMContentLoaded', function () {
    if (bpCheckLogin()) {
        bpSearch('201', 1);
    }
});
