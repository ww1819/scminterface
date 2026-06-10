/**
 * 科室 SPD vs HIS 库存核对报表（联调页 Tab）
 */
var DS_PAGE_SIZE = 20;
var dsProbeInited = false;
var dsState = { pageNum: 1, total: 0, rows: [] };
/** 跨页多选缓存：rowKey -> row */
var dsSelectedMap = {};

var DS_BATCH_PATH = '/spd/query/drug-batch-stocks';
var DS_PROBE_API_KEY = 'dsBatchStocks';
var DS_PROBE_MIRROR_KEY = 'mirror_dsBatchStocks';

function dsApiBase() {
    if (typeof msunHospitalApi === 'function') {
        return msunHospitalApi() + '/dept-stock';
    }
    return '/api/vendor/msun/hospitals/zaoqiang-tcm-001/dept-stock';
}

function dsEscape(s) {
    return String(s == null ? '' : s)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function dsFormatQty(v) {
    if (v === null || v === undefined || v === '') return '—';
    return v;
}

function dsQtySourceTag(source) {
    if (source === 'batch') return '<span class="tag tag-2" style="margin-left:4px">批次</span>';
    if (source === 'merge') return '<span class="tag tag-1" style="margin-left:4px">合并</span>';
    return '';
}

function dsDiffClass(diff) {
    if (diff == null || diff === '') return '';
    var n = Number(diff);
    if (isNaN(n) || n === 0) return 'ds-diff-ok';
    return 'ds-diff-warn';
}

function dsRowKey(row) {
    if (!row) return '';
    if (row.dep_inventory_id != null && row.dep_inventory_id !== '') {
        return 'inv:' + row.dep_inventory_id;
    }
    return [
        row.department_his_id || '',
        row.material_his_id || '',
        row.material_his_spec_packing_id || '',
        row.batch_number || ''
    ].join('|');
}

function dsGetPageSize() {
    var el = document.getElementById('dsPageSize');
    var n = el ? parseInt(el.value, 10) : DS_PAGE_SIZE;
    return isNaN(n) || n < 1 ? DS_PAGE_SIZE : Math.min(n, 200);
}

function dsQueryParams() {
    return {
        departmentKeyword: (document.getElementById('ds_departmentKeyword') || {}).value.trim(),
        materialKeyword: (document.getElementById('ds_materialKeyword') || {}).value.trim(),
        specKeyword: (document.getElementById('ds_specKeyword') || {}).value.trim(),
        pageNum: dsState.pageNum,
        pageSize: dsGetPageSize()
    };
}

function dsBuildQuery(params) {
    var parts = [];
    Object.keys(params).forEach(function (key) {
        var val = params[key];
        if (val !== null && val !== undefined && String(val).trim() !== '') {
            parts.push(encodeURIComponent(key) + '=' + encodeURIComponent(val));
        }
    });
    return parts.length ? '?' + parts.join('&') : '';
}

function dsSetMeta(text) {
    var el = document.getElementById('dsMetaInfo');
    if (el) el.textContent = text;
}

function dsTotalPages() {
    return Math.max(1, Math.ceil(dsState.total / dsGetPageSize()));
}

function dsUpdateSelCount() {
    var el = document.getElementById('dsSelCount');
    if (el) el.textContent = String(Object.keys(dsSelectedMap).length);
}

function dsCanProbe(row) {
    return row && row.department_his_id && row.material_his_id && row.material_his_spec_packing_id;
}

function dsUpdateProbeFields(row) {
    if (!row) return;
    var map = {
        ds_probe_deptId: row.department_his_id,
        ds_probe_drugId: row.material_his_id,
        ds_probe_drugSpecPackingId: row.material_his_spec_packing_id
    };
    Object.keys(map).forEach(function (id) {
        var el = document.getElementById(id);
        if (el && map[id] != null) el.value = String(map[id]);
    });
}

function dsCollectProbeParams() {
    return {
        deptId: (document.getElementById('ds_probe_deptId') || {}).value.trim(),
        drugId: (document.getElementById('ds_probe_drugId') || {}).value.trim(),
        drugSpecPackingId: (document.getElementById('ds_probe_drugSpecPackingId') || {}).value.trim()
    };
}

function dsValidateProbeParams(params) {
    if (!params.deptId || !params.drugId || !params.drugSpecPackingId) {
        alert('请填写 deptId、drugId、drugSpecPackingId');
        return false;
    }
    return true;
}

function dsShowProbeCard(apiKey, title, requestLine, result, elapsedMs) {
    var card = document.getElementById('resp-card-' + apiKey);
    if (!card) return;
    var sum = typeof zqHisSummary === 'function' ? zqHisSummary(result) : { ok: result && result.code === 200, text: (result && result.msg) || '' };
    var mirrorSum = '';
    if (apiKey === DS_PROBE_API_KEY && typeof zqMirrorSummary === 'function') {
        mirrorSum = zqMirrorSummary(result);
    }
    if (apiKey === DS_PROBE_MIRROR_KEY && typeof zqMirrorDataSummary === 'function') {
        mirrorSum = zqMirrorDataSummary(result);
    }
    var headBody =
        '<strong>' + dsEscape(title) + '</strong> · ' + elapsedMs + 'ms<br>' +
        '<code class="req-line">' + dsEscape(requestLine) + '</code><br>' +
        '<span class="summary">' + dsEscape(sum.text || '') + '</span>' +
        (mirrorSum ? '<br><span class="summary">' + dsEscape(mirrorSum) + '</span>' : '') +
        (result && result.activeEnv ? '<br><span class="env-tag">环境: ' + result.activeEnv + ' · ' + (result.msunBaseUrl || '') + '</span>' : '');
    card.classList.remove('empty');
    card.innerHTML =
        '<div class="resp-head ' + (sum.ok ? 'ok' : 'err') + '">' +
        '<div class="resp-head-row"><div class="resp-head-body">' + headBody + '</div></div></div>' +
        '<div class="api-resp-body probe-debug-body"></div>';
    var bodyEl = card.querySelector('.api-resp-body');
    if (bodyEl && typeof zqBuildDebugPanelsHtml === 'function') {
        bodyEl.innerHTML = zqBuildDebugPanelsHtml(apiKey, result, requestLine);
    } else if (bodyEl) {
        bodyEl.innerHTML = '<pre class="push-debug-pre">' + dsEscape(JSON.stringify(result, null, 2)) + '</pre>';
    }
    var slot = card.closest('.api-inline-resp');
    if (slot) slot.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function dsFillBatchStocksSilent(row) {
    if (!dsCanProbe(row)) return false;
    dsUpdateProbeFields(row);
    zqSetField('batchStocks', 'deptId', row.department_his_id);
    zqSetField('batchStocks', 'drugId', row.material_his_id);
    zqSetField('batchStocks', 'drugSpecPackingId', row.material_his_spec_packing_id);
    return true;
}

function dsFillBatchStocks(row) {
    if (!dsFillBatchStocksSilent(row)) {
        alert('缺少 HIS 三要素（科室 his_id / 耗材 his_id / his_spec_packing_id）');
        return;
    }
    dsSetMeta('已填充下方 2.5.43 探针入参');
}

async function dsCallBatchStocks(row) {
    if (!dsFillBatchStocksSilent(row)) {
        alert('缺少 HIS 三要素，无法调用 2.5.43');
        return;
    }
    await dsCallProbe();
}

function dsToggleRow(row, checked) {
    var key = dsRowKey(row);
    if (!key) return;
    if (checked) {
        dsSelectedMap[key] = row;
    } else {
        delete dsSelectedMap[key];
    }
    dsUpdateSelCount();
    dsSyncChkAllPage();
}

function dsSyncChkAllPage() {
    var chkAll = document.getElementById('dsChkAllPage');
    if (!chkAll || !dsState.rows.length) {
        if (chkAll) chkAll.checked = false;
        return;
    }
    var all = true;
    var any = false;
    dsState.rows.forEach(function (row) {
        var key = dsRowKey(row);
        if (!key) return;
        any = true;
        if (!dsSelectedMap[key]) all = false;
    });
    chkAll.checked = any && all;
}

function dsToggleAllPage(el) {
    var checked = el && el.checked;
    dsState.rows.forEach(function (row) {
        dsToggleRow(row, checked);
    });
    dsRenderTable();
}

function dsClearSelection() {
    dsSelectedMap = {};
    dsUpdateSelCount();
    var chkAll = document.getElementById('dsChkAllPage');
    if (chkAll) chkAll.checked = false;
    dsRenderTable();
}

function dsGetSelectedRows() {
    return Object.keys(dsSelectedMap).map(function (k) { return dsSelectedMap[k]; });
}

function dsFillProbeFromSelection() {
    var rows = dsGetSelectedRows();
    if (!rows.length) {
        alert('请先勾选报表行');
        return;
    }
    var row = rows[0];
    if (!dsFillBatchStocksSilent(row)) {
        alert('选中行缺少 HIS 三要素');
        return;
    }
    if (rows.length > 1) {
        dsSetMeta('已用第 1 条选中行填充探针（共 ' + rows.length + ' 条选中）');
    } else {
        dsSetMeta('已填充探针入参');
    }
}

async function dsCallSelectedProbes() {
    var rows = dsGetSelectedRows().filter(dsCanProbe);
    if (!rows.length) {
        alert('请先勾选含 HIS 三要素的行');
        return;
    }
    if (!confirm('对 ' + rows.length + ' 条选中明细依次调用 2.5.43 并刷新 HIS 库存数量？')) return;
    dsSetMeta('调用并刷新选中明细 HIS 库存…');
    var lastPack = null;
    for (var i = 0; i < rows.length; i++) {
        dsUpdateProbeFields(rows[i]);
        var params = dsCollectProbeParams();
        lastPack = await zqInvokeProbeBackend('batchStocks', params, null);
        dsShowProbeCard(
            DS_PROBE_API_KEY,
            '2.5.43 药房批次库存（选中 ' + (i + 1) + '/' + rows.length + '）',
            lastPack.requestLine,
            lastPack.result,
            lastPack.elapsed
        );
        if (i < rows.length - 1) await new Promise(function (r) { setTimeout(r, 200); });
    }
    await dsSearch(dsState.pageNum);
    dsSetMeta('已调用并刷新 · 共 ' + rows.length + ' 条选中明细');
}

function dsClearProbeForm() {
    ['ds_probe_deptId', 'ds_probe_drugId', 'ds_probe_drugSpecPackingId'].forEach(function (id) {
        var el = document.getElementById(id);
        if (el) el.value = '';
    });
}

async function dsCallProbe() {
    if (!zqCheckLogin()) return;
    var params = dsCollectProbeParams();
    if (!dsValidateProbeParams(params)) return;
    dsSetMeta('调用 2.5.43…');
    var pack = await zqInvokeProbeBackend('batchStocks', params, null);
    dsShowProbeCard(DS_PROBE_API_KEY, '2.5.43 药房批次库存', pack.requestLine, pack.result, pack.elapsed);
    dsSetMeta('2.5.43 调用完成');
    return pack.result;
}

async function dsViewProbeMirror() {
    if (!zqCheckLogin()) return;
    var params = dsCollectProbeParams();
    if (!dsValidateProbeParams(params)) return;
    var qs = '?deptId=' + encodeURIComponent(params.deptId) +
        '&drugId=' + encodeURIComponent(params.drugId) +
        '&drugSpecPackingId=' + encodeURIComponent(params.drugSpecPackingId);
    var url = msunHospitalApi() + '/mirror/entry-his' + qs;
    dsSetMeta('查询镜像库…');
    var t0 = Date.now();
    var result;
    try {
        result = await get(url);
    } catch (e) {
        result = { code: 500, msg: e.message };
    }
    dsShowProbeCard(DS_PROBE_MIRROR_KEY, '[镜像] 2.5.43 批次库存 entry-his', 'GET ' + url, result, Date.now() - t0);
    dsSetMeta('镜像查询完成');
}

function dsRenderTable() {
    var tbody = document.getElementById('dsDataBody');
    if (!tbody) return;
    if (!dsState.rows.length) {
        tbody.innerHTML = '<tr><td colspan="13" style="text-align:center;color:#9ca3af;">无数据，请调整条件后搜索</td></tr>';
        dsSyncChkAllPage();
        return;
    }
    tbody.innerHTML = dsState.rows.map(function (row, idx) {
        var can = dsCanProbe(row);
        var diffCls = dsDiffClass(row.qty_diff);
        var key = dsRowKey(row);
        var checked = !!(key && dsSelectedMap[key]);
        return '<tr class="' + (checked ? 'log-ok' : '') + '">' +
            '<td><input type="checkbox"' + (checked ? ' checked' : '') +
                ' onchange="dsToggleRow(dsState.rows[' + idx + '], this.checked); dsRenderTable()"></td>' +
            '<td>' + (idx + 1 + (dsState.pageNum - 1) * dsGetPageSize()) + '</td>' +
            '<td>' + dsEscape(row.department_code) + '</td>' +
            '<td>' + dsEscape(row.department_name) + '</td>' +
            '<td>' + dsEscape(row.material_code) + '</td>' +
            '<td>' + dsEscape(row.material_name) + '</td>' +
            '<td>' + dsEscape(row.material_spec) + '</td>' +
            '<td>' + dsEscape(row.batch_number) + '</td>' +
            '<td style="text-align:right">' + dsFormatQty(row.spd_qty) + '</td>' +
            '<td style="text-align:right">' + dsFormatQty(row.his_qty) + dsQtySourceTag(row.his_qty_source) + '</td>' +
            '<td style="text-align:right" class="' + diffCls + '">' + dsFormatQty(row.qty_diff) + '</td>' +
            '<td style="font-size:11px">' + dsEscape(row.department_his_id) + ' / ' +
                dsEscape(row.material_his_id) + ' / ' + dsEscape(row.material_his_spec_packing_id) + '</td>' +
            '<td style="white-space:nowrap">' +
                '<button type="button" class="btn-call secondary" style="padding:4px 8px;font-size:11px;margin:2px"' +
                    (can ? '' : ' disabled') + ' onclick="dsFillBatchStocks(dsState.rows[' + idx + '])">填充</button>' +
                '<button type="button" class="btn-call" style="padding:4px 8px;font-size:11px;margin:2px"' +
                    (can ? '' : ' disabled') + ' onclick="dsCallBatchStocks(dsState.rows[' + idx + '])">调用</button>' +
            '</td></tr>';
    }).join('');
    dsSyncChkAllPage();
}

function dsUpdatePager() {
    var pager = document.getElementById('dsPagerInfo');
    if (!pager) return;
    pager.textContent = '第 ' + dsState.pageNum + ' / ' + dsTotalPages() + ' 页，共 ' + dsState.total + ' 条';
    var jump = document.getElementById('dsPageJump');
    if (jump && document.activeElement !== jump) jump.value = dsState.pageNum;
}

async function dsSearch(page) {
    if (!zqCheckLogin()) return;
    if (page) dsState.pageNum = page;
    dsSetMeta('查询中…');
    var url = dsApiBase() + '/list' + dsBuildQuery(dsQueryParams());
    var res = await request(url);
    if (!res || res.code !== 200) {
        dsSetMeta('查询失败: ' + ((res && res.msg) || '未知错误'));
        return;
    }
    var data = res.data || {};
    dsState.rows = data.rows || [];
    dsState.total = data.total || 0;
    if (dsState.pageNum > dsTotalPages()) {
        dsState.pageNum = dsTotalPages();
        return dsSearch(dsState.pageNum);
    }
    dsRenderTable();
    dsUpdatePager();
    dsUpdateSelCount();
    dsSetMeta('就绪 · 共 ' + dsState.total + ' 条科室库存明细');
}

function dsReset() {
    ['ds_departmentKeyword', 'ds_materialKeyword', 'ds_specKeyword'].forEach(function (id) {
        var el = document.getElementById(id);
        if (el) el.value = '';
    });
    dsClearSelection();
    dsState.pageNum = 1;
    dsSearch(1);
}

function dsChangePageSize() {
    dsState.pageNum = 1;
    dsSearch(1);
}

function dsPrevPage() {
    if (dsState.pageNum > 1) dsSearch(dsState.pageNum - 1);
}

function dsNextPage() {
    if (dsState.pageNum < dsTotalPages()) dsSearch(dsState.pageNum + 1);
}

function dsGoPage(page) {
    var p = Math.max(1, Math.min(page, dsTotalPages()));
    dsSearch(p);
}

function dsGoLastPage() {
    dsGoPage(dsTotalPages());
}

function dsJumpPage() {
    var el = document.getElementById('dsPageJump');
    var p = el ? parseInt(el.value, 10) : NaN;
    if (isNaN(p) || p < 1) {
        alert('请输入有效页码');
        return;
    }
    dsGoPage(p);
}

async function dsRefreshMergeStock() {
    var deptHisId = null;
    var rows = dsGetSelectedRows();
    if (rows.length && rows[0].department_his_id) {
        deptHisId = rows[0].department_his_id;
    } else if (dsState.rows.length) {
        deptHisId = dsState.rows[0].department_his_id;
    }
    if (!deptHisId) {
        alert('请先搜索或勾选含 HIS 科室 ID 的行');
        return;
    }
    zqSetField('mergeStocks', 'deptId', deptHisId);
    zqSetField('mergeStocks', 'cascadeBatch', 'true');
    await zqCallApi('mergeStocks');
    dsSearch(dsState.pageNum);
}

function dsInitDeptStockTab() {
    if (dsProbeInited) return;
    dsProbeInited = true;
    dsUpdateSelCount();
    dsSearch(1);
}
