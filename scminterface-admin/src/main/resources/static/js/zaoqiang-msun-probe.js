/**
 * 枣强中医院众阳接口联调页脚本
 */
const ZQ_MSUN_API = '/api/customer/zaoqiang-tcm-001/msun';

function zqCheckLogin() {
    if (!getToken()) {
        window.location.href = '/login.html?redirect=' + encodeURIComponent('/zaoqiang-msun-probe.html');
        return false;
    }
    return true;
}

function zqBuildQuery(params) {
    const parts = [];
    Object.keys(params).forEach(function (key) {
        const val = params[key];
        if (val !== null && val !== undefined && String(val).trim() !== '') {
            parts.push(encodeURIComponent(key) + '=' + encodeURIComponent(val));
        }
    });
    return parts.length ? '?' + parts.join('&') : '';
}

function zqFmtJson(obj) {
    try {
        return JSON.stringify(obj, null, 2);
    } catch (e) {
        return String(obj);
    }
}

function zqSetMeta(text) {
    document.getElementById('metaInfo').textContent = text;
}

function zqShowResult(title, requestLine, result, elapsedMs) {
    const box = document.getElementById('responseBox');
    const status = result && result.code === 200 ? 'ok' : 'err';
    const hisBody = result && result.data && result.data.hisBody;
    let summary = '';
    if (hisBody && typeof hisBody === 'object') {
        summary = 'HIS success=' + hisBody.success + ', code=' + (hisBody.code || '') + ', message=' + (hisBody.message || '');
        if (Array.isArray(hisBody.data)) {
            summary += ', data.length=' + hisBody.data.length;
        }
    } else if (result) {
        summary = 'scminterface code=' + result.code + ', msg=' + (result.msg || '');
    }
    box.innerHTML =
        '<div class="resp-head ' + status + '">' +
        '<strong>' + title + '</strong> · ' + elapsedMs + 'ms<br>' +
        '<code class="req-line">' + requestLine + '</code><br>' +
        '<span class="summary">' + summary + '</span>' +
        (result && result.activeEnv ? '<br><span class="env-tag">环境: ' + result.activeEnv + ' · ' + (result.msunBaseUrl || '') + '</span>' : '') +
        '</div>' +
        '<pre class="resp-json">' + zqEscapeHtml(zqFmtJson(result)) + '</pre>';
}

function zqEscapeHtml(s) {
    return String(s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
}

async function zqInvoke(title, method, path, queryParams, body) {
    if (!zqCheckLogin()) return;
    const qs = queryParams ? zqBuildQuery(queryParams) : '';
    const url = ZQ_MSUN_API + path + qs;
    const requestLine = method + ' ' + url + (body ? '\nBody: ' + zqFmtJson(body) : '');
    zqSetMeta('请求中…');
    const t0 = Date.now();
    let result;
    try {
        if (method === 'GET') {
            result = await get(url);
        } else {
            result = await post(url, body || {});
        }
    } catch (e) {
        result = { code: 500, msg: e.message };
    }
    zqShowResult(title, requestLine, result, Date.now() - t0);
    zqSetMeta('上次请求: ' + title);
}

function zqVal(id) {
    const el = document.getElementById(id);
    if (!el) return '';
    return el.value.trim();
}

function zqValInt(id) {
    const v = zqVal(id);
    return v === '' ? '' : parseInt(v, 10);
}

function zqSwitchTab(tabId, btn) {
    document.querySelectorAll('.tab-panel').forEach(function (p) { p.classList.remove('active'); });
    document.querySelectorAll('.tab-btn').forEach(function (b) { b.classList.remove('active'); });
    document.getElementById(tabId).classList.add('active');
    if (btn) btn.classList.add('active');
}

function zqInitDefaults() {
    const now = new Date();
    const end = zqFormatDt(now);
    const startDate = new Date(now.getTime() - 7 * 24 * 3600 * 1000);
    const start = zqFormatDt(startDate);
    const endEl = document.getElementById('yk_endTime');
    const startEl = document.getElementById('yk_startTime');
    if (endEl && !endEl.value) endEl.value = end;
    if (startEl && !startEl.value) startEl.value = start;
}

function zqFormatDt(d) {
    const pad = function (n) { return n < 10 ? '0' + n : '' + n; };
    return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
        ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds());
}

async function zqLoadEnv() {
    await zqInvoke('当前环境', 'GET', '/env', null, null);
}

// --- 绑定调用 ---
function zqCallDepts() {
    zqInvoke('2.1.9 科室', 'GET', '/depts', {
        hospitalAreaId: zqVal('dept_hospitalAreaId'),
        invalidFlag: zqVal('dept_invalidFlag') || '-1',
        deptId: zqVal('dept_deptId'),
        deptName: zqVal('dept_deptName')
    });
}

function zqCallIdentities() {
    zqInvoke('2.1.12 人员身份', 'GET', '/identities', {
        roleType: zqVal('id_roleType'),
        deptId: zqVal('id_deptId'),
        identityId: zqVal('id_identityId'),
        userId: zqVal('id_userId')
    });
}

function zqCallIdentitiesSample() {
    zqInvoke('2.1.12 人员(自动取首科室)', 'GET', '/identities/sample', {
        roleType: zqVal('id_sampleRoleType') || '0'
    });
}

function zqCallDrugDict() {
    zqInvoke('2.5.44 药品材料字典', 'GET', '/spd/query/drug-dict-infos', {
        drugCode: zqVal('dd_drugCode'),
        drugId: zqVal('dd_drugId'),
        drugName: zqVal('dd_drugName'),
        startTime: zqVal('dd_startTime'),
        endTime: zqVal('dd_endTime'),
        limitCount: zqVal('dd_limitCount') || '10',
        materialOrDrug: zqVal('dd_materialOrDrug'),
        specialFlag: zqVal('dd_specialFlag'),
        invalidFlag: zqVal('dd_invalidFlag')
    });
}

function zqCallDictCategory() {
    zqInvoke('2.5.58 分类字典', 'GET', '/spd/query/dict-category', {
        keyWord: zqVal('dc_keyWord'),
        limitCount: zqVal('dc_limitCount') || '20'
    });
}

function zqCallSuppliers() {
    zqInvoke('2.5.62 供应商', 'GET', '/spd/query/drug-suppliers', {
        keyWord: zqVal('sup_keyWord'),
        limitCount: zqVal('sup_limitCount') || '20',
        materialOrDrug: zqVal('sup_materialOrDrug')
    });
}

function zqCallProducers() {
    zqInvoke('2.5.63 生产厂商', 'GET', '/spd/query/drug-producers', {
        keyWord: zqVal('prod_keyWord'),
        limitCount: zqVal('prod_limitCount') || '20',
        materialOrDrug: zqVal('prod_materialOrDrug')
    });
}

function zqCallBatchStocks() {
    zqInvoke('2.5.43 药房批次库存', 'GET', '/spd/query/drug-batch-stocks', {
        deptId: zqVal('bs_deptId'),
        drugId: zqVal('bs_drugId'),
        drugSpecPackingId: zqVal('bs_drugSpecPackingId')
    });
}

function zqCallYkInstock() {
    const body = {
        startTime: zqVal('yk_startTime'),
        endTime: zqVal('yk_endTime')
    };
    const deptId = zqVal('yk_deptId');
    const type = zqVal('yk_type');
    const instockCode = zqVal('yk_instockCode');
    if (deptId) body.deptId = parseInt(deptId, 10);
    if (type) body.type = type;
    if (instockCode) body.instockCode = instockCode;
    zqInvoke('2.5.102 一级库入退库', 'POST', '/spd/query/yk-instock', null, body);
}

document.addEventListener('DOMContentLoaded', function () {
    if (!zqCheckLogin()) return;
    zqInitDefaults();
    zqLoadEnv();
});
