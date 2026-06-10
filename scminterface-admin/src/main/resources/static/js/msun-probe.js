/**
 * 众阳 HIS 接口统一联调页（厂家 → 医院客户）
 */
const MSUN_VENDOR_API = '/api/vendor/msun';
const MSUN_LS_KEY = 'msun_probe_params_v3';
var msunHospitals = [];
var msunSelectedHospital = null;

function msunHospitalApi() {
    if (msunSelectedHospital && msunSelectedHospital.apiPrefix) {
        return msunSelectedHospital.apiPrefix;
    }
    return '/api/vendor/msun/hospitals/zaoqiang-tcm-001';
}

function msunLsKey() {
    const key = msunSelectedHospital ? msunSelectedHospital.hospitalKey : 'default';
    return MSUN_LS_KEY + '_' + key;
}

var zqTestLog = [];
var zqLastResult = null;
var zqLastApiKey = null;
var zqResponseStore = {};
var zqRunningAll = false;
var zqFetchingAllPages = false;
var zqFetchingAllRoles = false;
var zqRunningFetchAll = false;

var ZQ_API_RESP_ORDER = ['env', 'depts', 'identities', 'drugDict', 'dictCategory', 'suppliers', 'producers', 'mergeStocks', 'batchStocks', 'ykInstock'];
var ZQ_API_RESP_LABELS = { env: '当前环境' };
var ZQ_LOG_TITLE_TO_KEY = { '当前环境': 'env' };

function zqFieldId(apiKey, fieldKey) {
    return 'zq_' + apiKey + '_' + fieldKey;
}

function zqJsonId(apiKey) {
    return 'zq_json_' + apiKey;
}

function zqCheckLogin() {
    if (!getToken()) {
        window.location.href = '/login.html?redirect=' + encodeURIComponent('/msun-probe.html');
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
    try { return JSON.stringify(obj, null, 2); } catch (e) { return String(obj); }
}

function zqFmtRequestBodyRaw(raw) {
    if (raw === null || raw === undefined) return '';
    var s = String(raw);
    try { return JSON.stringify(JSON.parse(s), null, 2); } catch (e) { return s; }
}

function zqIsSchemaApiKey(apiKey) {
    return !!(apiKey && MSUN_PARAM_SCHEMA[apiKey]);
}

function zqCollectStringParams(apiKey) {
    return zqCollectFromForm(apiKey);
}

function zqGetJsonOverrideRaw(apiKey) {
    const ta = document.getElementById(zqJsonId(apiKey));
    if (!ta || !ta.value.trim()) return null;
    return ta.value.trim();
}

function zqValidateJsonOverrideSyntax(apiKey) {
    const raw = zqGetJsonOverrideRaw(apiKey);
    if (raw === null) return true;
    try {
        JSON.parse(raw);
        return true;
    } catch (e) {
        alert('JSON 入参格式错误: ' + e.message);
        return false;
    }
}

function zqToStringParams(src) {
    const out = {};
    if (!src) return out;
    Object.keys(src).forEach(function (key) {
        const val = src[key];
        if (val !== null && val !== undefined && String(val).trim() !== '') {
            out[key] = String(val);
        }
    });
    return out;
}

async function zqInvokeProbeBackend(apiKey, stringParams, paramsJsonOverride) {
    const url = msunHospitalApi() + '/probe/invoke';
    const params = zqToStringParams(stringParams);
    const payload = {
        apiKey: apiKey,
        params: params,
        paramsJsonOverride: paramsJsonOverride || null
    };
    const requestLine = 'POST ' + url + '\nBody: ' + zqFmtJson(payload);
    const t0 = Date.now();
    let result;
    try {
        result = await post(url, payload);
    } catch (e) {
        result = { code: 500, msg: e.message };
    }
    return { result: result, elapsed: Date.now() - t0, requestLine: requestLine };
}

async function zqFetchFirstDeptIdFromMirror() {
    try {
        const mirror = await get(msunHospitalApi() + '/mirror/data/depts?limit=1&offset=0');
        const tables = mirror && mirror.data && mirror.data.tables;
        if (!tables || !tables.length) return null;
        const rows = tables[0].rows;
        if (!rows || !rows.length) return null;
        const row = rows[0];
        if (row.dept_id != null && String(row.dept_id).trim()) return String(row.dept_id);
        if (row.deptId != null && String(row.deptId).trim()) return String(row.deptId);
    } catch (e) { /* ignore */ }
    return null;
}

function zqApplyDeptIdToForms(deptId) {
    if (!deptId) return;
    zqSetField('identities', 'deptId', deptId);
    zqSetField('mergeStocks', 'deptId', deptId);
    zqSetField('batchStocks', 'deptId', deptId);
    zqSetField('ykInstock', 'deptId', deptId);
}

function zqSetMeta(text) {
    document.getElementById('metaInfo').textContent = text;
}

function zqEscapeHtml(s) {
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function zqCopyDebugBlock(preId, btn) {
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
            zqCopyFallbackText(text);
            done();
        });
    } else {
        zqCopyFallbackText(text);
        done();
    }
}

function zqCopyFallbackText(text) {
    var ta = document.createElement('textarea');
    ta.value = text;
    ta.style.position = 'fixed';
    ta.style.left = '-9999px';
    document.body.appendChild(ta);
    ta.select();
    try { document.execCommand('copy'); } catch (e) { /* ignore */ }
    document.body.removeChild(ta);
}

function zqRenderDebugBlock(title, content, blockId) {
    var empty = content === null || content === undefined || !String(content).trim();
    var preClass = 'push-debug-pre' + (empty ? ' empty' : '');
    var body = empty ? '（无数据）' : zqEscapeHtml(content);
    return '<div class="push-debug-block">' +
        '<div class="push-debug-head"><span>' + zqEscapeHtml(title) + '</span>' +
        '<button type="button" class="btn-copy" onclick="zqCopyDebugBlock(\'' + blockId + '\', this)">复制</button></div>' +
        '<pre class="' + preClass + '" id="' + blockId + '">' + body + '</pre></div>';
}

function zqParseRequestLine(requestLine) {
    var line = String(requestLine || '').trim();
    if (!line) return { method: '', url: '' };
    var firstNl = line.indexOf('\n');
    var head = firstNl >= 0 ? line.substring(0, firstNl) : line;
    var parts = head.trim().split(/\s+/);
    return { method: parts[0] || '', url: parts[1] || '' };
}

function zqResolveInvokeDebug(result, requestLine) {
    var data = result && result.data;
    if (data && data.hisInvoke) {
        return data.hisInvoke;
    }
    if (data && (data.hisBody || data.requestParams)) {
        return {
            requestBody: data.requestParams || null,
            hisBody: data.hisBody,
            responseRaw: data.hisBody
        };
    }
    var parsed = zqParseRequestLine(requestLine);
    return {
        note: '本段为浏览器 → scminterface 前置机（JWT Bearer），非众阳 HIS 签名头',
        method: parsed.method,
        url: parsed.url,
        requestHeaders: { Authorization: 'Bearer <登录Token>' },
        requestBody: data,
        responseRaw: result
    };
}

function zqBuildDebugPanelsHtml(apiKey, result, requestLine) {
    var inv = zqResolveInvokeDebug(result, requestLine);
    var prefix = 'zqDbg' + apiKey.replace(/[^a-zA-Z0-9_]/g, '_');
    var html = [];

    if (inv && inv.note) {
        html.push('<p class="hint">' + zqEscapeHtml(inv.note) + '</p>');
    }
    if (inv && (inv.apiCode || inv.url)) {
        var title = (inv.apiCode || 'HIS') + (inv.method ? ' · ' + inv.method : '');
        if (inv.url) title += ' · ' + inv.url;
        if (inv.httpStatus != null) title += ' · HTTP ' + inv.httpStatus;
        html.push('<div class="push-inv-title">' + zqEscapeHtml(title) + '</div>');
    }

    var headers = inv && inv.requestHeaders;
    if (!headers && inv && inv.note) {
        headers = inv.requestHeaders;
    }
    html.push(zqRenderDebugBlock(
        '请求头 Headers（众阳 sign/license 已脱敏）',
        zqFmtJson(headers || {}),
        prefix + 'Hdr'));

    var reqBodyRaw = inv && inv.requestBodyRaw;
    var reqBody = inv && (inv.requestBody != null ? inv.requestBody : inv.requestParams);
    if (reqBody == null && result && result.data && result.data.requestParams) {
        reqBody = result.data.requestParams;
    }
    html.push(zqRenderDebugBlock(
        '入参 Request' + (inv && inv.method === 'GET' ? '（Query / Body）' : ' Body'),
        reqBodyRaw != null ? zqFmtRequestBodyRaw(reqBodyRaw) : zqFmtJson(reqBody),
        prefix + 'Req'));

    var resp = inv && (inv.hisBody != null ? inv.hisBody : inv.responseRaw);
    if (resp == null && result && result.data && result.data.hisBody) {
        resp = result.data.hisBody;
    }
    if (resp == null) {
        resp = result;
    }
    html.push(zqRenderDebugBlock('回参 Response（HIS hisBody 或完整 JSON）', zqFmtJson(resp), prefix + 'Resp'));

    if (result && result.probeInvoke) {
        html.push(zqRenderDebugBlock(
            '服务端合并参数 resolvedParams（雪花 ID 以字符串传入）',
            zqFmtJson(result.probeInvoke.resolvedParams || {}),
            prefix + 'ResP'));
        if (result.probeInvoke.note) {
            html.push('<p class="hint">' + zqEscapeHtml(result.probeInvoke.note) + '</p>');
        }
    }
    if (result && result.mirrorSync) {
        html.push(zqRenderDebugBlock('镜像落库 mirrorSync', zqFmtJson(result.mirrorSync), prefix + 'Mir'));
    }
    if (result && result.data && result.data.cascadeBatch) {
        html.push(zqRenderDebugBlock('链式批次 cascadeBatch', zqFmtJson(result.data.cascadeBatch), prefix + 'Cas'));
    }

    return html.join('');
}

function zqSpdSyncSummary(result) {
    const ms = result && result.mirrorSync;
    if (!ms) return '';
    let s = '镜像落库=' + (ms.mirrorRows != null ? ms.mirrorRows : 0) + '行';
    s += ', SPD主数据=' + (ms.spdRows != null ? ms.spdRows : 0) + '行';
    if (ms.syncBatchNo) s += ', batch=' + ms.syncBatchNo;
    if (ms.mirrorEnabled === false) s += ', [mirror未启用]';
    if (ms.spdSyncEnabled === false) s += ', [spd-master-sync未启用]';
    if (ms.spdDataSourceAvailable === false) s += ', [SPD数据源未启用]';
    if (ms.mirrorSkippedReason) s += ', ' + ms.mirrorSkippedReason;
    if (ms.mirrorError) s += ', 镜像ERR=' + ms.mirrorError;
    if (ms.spdSyncError) s += ', SPD ERR=' + ms.spdSyncError;
    if (ms.spdNote) s += ', ' + ms.spdNote;
    return s;
}

function zqHisSummary(result) {
    const hisBody = result && result.data && result.data.hisBody;
    if (hisBody && typeof hisBody === 'object') {
        let s = 'HIS success=' + hisBody.success + ', code=' + (hisBody.code || '') + ', message=' + (hisBody.message || '');
        if (Array.isArray(hisBody.data)) s += ', data.length=' + hisBody.data.length;
        if (hisBody._probeMerged) {
            if (hisBody._probeMerged.mode === 'allRoleTypes') {
                s += ', roleTypes=' + hisBody._probeMerged.roleTypes + ', mergedRows=' + hisBody._probeMerged.totalRows;
            } else {
                s += ', pages=' + hisBody._probeMerged.pages + ', mergedRows=' + hisBody._probeMerged.totalRows;
            }
        }
        if (result.data && result.data.cascadeBatch) {
            const cb = result.data.cascadeBatch;
            s += ', cascadeBatch=' + (cb.success || 0) + '/' + (cb.requested || 0)
                + ', batchRows=' + (cb.batchRows || 0);
        }
        const spd = zqSpdSyncSummary(result);
        if (spd) s += ' | ' + spd;
        return { text: s, ok: result.code === 200 && hisBody.success === true, hisCode: hisBody.code || '', message: hisBody.message || '' };
    }
    if (result && result.data && result.data.baseUrl) {
        return { text: '环境 activeEnv=' + (result.data.activeEnv || ''), ok: result.code === 200, hisCode: '', message: result.msg || '' };
    }
    const spdOnly = zqSpdSyncSummary(result);
    if (spdOnly) {
        const ok = result && result.code === 200 && !(result.mirrorSync && result.mirrorSync.spdSyncError);
        return { text: spdOnly, ok: ok, hisCode: '', message: (result.mirrorSync && result.mirrorSync.spdSyncError) || (result && result.msg) || '' };
    }
    return { text: 'scminterface code=' + (result && result.code) + ', msg=' + ((result && result.msg) || ''), ok: result && result.code === 200, hisCode: '', message: (result && result.msg) || '' };
}

function zqRecordTest(title, result, elapsedMs) {
    const sum = zqHisSummary(result);
    zqTestLog.push({ time: zqFormatDt(new Date()), api: title, ok: sum.ok, hisCode: sum.hisCode, message: sum.message, elapsedMs: elapsedMs, raw: result });
    zqRefreshChecklist(title, sum.ok);
    zqRenderTestLog();
}

function zqRefreshChecklist(apiTitle, ok) {
    const row = document.querySelector('[data-api="' + apiTitle + '"]');
    if (!row) return;
    const badge = row.querySelector('.chk-status');
    if (!badge) return;
    badge.textContent = ok ? '成功' : '失败';
    badge.className = 'chk-status ' + (ok ? 'pass' : 'fail');
}

function zqInitChecklist() {
    const apis = ['当前环境', '2.1.9 科室', '2.1.12 人员身份', '2.5.44 药品材料字典', '2.5.58 分类字典',
        '2.5.62 供应商', '2.5.63 生产厂商', '2.5.82 SPD合并库存', '2.5.43 药房批次库存', '2.5.102 一级库入退库'];
    const tbody = document.getElementById('checklistBody');
    if (!tbody) return;
    tbody.innerHTML = apis.map(function (api) {
        return '<tr data-api="' + api + '"><td>' + api + '</td><td><span class="chk-status pending">待测</span></td></tr>';
    }).join('');
}

function zqOwnerKeyForResp(apiKey) {
    return String(apiKey).replace(/^mirror_/, '');
}

function zqTabIdForApiKey(apiKey) {
    const k = zqOwnerKeyForResp(apiKey);
    if (k === 'env') return 'tab-guide';
    if (k === 'depts' || k === 'identities') return 'tab-base';
    return 'tab-spd';
}

function zqSwitchTabForApiKey(apiKey) {
    const tabId = zqTabIdForApiKey(apiKey);
    const idx = { 'tab-guide': 0, 'tab-base': 1, 'tab-spd': 2, 'tab-dept-stock': 3, 'tab-bill-push': 4 }[tabId];
    const btn = document.querySelectorAll('.tabs .tab-btn')[idx];
    zqSwitchTab(tabId, btn);
}

function zqScrollToSection(sectionId) {
    const el = document.getElementById(sectionId);
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function zqInitRespMeta() {
    Object.keys(MSUN_PARAM_SCHEMA).forEach(function (apiKey) {
        ZQ_API_RESP_LABELS[apiKey] = MSUN_PARAM_SCHEMA[apiKey].title;
        ZQ_LOG_TITLE_TO_KEY[MSUN_PARAM_SCHEMA[apiKey].logTitle] = apiKey;
    });
}

function zqDetectFormat(text) {
    const s = String(text || '').trim();
    if (!s) return 'text';
    if (s.charAt(0) === '<' && /<\/?[a-zA-Z]/.test(s)) return 'xml';
    if (s.charAt(0) === '{' || s.charAt(0) === '[') {
        try { JSON.parse(s); return 'json'; } catch (e) { /* ignore */ }
    }
    return 'text';
}

function zqResultToText(result) {
    if (result === null || result === undefined) return '';
    if (typeof result === 'string') return result;
    return zqFmtJson(result);
}

function zqRenderJsonTree(value, keyLabel) {
    if (value === null || value === undefined || typeof value !== 'object') {
        let keyHtml = (keyLabel !== undefined && keyLabel !== null)
            ? '<span class="tree-key">' + zqEscapeHtml(String(keyLabel)) + ': </span>' : '';
        if (typeof value === 'string' && zqDetectFormat(value) === 'xml') {
            const xmlHtml = zqRenderXmlTree(value);
            if (xmlHtml) {
                return '<span class="tree-leaf">' + keyHtml +
                    '<span class="format-inline xml">XML</span></span>' +
                    '<div class="tree-nested-xml">' + xmlHtml + '</div>';
            }
        }
        let valHtml;
        if (value === null) valHtml = '<span class="jv-null">null</span>';
        else if (typeof value === 'string') valHtml = '<span class="jv-str">"' + zqEscapeHtml(value) + '"</span>';
        else if (typeof value === 'boolean') valHtml = '<span class="jv-bool">' + value + '</span>';
        else valHtml = '<span class="jv-num">' + zqEscapeHtml(String(value)) + '</span>';
        return '<span class="tree-leaf">' + keyHtml + valHtml + '</span>';
    }
    const isArr = Array.isArray(value);
    const entries = isArr
        ? value.map(function (v, i) { return [i, v]; })
        : Object.keys(value).map(function (k) { return [k, value[k]]; });
    const keyPrefix = (keyLabel !== undefined && keyLabel !== null)
        ? '<span class="tree-key">' + zqEscapeHtml(isArr ? String(keyLabel) : '"' + keyLabel + '"') + ': </span>'
        : '';
    if (!entries.length) {
        return '<span class="tree-leaf">' + keyPrefix + '<span class="jv-null">' + (isArr ? '[]' : '{}') + '</span></span>';
    }
    let html = '<div class="tree-branch">' + keyPrefix +
        '<span class="tree-toggle expanded" onclick="zqTreeToggle(this)">▼</span>' +
        '<span class="tree-brace">' + (isArr ? '[' : '{') + '</span>' +
        '<div class="tree-children">';
    entries.forEach(function (pair) {
        html += '<div class="tree-line">' + zqRenderJsonTree(pair[1], isArr ? pair[0] : pair[0]) + '</div>';
    });
    html += '</div><span class="tree-brace">' + (isArr ? ']' : '}') + '</span></div>';
    return html;
}

function zqRenderXmlNode(node) {
    if (!node || node.nodeType !== 1) return '';
    const childElements = Array.prototype.filter.call(node.childNodes, function (n) { return n.nodeType === 1; });
    const textContent = Array.prototype.filter.call(node.childNodes, function (n) {
        return n.nodeType === 3 && n.textContent.trim();
    }).map(function (n) { return n.textContent.trim(); }).join(' ');
    let html = '<div class="tree-branch xml-node">';
    if (childElements.length) {
        html += '<span class="tree-toggle expanded" onclick="zqTreeToggle(this)">▼</span>';
    }
    html += '<span class="tree-tag">&lt;' + zqEscapeHtml(node.nodeName);
    for (let i = 0; i < node.attributes.length; i++) {
        const a = node.attributes[i];
        html += ' <span class="xml-attr">' + zqEscapeHtml(a.name) + '="' + zqEscapeHtml(a.value) + '"</span>';
    }
    html += '&gt;</span>';
    if (childElements.length) {
        html += '<div class="tree-children">';
        Array.prototype.forEach.call(node.childNodes, function (ch) {
            if (ch.nodeType === 1) html += zqRenderXmlNode(ch);
            else if (ch.nodeType === 3 && ch.textContent.trim()) {
                html += '<span class="xml-text">' + zqEscapeHtml(ch.textContent.trim()) + '</span>';
            }
        });
        html += '</div>';
    } else if (textContent) {
        html += '<span class="xml-text">' + zqEscapeHtml(textContent) + '</span>';
    }
    html += '<span class="tree-tag">&lt;/' + zqEscapeHtml(node.nodeName) + '&gt;</span></div>';
    return html;
}

function zqRenderXmlTree(xmlString) {
    try {
        const parser = new DOMParser();
        const doc = parser.parseFromString(xmlString, 'text/xml');
        if (doc.querySelector('parsererror')) return null;
        if (!doc.documentElement) return null;
        return zqRenderXmlNode(doc.documentElement);
    } catch (e) {
        return null;
    }
}

function zqRenderResponseBody(result) {
    if (typeof result === 'string') {
        const fmt = zqDetectFormat(result);
        if (fmt === 'xml') {
            const xmlHtml = zqRenderXmlTree(result);
            if (xmlHtml) {
                return { format: 'xml', html: '<div class="tree-view">' + xmlHtml + '</div>', text: result };
            }
        }
        if (fmt === 'json') {
            try {
                const parsed = JSON.parse(result);
                return {
                    format: 'json',
                    html: '<div class="tree-view">' + zqRenderJsonTree(parsed) + '</div>',
                    text: zqFmtJson(parsed)
                };
            } catch (e) { /* fall through */ }
        }
        return { format: 'text', html: '<pre class="resp-plain">' + zqEscapeHtml(result) + '</pre>', text: result };
    }
    const text = zqFmtJson(result);
    return {
        format: 'json',
        html: '<div class="tree-view">' + zqRenderJsonTree(result) + '</div>',
        text: text
    };
}

function zqTreeToggle(el) {
    const branch = el.closest('.tree-branch');
    if (!branch) return;
    const children = branch.querySelector(':scope > .tree-children');
    if (!children) return;
    const collapsed = children.classList.toggle('collapsed');
    el.textContent = collapsed ? '▶' : '▼';
    el.classList.toggle('collapsed', collapsed);
}

function zqTreeExpandAll(apiKey) {
    const card = document.getElementById('resp-card-' + apiKey);
    if (!card) return;
    card.querySelectorAll('.tree-children').forEach(function (c) { c.classList.remove('collapsed'); });
    card.querySelectorAll('.tree-toggle').forEach(function (t) {
        t.textContent = '▼';
        t.classList.remove('collapsed');
    });
}

function zqTreeCollapseAll(apiKey) {
    const card = document.getElementById('resp-card-' + apiKey);
    if (!card) return;
    card.querySelectorAll('.tree-children').forEach(function (c) { c.classList.add('collapsed'); });
    card.querySelectorAll('.tree-toggle').forEach(function (t) {
        t.textContent = '▶';
        t.classList.add('collapsed');
    });
}

function zqResponseActionsHtml(apiKey, format) {
    const badge = format ? '<span class="format-badge ' + format + '">' + format.toUpperCase() + '</span>' : '';
    return '<div class="resp-actions">' + badge +
        '<button type="button" class="resp-act-btn" onclick="zqTreeExpandAll(\'' + apiKey + '\')">展开</button>' +
        '<button type="button" class="resp-act-btn" onclick="zqTreeCollapseAll(\'' + apiKey + '\')">折叠</button>' +
        '<button type="button" class="resp-act-btn" onclick="zqSelectResponseJson(\'' + apiKey + '\')">全选</button>' +
        '<button type="button" class="resp-act-btn primary" onclick="zqCopyResponseJson(\'' + apiKey + '\')">复制</button>' +
        '<button type="button" class="resp-act-btn" onclick="zqDownloadResponseJson(\'' + apiKey + '\')">下载</button>' +
        '</div>';
}

function zqDownloadResponseJson(apiKey) {
    const text = (zqResponseStore[apiKey] && zqResponseStore[apiKey].text) ||
        ((document.getElementById('resp-raw-' + apiKey) || {}).value || '');
    if (!text.trim()) {
        alert('暂无回参可下载');
        return;
    }
    const hospitalKey = (msunSelectedHospital && msunSelectedHospital.hospitalKey) || 'hospital';
    const ts = zqFormatDt(new Date()).replace(/[:\s]/g, '-');
    const blob = new Blob([text], { type: 'application/json;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = hospitalKey + '-' + apiKey + '-' + ts + '.json';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    zqSetMeta('已下载: ' + a.download);
}

function zqEmptyRespCardHtml(apiKey, label) {
    return '<div class="api-resp-card empty" id="resp-card-' + apiKey + '" data-api-key="' + apiKey + '">' +
        '<div class="resp-head"><div class="resp-head-row"><div class="resp-head-body"><strong>' +
        zqEscapeHtml(label) + '</strong><span class="api-resp-placeholder"> · 尚未调用</span></div></div></div>' +
        '<div class="api-resp-body"></div></div>';
}

function zqRenderInlineRespSlotContent(ownerKey) {
    const hisLabel = ZQ_API_RESP_LABELS[ownerKey] || ownerKey;
    let html = '<div class="inline-resp-label">排错：Headers / 入参 / 回参</div>' + zqEmptyRespCardHtml(ownerKey, hisLabel);
    if (ownerKey !== 'env') {
        html += '<div class="inline-resp-label mirror">镜像库数据</div>' +
            zqEmptyRespCardHtml(zqMirrorRespKey(ownerKey), '[镜像] ' + hisLabel);
    }
    return html;
}

function zqInitInlineRespSlots() {
    const envSlot = document.getElementById('resp-slot-env');
    if (envSlot) envSlot.innerHTML = zqRenderInlineRespSlotContent('env');
}

function zqFocusResponseCard(apiKey) {
    document.querySelectorAll('.api-resp-card').forEach(function (c) { c.classList.remove('active-card'); });
    const card = document.getElementById('resp-card-' + apiKey);
    if (!card) return;
    card.classList.add('active-card');
    const owner = zqOwnerKeyForResp(apiKey);
    const block = document.getElementById('block-' + owner);
    if (block) {
        block.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } else {
        card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
}

function zqGotoResponse(apiKey) {
    zqSwitchTabForApiKey(apiKey);
    zqFocusResponseCard(apiKey);
}

function zqSelectResponseJson(apiKey) {
    const ta = document.getElementById('resp-raw-' + apiKey);
    if (!ta || !ta.value) {
        alert('暂无回参可选中');
        return;
    }
    ta.style.position = 'fixed';
    ta.style.left = '0';
    ta.style.top = '0';
    ta.style.width = '2px';
    ta.style.height = '2px';
    ta.style.opacity = '0';
    ta.focus();
    ta.select();
}

async function zqCopyResponseJson(apiKey) {
    const text = (zqResponseStore[apiKey] && zqResponseStore[apiKey].text) ||
        ((document.getElementById('resp-raw-' + apiKey) || {}).value || '');
    if (!text.trim()) {
        alert('暂无回参可复制');
        return;
    }
    try {
        if (navigator.clipboard && navigator.clipboard.writeText) {
            await navigator.clipboard.writeText(text);
        } else {
            const ta = document.createElement('textarea');
            ta.value = text;
            ta.style.position = 'fixed';
            ta.style.left = '-9999px';
            document.body.appendChild(ta);
            ta.select();
            document.execCommand('copy');
            document.body.removeChild(ta);
        }
        zqSetMeta((ZQ_API_RESP_LABELS[apiKey] || apiKey) + ' 回参已复制');
    } catch (e) {
        zqSelectResponseJson(apiKey);
        alert('自动复制失败，已全选回参，请按 Ctrl+C 复制');
    }
}

function zqMirrorRespKey(apiKey) {
    return 'mirror_' + apiKey;
}

function zqEnsureRespCard(apiKey, label) {
    let card = document.getElementById('resp-card-' + apiKey);
    if (card) return card;
    const owner = zqOwnerKeyForResp(apiKey);
    const slot = document.getElementById('resp-slot-' + owner);
    if (!slot) return null;
    card = document.createElement('div');
    card.className = 'api-resp-card empty';
    card.id = 'resp-card-' + apiKey;
    card.dataset.apiKey = apiKey;
    card.innerHTML =
        '<div class="resp-head"><div class="resp-head-row"><div class="resp-head-body"><strong>' +
        zqEscapeHtml(label || apiKey) + '</strong><span class="api-resp-placeholder"> · 尚未加载</span></div></div></div>' +
        '<div class="api-resp-body"></div>';
    slot.appendChild(card);
    return card;
}

function zqMirrorSummary(result) {
    const data = result && result.data;
    if (!data || !Array.isArray(data.tables)) return null;
    let totalRows = 0;
    data.tables.forEach(function (t) { totalRows += (t.total || 0); });
    return '镜像表 ' + data.tables.length + ' 张，合计约 ' + totalRows + ' 行（当前页展示见 tables[].rows）';
}

function zqShowResult(apiKey, title, requestLine, result, elapsedMs) {
    zqLastResult = result;
    zqLastApiKey = apiKey;
    if (!String(apiKey).startsWith('mirror_')) {
        zqRecordTest(title, result, elapsedMs);
    }
    zqEnsureRespCard(apiKey, title);
    const card = document.getElementById('resp-card-' + apiKey);
    if (!card) return;
    const sum = zqHisSummary(result);
    const mirrorSum = zqMirrorSummary(result);
    const fullText = zqResultToText(result);
    zqResponseStore[apiKey] = { text: fullText, format: 'json' };
    const headBody =
        '<strong>' + zqEscapeHtml(title) + '</strong> · ' + elapsedMs + 'ms<br>' +
        '<code class="req-line">' + zqEscapeHtml(requestLine) + '</code><br>' +
        '<span class="summary">' + zqEscapeHtml(sum.text) + '</span>' +
        (mirrorSum ? '<br><span class="summary">' + zqEscapeHtml(mirrorSum) + '</span>' : '') +
        (result && result.activeEnv ? '<br><span class="env-tag">环境: ' + result.activeEnv + ' · ' + (result.msunBaseUrl || '') + '</span>' : '');
    card.classList.remove('empty');
    card.innerHTML =
        '<div class="resp-head ' + (sum.ok ? 'ok' : 'err') + '">' +
        '<div class="resp-head-row"><div class="resp-head-body">' + headBody + '</div>' +
        zqResponseActionsHtml(apiKey, 'json') + '</div></div>' +
        '<div class="api-resp-body probe-debug-body"></div>';
    const bodyEl = card.querySelector('.api-resp-body');
    bodyEl.innerHTML = zqBuildDebugPanelsHtml(apiKey, result, requestLine);
    const rawTa = document.createElement('textarea');
    rawTa.className = 'resp-raw-store';
    rawTa.id = 'resp-raw-' + apiKey;
    rawTa.readOnly = true;
    rawTa.value = fullText;
    bodyEl.appendChild(rawTa);
    zqSwitchTabForApiKey(apiKey);
    zqFocusResponseCard(apiKey);
}

function zqRenderTestLog() {
    const el = document.getElementById('testLogBody');
    if (!el) return;
    if (!zqTestLog.length) {
        el.innerHTML = '<tr><td colspan="6" class="empty-hint">暂无记录</td></tr>';
        return;
    }
    el.innerHTML = zqTestLog.map(function (item, idx) {
        return '<tr class="' + (item.ok ? 'log-ok' : 'log-fail') + '">' +
            '<td>' + (idx + 1) + '</td><td>' + zqEscapeHtml(item.time) + '</td><td>' + zqEscapeHtml(item.api) + '</td>' +
            '<td>' + (item.ok ? '成功' : '失败') + '</td>' +
            '<td>' + zqEscapeHtml(item.hisCode || '-') + ' / ' + zqEscapeHtml((item.message || '').substring(0, 40)) + '</td>' +
            '<td><button type="button" class="btn-mini" onclick="zqReplayLog(' + idx + ')">查看</button></td></tr>';
    }).join('');
}

function zqReplayLog(idx) {
    const item = zqTestLog[idx];
    if (!item || !item.raw) return;
    const apiKey = ZQ_LOG_TITLE_TO_KEY[item.api] || zqLastApiKey || 'env';
    zqShowResult(apiKey, item.api, '历史记录 · ' + item.time, item.raw, item.elapsedMs || 0);
}

async function zqInvokeRaw(method, path, queryParams, body, apiKey) {
    if (apiKey && zqIsSchemaApiKey(apiKey)) {
        const src = body || queryParams || {};
        return zqInvokeProbeBackend(apiKey, src, zqGetJsonOverrideRaw(apiKey));
    }
    const qs = queryParams ? zqBuildQuery(queryParams) : '';
    const url = msunHospitalApi() + path + qs;
    const requestLine = method + ' ' + url + (body ? '\nBody: ' + zqFmtJson(body) : '');
    const t0 = Date.now();
    let result;
    try {
        result = method === 'GET' ? await get(url) : await post(url, body || {});
    } catch (e) {
        result = { code: 500, msg: e.message };
    }
    return { result: result, elapsed: Date.now() - t0, requestLine: requestLine };
}

async function zqInvoke(apiKey, title, method, path, queryParams, body) {
    if (!zqCheckLogin()) return null;
    zqSetMeta('请求中: ' + title + '…');
    let pack;
    if (zqIsSchemaApiKey(apiKey)) {
        if (!zqValidateJsonOverrideSyntax(apiKey)) return null;
        pack = await zqInvokeProbeBackend(
            apiKey,
            body || queryParams || zqCollectStringParams(apiKey),
            zqGetJsonOverrideRaw(apiKey));
    } else {
        pack = await zqInvokeRaw(method, path, queryParams, body);
    }
    zqShowResult(apiKey, title, pack.requestLine, pack.result, pack.elapsed);
    zqSetMeta('完成: ' + title);
    return pack.result;
}

function zqExtractHisDataArray(result) {
    const data = result && result.data && result.data.hisBody && result.data.hisBody.data;
    return Array.isArray(data) ? data : null;
}

function zqIsHisOk(result) {
    const hisBody = result && result.data && result.data.hisBody;
    return !!(result && result.code === 200 && hisBody && hisBody.success === true);
}

function zqMaxCursorFromPage(items, field) {
    let maxStr = null;
    let maxBi = null;
    items.forEach(function (item) {
        if (!item || item[field] === undefined || item[field] === null) return;
        const text = String(item[field]).trim();
        if (!text) return;
        try {
            const bi = BigInt(text.replace(/\.\d+$/, ''));
            if (maxBi === null || bi > maxBi) {
                maxBi = bi;
                maxStr = text;
            }
        } catch (e) {
            const num = Number(text);
            if (!isNaN(num) && (maxStr === null || num > Number(maxStr))) {
                maxStr = text;
            }
        }
    });
    return maxStr;
}

function zqSchemaHasField(schema, fieldKey) {
    return !!(schema && schema.fields && schema.fields.some(function (f) { return f.key === fieldKey; }));
}

/** 联调调用策略：materialOrDrug 固定 1；limitCount 暂不传给 HIS（仅表单选填/翻页内部用 defaultPageSize） */
function zqApplySchemaCallPolicy(schema, params) {
    const out = Object.assign({}, params || {});
    if (zqSchemaHasField(schema, 'materialOrDrug')) {
        out.materialOrDrug = '1';
    }
    if (zqSchemaHasField(schema, 'limitCount')) {
        delete out.limitCount;
    }
    return out;
}

function zqShouldOmitLimitCountParam(schema) {
    return zqSchemaHasField(schema, 'limitCount');
}

/** invalidFlag 未填时，分别请求 0 与 1 并合并 */
function zqNeedsInvalidFlagSweep(schema, params) {
    if (!zqSchemaHasField(schema, 'invalidFlag')) return false;
    const v = params.invalidFlag;
    return v === undefined || v === null || String(v).trim() === '';
}

function zqBackfillInvalidFlagOnItems(items, invalidFlag) {
    if (!Array.isArray(items)) return items;
    const flag = String(invalidFlag);
    items.forEach(function (item) {
        if (!item) return;
        if (item.invalidFlag === undefined || item.invalidFlag === null || String(item.invalidFlag).trim() === '') {
            item.invalidFlag = flag;
        }
    });
    return items;
}

function zqBuildInvalidFlagSweepShell(r0, r1) {
    const shell = (r1 && r1.result) ? r1.result : (r0 && r0.result ? r0.result : { code: 500, msg: '无有效回参' });
    const out = JSON.parse(JSON.stringify(shell));
    if (!out.data) out.data = {};
    if (!out.data.hisBody) out.data.hisBody = { success: true, code: '0000', data: [] };
    return out;
}

function zqMergeInvalidFlagSweepResults(apiKey, schema, baseParams, r0, r1) {
    let items0 = [];
    let items1 = [];
    if (r0 && r0.result && zqIsHisOk(r0.result)) {
        items0 = zqExtractHisDataArray(r0.result) || [];
    }
    if (r1 && r1.result && zqIsHisOk(r1.result)) {
        items1 = zqExtractHisDataArray(r1.result) || [];
    }
    zqBackfillInvalidFlagOnItems(items0, '0');
    zqBackfillInvalidFlagOnItems(items1, '1');
    const merged = items0.concat(items1);
    const out = zqBuildInvalidFlagSweepShell(r0, r1);
    out.data.hisBody.data = merged;
    out.data.hisBody.success = merged.length > 0 || zqIsHisOk(r0 && r0.result) || zqIsHisOk(r1 && r1.result);
    out.data.hisBody._probeMerged = {
        mode: 'invalidFlagSweep',
        invalidFlags: ['0', '1'],
        totalRows: merged.length,
        rows0: items0.length,
        rows1: items1.length
    };
    out.data.requestParams = Object.assign({}, baseParams, { materialOrDrug: '1', invalidFlag: '0+1' });
    const elapsed = ((r0 && r0.elapsed) || 0) + ((r1 && r1.elapsed) || 0);
    const invokeUrl = msunHospitalApi() + '/probe/invoke';
    const requestLine = 'POST ' + invokeUrl
        + ' [invalidFlag=0+1 合并]\n参数0: ' + zqFmtJson({ apiKey: apiKey, params: Object.assign({}, baseParams, { invalidFlag: '0' }) })
        + '\n参数1: ' + zqFmtJson({ apiKey: apiKey, params: Object.assign({}, baseParams, { invalidFlag: '1' }) });
    return { result: out, elapsed: elapsed, requestLine: requestLine, rows: merged.length };
}

function zqSetBlockBusy(apiKey, busy) {
    const block = document.getElementById('block-' + apiKey);
    if (!block) return;
    block.querySelectorAll('.btn-call').forEach(function (btn) {
        btn.disabled = !!busy;
    });
}

async function zqCallApiAllRoleTypes(apiKey) {
    if (!zqCheckLogin() || zqFetchingAllRoles || zqFetchingAllPages) return null;
    const schema = MSUN_PARAM_SCHEMA[apiKey];
    if (!schema || !schema.roleTypeSweep) {
        alert(schema ? schema.title + ' 无 roleType 遍历逻辑' : '未知接口');
        return null;
    }

    const deptId = zqGetFieldValue(apiKey, { key: 'deptId' });
    const identityId = zqGetFieldValue(apiKey, { key: 'identityId' });
    const userId = zqGetFieldValue(apiKey, { key: 'userId' });
    if (deptId || identityId || userId) {
        if (!confirm('获取全部用户将仅按 roleType 0~8 遍历，忽略 deptId / identityId / userId。是否继续？')) {
            return null;
        }
    }

    const allPath = schema.roleTypeSweep.path || (schema.path + '/all');
    zqFetchingAllRoles = true;
    zqSetBlockBusy(apiKey, true);
    try {
        zqSetMeta('获取全部用户: 服务端遍历 roleType 0~8…');
        const pack = await zqInvokeRaw(schema.method, allPath, null, null);
        const merged = pack.result;
        const hisBody = merged && merged.data && merged.data.hisBody;
        const totalRows = (hisBody && hisBody._probeMerged && hisBody._probeMerged.totalRows)
            || (hisBody && Array.isArray(hisBody.data) ? hisBody.data.length : 0);
        const title = schema.logTitle + '（全部用户 roleType 0~8 / ' + totalRows + ' 条）';
        zqShowResult(apiKey, title, pack.requestLine, merged, pack.elapsed);
        zqSetMeta('全部用户拉取完成: ' + totalRows + ' 条');
        return merged;
    } finally {
        zqFetchingAllRoles = false;
        zqSetBlockBusy(apiKey, false);
    }
}

async function zqCallApiAllPages(apiKey) {
    if (!zqCheckLogin() || zqFetchingAllPages || zqFetchingAllRoles) return null;
    const schema = MSUN_PARAM_SCHEMA[apiKey];
    if (!schema || !schema.pagination) {
        alert(schema ? schema.title + ' 无翻页逻辑，请使用单页调用' : '未知接口');
        return null;
    }
    const formParams = zqCollectStringParams(apiKey);
    const jsonOverride = zqGetJsonOverrideRaw(apiKey);
    if (!zqValidateRequired(apiKey, formParams)) return null;
    if (!zqValidateJsonOverrideSyntax(apiKey)) return null;
    let baseParams = zqApplySchemaCallPolicy(schema, formParams);
    if (zqNeedsInvalidFlagSweep(schema, baseParams)) {
        return zqCallApiAllPagesInvalidFlagSweep(apiKey, baseParams, schema, jsonOverride);
    }

    const pag = schema.pagination;
    const pageSizeKey = pag.pageSizeKey || (pag.emptyPageBreak ? null : 'limitCount');
    const omitLimitCount = zqShouldOmitLimitCountParam(schema);
    let pageSize = pag.defaultPageSize || 100;
    const cursorParam = pag.cursorParam;
    const cursorField = pag.cursorField || cursorParam;
    const maxPages = pag.maxPages || 500;
    const delayMs = pag.delayMs || 300;

    if (baseParams[cursorParam] !== undefined && baseParams[cursorParam] !== null && String(baseParams[cursorParam]).trim() !== '') {
        if (!confirm('全量拉取将忽略表单中的 ' + cursorParam + '，从首条记录开始翻页。是否继续？')) {
            return null;
        }
    }
    delete baseParams[cursorParam];

    zqFetchingAllPages = true;
    zqSetBlockBusy(apiKey, true);
    let allItems = [];
    let pageNum = 0;
    let cursor = null;
    let lastPack = null;
    let totalElapsed = 0;

    try {
        while (pageNum < maxPages) {
            pageNum++;
            const params = Object.assign({}, baseParams);
            if (pageSizeKey && !(omitLimitCount && pageSizeKey === 'limitCount')) {
                params[pageSizeKey] = String(pageSize);
            }
            if (cursor != null) {
                params[cursorParam] = String(cursor);
            }
            zqSetMeta('全量拉取: ' + schema.title + ' 第 ' + pageNum + ' 页…（已合并 ' + allItems.length + ' 条）');

            const pack = await zqInvokeProbeBackend(apiKey, params, jsonOverride);
            totalElapsed += pack.elapsed;
            lastPack = pack;

            if (!zqIsHisOk(pack.result)) {
                const failTitle = schema.logTitle + '（全量第' + pageNum + '页失败）';
                zqShowResult(apiKey, failTitle, pack.requestLine, pack.result, totalElapsed);
                zqSetMeta('全量拉取失败: ' + failTitle);
                return pack.result;
            }

            const items = zqExtractHisDataArray(pack.result) || [];
            allItems = allItems.concat(items);

            if (pag.emptyPageBreak) {
                if (items.length === 0) {
                    break;
                }
            } else if (items.length < pageSize) {
                break;
            }
            const nextCursor = zqMaxCursorFromPage(items, cursorField);
            if (nextCursor == null || nextCursor === cursor) {
                break;
            }
            cursor = nextCursor;
            if (pageNum < maxPages) {
                await zqSleep(delayMs);
            }
        }

        const merged = JSON.parse(JSON.stringify(lastPack.result));
        merged.data.hisBody.data = allItems;
        merged.data.hisBody._probeMerged = {
            pages: pageNum,
            totalRows: allItems.length,
            pageSize: pageSize,
            cursorParam: cursorParam,
            mode: 'allPages'
        };
        if (lastPack.result && lastPack.result.probeInvoke) {
            merged.probeInvoke = lastPack.result.probeInvoke;
        }
        merged.data.requestParams = Object.assign({}, baseParams);

        const title = schema.logTitle + '（全量 ' + pageNum + ' 页 / ' + allItems.length + ' 条）';
        const requestLine = 'POST ' + msunHospitalApi() + '/probe/invoke [全量翻页 x' + pageNum
            + (omitLimitCount ? ', 未传 limitCount' : ', pageSize=' + pageSize)
            + ', cursor=' + cursorParam + ']\n首屏参数: ' + zqFmtJson({ apiKey: apiKey, params: baseParams, paramsJsonOverride: jsonOverride || undefined });
        zqShowResult(apiKey, title, requestLine, merged, totalElapsed);
        zqSetMeta('全量拉取完成: ' + title);
        return merged;
    } finally {
        zqFetchingAllPages = false;
        zqSetBlockBusy(apiKey, false);
    }
}

async function zqCallApiAllPagesInvalidFlagSweep(apiKey, baseParams, schema, jsonOverride) {
    zqFetchingAllPages = true;
    zqSetBlockBusy(apiKey, true);
    try {
        zqSetMeta('全量拉取: ' + schema.title + '（invalidFlag 0+1）…');
        const r0 = await zqFetchAllPagesSilent(apiKey, Object.assign({}, baseParams, { invalidFlag: '0' }),
            { _skipInvalidFlagSweep: true, showResult: false, jsonOverride: jsonOverride });
        const r1 = await zqFetchAllPagesSilent(apiKey, Object.assign({}, baseParams, { invalidFlag: '1' }),
            { _skipInvalidFlagSweep: true, showResult: false, jsonOverride: jsonOverride });
        const merged = zqMergeInvalidFlagSweepResults(apiKey, schema, baseParams, r0, r1);
        const title = schema.logTitle + '（invalidFlag 0+1 全量 / ' + merged.rows + ' 条）';
        zqShowResult(apiKey, title, merged.requestLine, merged.result, merged.elapsed);
        zqSetMeta('全量拉取完成: ' + title);
        return merged.result;
    } finally {
        zqFetchingAllPages = false;
        zqSetBlockBusy(apiKey, false);
    }
}

function zqSwitchTab(tabId, btn) {
    document.querySelectorAll('.tab-panel').forEach(function (p) { p.classList.remove('active'); });
    document.querySelectorAll('.tabs .tab-btn').forEach(function (b) { b.classList.remove('active'); });
    document.getElementById(tabId).classList.add('active');
    if (btn) btn.classList.add('active');
    if (tabId === 'tab-bill-push' && typeof bpInitBillPushTab === 'function') {
        bpInitBillPushTab();
    }
    if (tabId === 'tab-dept-stock' && typeof dsInitDeptStockTab === 'function') {
        dsInitDeptStockTab();
    }
}

function zqFormatDt(d) {
    const pad = function (n) { return n < 10 ? '0' + n : '' + n; };
    return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' +
        pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds());
}

/** API 时间 yyyy-MM-dd HH:mm:ss → datetime-local 控件值 */
function zqApiDtToLocal(apiStr) {
    if (!apiStr) return '';
    const s = String(apiStr).trim();
    const m = s.match(/^(\d{4}-\d{2}-\d{2})[ T](\d{2}:\d{2})(?::(\d{2}))?/);
    if (!m) return '';
    return m[1] + 'T' + m[2] + ':' + (m[3] || '00');
}

/** datetime-local 控件值 → API 时间 yyyy-MM-dd HH:mm:ss */
function zqLocalDtToApi(localStr) {
    if (!localStr) return '';
    const s = String(localStr).trim();
    if (s.indexOf('T') < 0) return s;
    const parts = s.split('T');
    let time = parts[1] || '00:00:00';
    if (time.length === 5) time += ':00';
    const segs = time.split(':');
    if (segs.length === 2) time += ':00';
    return parts[0] + ' ' + time;
}

function zqSleep(ms) { return new Promise(function (r) { setTimeout(r, ms); }); }

// ---------- 动态表单渲染 ----------
function zqRenderField(apiKey, field) {
    const id = zqFieldId(apiKey, field.key);
    const req = field.required ? ' <span class="req">*</span>' : '';
    let input;
    if (field.options) {
        input = '<select id="' + id + '" data-api="' + apiKey + '" data-key="' + field.key + '">' +
            field.options.map(function (opt) {
                const label = opt === '' ? '（不填）' : opt;
                const sel = (field.defaultValue === opt) ? ' selected' : '';
                return '<option value="' + zqEscapeHtml(opt) + '"' + sel + '>' + zqEscapeHtml(label) + '</option>';
            }).join('') + '</select>';
    } else if (field.inputType === 'datetime') {
        const dv = field.defaultValue !== undefined ? zqApiDtToLocal(field.defaultValue) : '';
        input = '<input type="datetime-local" step="1" id="' + id + '" data-api="' + apiKey + '" data-key="' + field.key + '"'
            + (dv ? ' value="' + zqEscapeHtml(dv) + '"' : '') + '>';
    } else {
        input = '<input type="text" id="' + id + '" data-api="' + apiKey + '" data-key="' + field.key + '"' +
            (field.defaultValue !== undefined ? ' value="' + zqEscapeHtml(field.defaultValue) + '"' : '') + '>';
    }
    return '<div class="field"><label>' + field.label + req + '</label>' + input +
        '<span class="field-hint">' + zqEscapeHtml(field.hint || '') + '</span></div>';
}

function zqRenderApiForm(apiKey, schema) {
    const fieldsHtml = schema.fields.map(function (f) { return zqRenderField(apiKey, f); }).join('');
    const singleLabel = schema.pagination ? '调用（单页）' : '调用';
    let actions = '<button type="button" class="btn-call" onclick="zqCallApi(\'' + apiKey + '\')">' + singleLabel + '</button>';
    if (schema.pagination) {
        actions += '<button type="button" class="btn-call warn" onclick="zqCallApiAllPages(\'' + apiKey + '\')" title="'
            + zqEscapeHtml(schema.pagination.hint || '') + '">获取全部分页</button>';
    }
    if (schema.roleTypeSweep) {
        actions += '<button type="button" class="btn-call warn" onclick="zqCallApiAllRoleTypes(\'' + apiKey + '\')" title="'
            + zqEscapeHtml(schema.roleTypeSweep.hint || '') + '">获取全部用户</button>';
    }
    if (schema.spdMasterSync) {
        actions += '<button type="button" class="btn-call warn" onclick="zqSyncSpdMaster(\'' + apiKey + '\')" '
            + 'title="将镜像库最新批次（或上次调用批次）upsert 至 SPD 主数据表">同步至 SPD 主数据</button>';
    }
    actions += '<button type="button" class="btn-call secondary" onclick="zqViewMirrorData(\'' + apiKey + '\')">查看镜像数据</button>' +
        '<button type="button" class="btn-call secondary" onclick="zqResetApi(\'' + apiKey + '\')">重置</button>' +
        '<button type="button" class="btn-call secondary" onclick="zqSyncJsonFromForm(\'' + apiKey + '\')">表单→JSON</button>';
    if (schema.actions) {
        schema.actions.forEach(function (fn) {
            if (fn === 'zqCallIdentitiesSample') actions += '<button type="button" class="btn-call secondary" onclick="zqCallIdentitiesSample()">自动取首科室</button>';
            if (fn === 'zqFillDeptFromLast') actions += '<button type="button" class="btn-call secondary" onclick="zqFillDeptFromLast()">填充deptId</button>';
            if (fn === 'zqFillFromLastDict') actions += '<button type="button" class="btn-call secondary" onclick="zqFillFromLastDict()">从字典填充批次</button>';
        });
    }
    return '<div class="api-block" id="block-' + apiKey + '">' +
        '<h3>' + zqEscapeHtml(schema.title) + '</h3>' +
        (schema.hint ? '<p class="api-desc">' + zqEscapeHtml(schema.hint) + '</p>' : '') +
        '<div class="form-grid">' + fieldsHtml + '</div>' +
        '<details class="json-editor-wrap"><summary>高级：JSON 入参编辑（原文提交服务端合并，避免 JS 解析大整数丢精度）</summary>' +
        '<textarea id="' + zqJsonId(apiKey) + '" class="json-editor" rows="6" placeholder="{}"></textarea>' +
        '<button type="button" class="btn-call secondary" onclick="zqSyncFormFromJson(\'' + apiKey + '\')">JSON→表单</button></details>' +
        '<div class="tool-row">' + actions + '</div>' +
        '<div class="api-inline-resp" id="resp-slot-' + apiKey + '">' + zqRenderInlineRespSlotContent(apiKey) + '</div></div>';
}

function zqRenderAllForms() {
    document.getElementById('form-depts').innerHTML = zqRenderApiForm('depts', MSUN_PARAM_SCHEMA.depts);
    document.getElementById('form-identities').innerHTML = zqRenderApiForm('identities', MSUN_PARAM_SCHEMA.identities);
    ['drugDict', 'dictCategory', 'suppliers', 'producers', 'mergeStocks', 'batchStocks', 'ykInstock'].forEach(function (key) {
        document.getElementById('form-' + key).innerHTML = zqRenderApiForm(key, MSUN_PARAM_SCHEMA[key]);
    });
}

function zqGetFieldValue(apiKey, field) {
    const key = typeof field === 'object' ? field.key : field;
    const el = document.getElementById(zqFieldId(apiKey, key));
    if (!el) return '';
    const val = el.value.trim();
    if (el.type === 'datetime-local') {
        return zqLocalDtToApi(val);
    }
    if (typeof field === 'object' && field.inputType === 'datetime') {
        return zqLocalDtToApi(val);
    }
    return val;
}

function zqCollectFromForm(apiKey) {
    const schema = MSUN_PARAM_SCHEMA[apiKey];
    const params = {};
    schema.fields.forEach(function (field) {
        const v = zqGetFieldValue(apiKey, field);
        if (v !== '') params[field.key] = v;
    });
    return params;
}

function zqMergeJsonOverride(apiKey, formParams) {
    const ta = document.getElementById(zqJsonId(apiKey));
    if (!ta || !ta.value.trim()) return formParams;
    try {
        const json = JSON.parse(ta.value.trim());
        return Object.assign({}, formParams, json);
    } catch (e) {
        alert('JSON 入参格式错误: ' + e.message);
        return null;
    }
}

function zqValidateRequired(apiKey, params) {
    const schema = MSUN_PARAM_SCHEMA[apiKey];
    const missing = [];
    schema.fields.forEach(function (f) {
        if (f.required && (params[f.key] === undefined || params[f.key] === null || String(params[f.key]).trim() === '')) {
            missing.push(f.key);
        }
    });
    if (missing.length) {
        alert(schema.title + ' 缺少必填参数: ' + missing.join(', '));
        return false;
    }
    return true;
}

function zqSyncJsonFromForm(apiKey) {
    const params = zqCollectFromForm(apiKey);
    document.getElementById(zqJsonId(apiKey)).value = zqFmtJson(params);
}

function zqSyncFormFromJson(apiKey) {
    const ta = document.getElementById(zqJsonId(apiKey));
    if (!ta.value.trim()) return;
    try {
        const json = JSON.parse(ta.value.trim());
        const schema = MSUN_PARAM_SCHEMA[apiKey];
        schema.fields.forEach(function (f) {
            const el = document.getElementById(zqFieldId(apiKey, f.key));
            if (el && json[f.key] !== undefined && json[f.key] !== null) {
                el.value = String(json[f.key]);
            }
        });
    } catch (e) {
        alert('JSON 解析失败: ' + e.message);
    }
}

function zqResetApi(apiKey) {
    const schema = MSUN_PARAM_SCHEMA[apiKey];
    schema.fields.forEach(function (f) {
        const el = document.getElementById(zqFieldId(apiKey, f.key));
        if (!el) return;
        el.value = f.defaultValue !== undefined ? f.defaultValue : '';
    });
    const jsonTa = document.getElementById(zqJsonId(apiKey));
    if (jsonTa) jsonTa.value = '';
    if (apiKey === 'ykInstock') zqInitYkDefaults();
}

function zqSaveAllParams() {
    const data = {};
    Object.keys(MSUN_PARAM_SCHEMA).forEach(function (apiKey) {
        data[apiKey] = { form: zqCollectFromForm(apiKey), json: (document.getElementById(zqJsonId(apiKey)) || {}).value || '' };
    });
    localStorage.setItem(msunLsKey(), JSON.stringify(data));
    zqSetMeta('参数已保存到浏览器本地');
}

function zqLoadAllParams() {
    const raw = localStorage.getItem(msunLsKey());
    if (!raw) return;
    try {
        const data = JSON.parse(raw);
        Object.keys(data).forEach(function (apiKey) {
            if (!MSUN_PARAM_SCHEMA[apiKey]) return;
            const saved = data[apiKey];
            if (saved.form) {
                MSUN_PARAM_SCHEMA[apiKey].fields.forEach(function (f) {
                    const el = document.getElementById(zqFieldId(apiKey, f.key));
                    if (el && saved.form[f.key] !== undefined) {
                        el.value = (f.inputType === 'datetime' || el.type === 'datetime-local')
                            ? zqApiDtToLocal(saved.form[f.key]) : saved.form[f.key];
                    }
                });
            }
            const jsonTa = document.getElementById(zqJsonId(apiKey));
            if (jsonTa && saved.json) jsonTa.value = saved.json;
        });
    } catch (e) { /* ignore */ }
}

function zqInitYkDefaults() {
    const now = new Date();
    const start = zqFormatDt(new Date(now.getTime() - 7 * 86400000));
    const end = zqFormatDt(now);
    const startEl = document.getElementById(zqFieldId('ykInstock', 'startTime'));
    const endEl = document.getElementById(zqFieldId('ykInstock', 'endTime'));
    if (startEl && !startEl.value) startEl.value = zqApiDtToLocal(start);
    if (endEl && !endEl.value) endEl.value = zqApiDtToLocal(end);
    const faStart = document.getElementById('fetchAll_startTime');
    const faEnd = document.getElementById('fetchAll_endTime');
    if (faStart && !faStart.value) faStart.value = zqApiDtToLocal(start);
    if (faEnd && !faEnd.value) faEnd.value = zqApiDtToLocal(end);
}

function zqGetFetchAllTimes() {
    const startEl = document.getElementById('fetchAll_startTime');
    const endEl = document.getElementById('fetchAll_endTime');
    return {
        startTime: zqLocalDtToApi((startEl && startEl.value) || ''),
        endTime: zqLocalDtToApi((endEl && endEl.value) || '')
    };
}

function zqSyncFetchAllTimesToYk() {
    const t = zqGetFetchAllTimes();
    zqSetField('ykInstock', 'startTime', t.startTime);
    zqSetField('ykInstock', 'endTime', t.endTime);
}

function zqBuildDrugSpecLookup(dictResult) {
    const map = {};
    const items = zqExtractHisDataArray(dictResult) || [];
    items.forEach(function (item) {
        if (!item || item.drugId === undefined || item.drugId === null) return;
        const drugId = String(item.drugId);
        let specId = item.drugSpecPackingId;
        if ((specId === undefined || specId === null || specId === '') && item.specPackingList && item.specPackingList.length) {
            specId = item.specPackingList[0].drugSpecPackingId;
        }
        if (specId !== undefined && specId !== null && String(specId).trim() !== '') {
            map[drugId] = String(specId);
        }
    });
    return map;
}

function zqExtractStockKeysFromYkInstock(ykResult, drugSpecLookup) {
    const headers = zqExtractHisDataArray(ykResult) || [];
    const seen = {};
    const keys = [];
    headers.forEach(function (header) {
        if (!header) return;
        const deptId = header.deptId != null ? String(header.deptId) : '';
        const details = header.stockDetailList || [];
        details.forEach(function (detail) {
            if (!detail || detail.drugId === undefined || detail.drugId === null) return;
            const drugId = String(detail.drugId);
            let drugSpecPackingId = detail.drugSpecPackingId != null ? String(detail.drugSpecPackingId) : '';
            if (!drugSpecPackingId && drugSpecLookup && drugSpecLookup[drugId]) {
                drugSpecPackingId = drugSpecLookup[drugId];
            }
            const mapKey = deptId + '|' + drugId + '|' + drugSpecPackingId;
            if (seen[mapKey]) return;
            seen[mapKey] = true;
            keys.push({ deptId: deptId, drugId: drugId, drugSpecPackingId: drugSpecPackingId });
        });
    });
    return keys;
}

async function zqFetchAllPagesSilent(apiKey, paramOverrides, options) {
    options = options || {};
    const schema = MSUN_PARAM_SCHEMA[apiKey];
    if (!schema) return { ok: false, result: null, rows: 0, message: '未知接口' };

    let jsonOverride = options.jsonOverride != null ? options.jsonOverride : null;
    let baseParams;
    if (options.useOverridesOnly) {
        baseParams = Object.assign({}, paramOverrides || {});
    } else {
        if (!zqValidateJsonOverrideSyntax(apiKey)) {
            return { ok: false, result: null, rows: 0, message: 'JSON 入参错误' };
        }
        baseParams = Object.assign({}, zqCollectStringParams(apiKey), paramOverrides || {});
        jsonOverride = zqGetJsonOverrideRaw(apiKey);
    }
    baseParams = zqApplySchemaCallPolicy(schema, baseParams);
    Object.keys(baseParams).forEach(function (k) {
        const v = baseParams[k];
        if (v === undefined || v === null || String(v).trim() === '') delete baseParams[k];
    });

    if (!options._skipInvalidFlagSweep && zqNeedsInvalidFlagSweep(schema, baseParams)) {
        const r0 = await zqFetchAllPagesSilent(apiKey, Object.assign({}, baseParams, { invalidFlag: '0' }),
            Object.assign({}, options, { useOverridesOnly: true, _skipInvalidFlagSweep: true, showResult: false }));
        const r1 = await zqFetchAllPagesSilent(apiKey, Object.assign({}, baseParams, { invalidFlag: '1' }),
            Object.assign({}, options, { useOverridesOnly: true, _skipInvalidFlagSweep: true, showResult: false }));
        const merged = zqMergeInvalidFlagSweepResults(apiKey, schema, baseParams, r0, r1);
        const title = schema.logTitle + '（invalidFlag 0+1 全量 / ' + merged.rows + ' 条）';
        if (options.showResult !== false) {
            zqShowResult(apiKey, title, merged.requestLine, merged.result, merged.elapsed);
        }
        return {
            ok: merged.rows > 0 || zqIsHisOk(merged.result),
            result: merged.result,
            rows: merged.rows,
            elapsed: merged.elapsed,
            requestLine: merged.requestLine,
            message: ''
        };
    }

    if (!schema.pagination) {
        const pack = await zqInvokeProbeBackend(apiKey, baseParams, jsonOverride);
        const rows = (zqExtractHisDataArray(pack.result) || []).length;
        if (options.showResult !== false) {
            zqShowResult(apiKey, schema.logTitle + '（全量 1 页 / ' + rows + ' 条）', pack.requestLine, pack.result, pack.elapsed);
        }
        return {
            ok: zqIsHisOk(pack.result),
            result: pack.result,
            rows: rows,
            elapsed: pack.elapsed,
            requestLine: pack.requestLine,
            message: ''
        };
    }

    const pag = schema.pagination;
    const pageSizeKey = pag.pageSizeKey || (pag.emptyPageBreak ? null : 'limitCount');
    const omitLimitCount = zqShouldOmitLimitCountParam(schema);
    let pageSize = pag.defaultPageSize || 100;
    const cursorParam = pag.cursorParam;
    const cursorField = pag.cursorField || cursorParam;
    const maxPages = options.maxPages || pag.maxPages || 500;
    const delayMs = pag.delayMs || 300;
    delete baseParams[cursorParam];

    let allItems = [];
    let pageNum = 0;
    let cursor = null;
    let lastPack = null;
    let totalElapsed = 0;
    let truncated = false;
    let lastPageSize = 0;

    while (pageNum < maxPages) {
        pageNum++;
        const params = Object.assign({}, baseParams);
        if (pageSizeKey && !(omitLimitCount && pageSizeKey === 'limitCount')) {
            params[pageSizeKey] = String(pageSize);
        }
        if (cursor != null) params[cursorParam] = String(cursor);
        zqSetMeta('全量拉取: ' + schema.title + ' 第 ' + pageNum + ' 页…（已合并 ' + allItems.length + ' 条）');

        const pack = await zqInvokeProbeBackend(apiKey, params, jsonOverride);
        totalElapsed += pack.elapsed;
        lastPack = pack;

        if (!zqIsHisOk(pack.result)) {
            if (options.showResult !== false) {
                zqShowResult(apiKey, schema.logTitle + '（全量第' + pageNum + '页失败）', pack.requestLine, pack.result, totalElapsed);
            }
            return { ok: false, result: pack.result, rows: allItems.length, elapsed: totalElapsed, message: '第' + pageNum + '页失败' };
        }

        const items = zqExtractHisDataArray(pack.result) || [];
        lastPageSize = items.length;
        allItems = allItems.concat(items);

        if (pag.emptyPageBreak) {
            if (items.length === 0) break;
        } else if (items.length < pageSize) {
            break;
        }
        const nextCursor = zqMaxCursorFromPage(items, cursorField);
        if (nextCursor == null || nextCursor === cursor) {
            if (items.length >= pageSize) truncated = true;
            break;
        }
        cursor = nextCursor;
        if (pageNum < maxPages) await zqSleep(delayMs);
    }
    if (pageNum >= maxPages && lastPageSize >= pageSize) truncated = true;

    const merged = JSON.parse(JSON.stringify(lastPack.result));
    merged.data.hisBody.data = allItems;
    merged.data.hisBody._probeMerged = {
        pages: pageNum,
        totalRows: allItems.length,
        pageSize: pageSize,
        cursorParam: cursorParam,
        mode: 'allPages',
        truncated: truncated
    };
    if (lastPack.result && lastPack.result.probeInvoke) {
        merged.probeInvoke = lastPack.result.probeInvoke;
    }
    merged.data.requestParams = Object.assign({}, baseParams);

    let title = schema.logTitle + '（全量 ' + pageNum + ' 页 / ' + allItems.length + ' 条）';
    if (truncated) title += ' [可能未拉全，请检查游标或增大 maxPages]';
    const requestLine = 'POST ' + msunHospitalApi() + '/probe/invoke [全量翻页 x' + pageNum
        + (omitLimitCount ? ', 未传 limitCount' : ', pageSize=' + pageSize)
        + ', cursor=' + cursorParam + (truncated ? ', truncated' : '') + ']\n首屏参数: ' + zqFmtJson({ apiKey: apiKey, params: baseParams, paramsJsonOverride: jsonOverride || undefined });
    if (options.showResult !== false) {
        zqShowResult(apiKey, title, requestLine, merged, totalElapsed);
    }
    return {
        ok: true,
        result: merged,
        rows: allItems.length,
        elapsed: totalElapsed,
        requestLine: requestLine,
        message: truncated ? '翻页可能未完整结束' : '',
        truncated: truncated
    };
}

async function zqFetchAllData() {
    if (zqRunningFetchAll || zqRunningAll || zqFetchingAllPages || zqFetchingAllRoles || !zqCheckLogin()) return;
    const times = zqGetFetchAllTimes();
    if (!times.startTime || !times.endTime) {
        alert('请填写出退库查询的开始时间与结束时间');
        return;
    }
    let stockLimit = parseInt((document.getElementById('fetchAll_stockLimit') || {}).value || '30', 10);
    if (isNaN(stockLimit) || stockLimit < 1) stockLimit = 30;
    if (stockLimit > 200) stockLimit = 200;

    if (!confirm('将按顺序拉取全部数据（含自动翻页），出退库区间：\n' + times.startTime + ' ~ ' + times.endTime
        + '\n供应商/厂商/耗材字典均 materialOrDrug=1（仅耗材）；请求不传 limitCount；'
        + '库存最多查询 ' + stockLimit + ' 组。继续？')) {
        return;
    }

    zqRunningFetchAll = true;
    zqSyncFetchAllTimesToYk();
    const btn = document.getElementById('btnFetchAll');
    const btnRun = document.getElementById('btnRunAll');
    if (btn) btn.disabled = true;
    if (btnRun) btnRun.disabled = true;
    zqTestLog = [];
    zqInitChecklist();
    zqRenderTestLog();

    const materialMasterParams = { materialOrDrug: '1' };
    const materialMasterFetchOpts = { useOverridesOnly: true, maxPages: 2000 };
    const drugDictParams = { materialOrDrug: '1' };
    const drugDictFetchOpts = { useOverridesOnly: true, maxPages: 2000 };
    let drugSpecLookup = {};
    let stockKeys = [];

    try {
        zqSetMeta('① 科室…');
        const deptPack = await zqInvokeProbeBackend('depts', { invalidFlag: '-1' }, null);
        zqShowResult('depts', '2.1.9 科室（全量 invalidFlag=-1）', deptPack.requestLine, deptPack.result, deptPack.elapsed);
        if (!zqIsHisOk(deptPack.result)) {
            alert('科室查询失败，已中止');
            return;
        }
        const mirrorDeptId = await zqFetchFirstDeptIdFromMirror();
        if (mirrorDeptId) {
            zqApplyDeptIdToForms(mirrorDeptId);
        }
        await zqSleep(300);

        zqSetMeta('② 人员身份（roleType 0~8 全量）…');
        const idPack = await zqInvokeRaw('GET', '/identities/all', null, null);
        zqShowResult('identities', '2.1.12 人员身份（获取全部数据）', idPack.requestLine, idPack.result, idPack.elapsed);
        if (!zqIsHisOk(idPack.result)) {
            alert('人员身份拉取失败，已中止');
            return;
        }
        await zqSleep(300);

        zqSetMeta('③ 分类字典（全量翻页，不传 keyWord）…');
        const catRes = await zqFetchAllPagesSilent('dictCategory', {}, { useOverridesOnly: true });
        if (!catRes.ok) { alert('分类字典拉取失败，已中止'); return; }
        await zqSleep(300);

        zqSetMeta('④ 供应商（materialOrDrug=1 全量翻页）…');
        const supRes = await zqFetchAllPagesSilent('suppliers', materialMasterParams, materialMasterFetchOpts);
        if (!supRes.ok) { alert('供应商拉取失败，已中止'); return; }
        await zqSleep(300);

        zqSetMeta('⑤ 生产厂商（materialOrDrug=1 全量翻页）…');
        const prodRes = await zqFetchAllPagesSilent('producers', materialMasterParams, materialMasterFetchOpts);
        if (!prodRes.ok) { alert('生产厂商拉取失败，已中止'); return; }
        await zqSleep(300);

        zqSetMeta('⑥ 耗材字典（materialOrDrug=1，不传 limitCount 翻页）…');
        const dictRes = await zqFetchAllPagesSilent('drugDict', drugDictParams, drugDictFetchOpts);
        if (!dictRes.ok) { alert('耗材字典拉取失败，已中止'); return; }
        if (dictRes.truncated) {
            zqRecordSkipped('2.5.44 耗材字典', '翻页可能未完整结束，已合并 ' + dictRes.rows + ' 条');
        }
        drugSpecLookup = zqBuildDrugSpecLookup(dictRes.result);
        await zqSleep(300);

        zqSetMeta('⑦ 出退库单据明细…');
        const ykBody = {
            startTime: times.startTime,
            endTime: times.endTime
        };
        const ykDept = zqGetFieldValue('ykInstock', { key: 'deptId' });
        if (ykDept) ykBody.deptId = ykDept;
        const ykType = zqGetFieldValue('ykInstock', { key: 'type' });
        if (ykType) ykBody.type = ykType;
        const ykCode = zqGetFieldValue('ykInstock', { key: 'instockCode' });
        if (ykCode) ykBody.instockCode = ykCode;
        const ykPack = await zqInvokeProbeBackend('ykInstock', ykBody, null);
        zqShowResult('ykInstock', '2.5.102 一级库入退库（获取全部数据）', ykPack.requestLine, ykPack.result, ykPack.elapsed);
        if (!zqIsHisOk(ykPack.result)) {
            alert('出退库查询失败，已中止');
            return;
        }
        stockKeys = zqExtractStockKeysFromYkInstock(ykPack.result, drugSpecLookup);
        if (!stockKeys.length) {
            zqRecordSkipped('2.5.82/2.5.43 库存', '出退库明细无 drugId 或未匹配到规格');
            alert('出退库无可用明细行，已跳过汇总/明细库存');
            zqSwitchTab('tab-guide', document.querySelector('.tabs .tab-btn'));
            zqScrollToSection('guide-test-log');
            return;
        }
        await zqSleep(300);

        const toQuery = stockKeys.slice(0, stockLimit);
        if (stockKeys.length > stockLimit) {
            zqRecordSkipped('库存查询', '明细去重 ' + stockKeys.length + ' 组，仅查前 ' + stockLimit + ' 组');
        }

        for (let i = 0; i < toQuery.length; i++) {
            const key = toQuery[i];
            zqSetField('mergeStocks', 'deptId', key.deptId);
            zqSetField('mergeStocks', 'drugId', key.drugId);
            if (key.drugSpecPackingId) zqSetField('mergeStocks', 'drugSpecPackingId', key.drugSpecPackingId);
            zqSetField('mergeStocks', 'cascadeBatch', 'false');

            zqSetMeta('⑧ 汇总库存 ' + (i + 1) + '/' + toQuery.length + ' dept=' + key.deptId + ' drug=' + key.drugId + '…');
            const mergeRes = await zqFetchAllPagesSilent('mergeStocks', {
                deptId: key.deptId,
                drugId: key.drugId,
                drugSpecPackingId: key.drugSpecPackingId || undefined,
                cascadeBatch: 'false'
            }, { showResult: i === 0 || i === toQuery.length - 1 });
            if (!mergeRes.ok) {
                zqRecordSkipped('2.5.82 汇总库存', '第' + (i + 1) + '组失败 dept=' + key.deptId);
            }
            await zqSleep(250);

            if (key.deptId && key.drugId && key.drugSpecPackingId) {
                zqSetField('batchStocks', 'deptId', key.deptId);
                zqSetField('batchStocks', 'drugId', key.drugId);
                zqSetField('batchStocks', 'drugSpecPackingId', key.drugSpecPackingId);
                zqSetMeta('⑨ 明细库存 ' + (i + 1) + '/' + toQuery.length + '…');
                const batchPack = await zqInvokeProbeBackend('batchStocks', {
                    deptId: key.deptId,
                    drugId: key.drugId,
                    drugSpecPackingId: key.drugSpecPackingId
                }, null);
                if (i === 0 || i === toQuery.length - 1) {
                    zqShowResult('batchStocks',
                        '2.5.43 明细库存（' + (i + 1) + '/' + toQuery.length + '）',
                        batchPack.requestLine, batchPack.result, batchPack.elapsed);
                } else {
                    const sum = zqHisSummary(batchPack.result);
                    zqTestLog.push({
                        time: zqFormatDt(new Date()),
                        api: '2.5.43 明细库存 #' + (i + 1),
                        ok: sum.ok,
                        hisCode: sum.hisCode,
                        message: sum.message,
                        elapsedMs: batchPack.elapsed,
                        raw: batchPack.result
                    });
                    zqRenderTestLog();
                }
            } else {
                zqRecordSkipped('2.5.43 明细库存 #' + (i + 1), '缺少 drugSpecPackingId（产品档案未匹配）');
            }
            await zqSleep(250);
        }

        zqSetMeta('获取全部数据完成：出退库明细 ' + stockKeys.length + ' 组，已查库存 ' + toQuery.length + ' 组');
        zqSwitchTab('tab-guide', document.querySelector('.tabs .tab-btn'));
        zqScrollToSection('guide-test-log');
    } finally {
        zqRunningFetchAll = false;
        if (btn) btn.disabled = false;
        if (btnRun) btnRun.disabled = false;
    }
}

async function zqSyncSpdMaster(apiKey) {
    if (!zqCheckLogin()) return null;
    const schema = MSUN_PARAM_SCHEMA[apiKey];
    if (!schema || !schema.spdMasterSync) {
        alert('该接口不支持 SPD 主数据同步');
        return null;
    }
    let batchNo = '';
    if (zqLastApiKey === apiKey && zqLastResult && zqLastResult.mirrorSync && zqLastResult.mirrorSync.syncBatchNo) {
        batchNo = zqLastResult.mirrorSync.syncBatchNo;
    }
    const qs = batchNo ? '?batchNo=' + encodeURIComponent(batchNo) : '';
    const path = '/mirror/spd-sync/' + apiKey + qs;
    const url = msunHospitalApi() + path;
    zqSetMeta('SPD 主数据同步: ' + schema.title + '…');
    const t0 = Date.now();
    let result;
    try {
        result = await post(url, {});
    } catch (e) {
        result = { code: 500, msg: e.message };
    }
    const elapsed = Date.now() - t0;
    const title = '[SPD同步] ' + schema.logTitle;
    zqShowResult(apiKey, title, 'POST ' + url, result, elapsed);
    zqSetMeta('完成: ' + title);
    return result;
}

async function zqViewMirrorData(apiKey) {
    if (!zqCheckLogin()) return null;
    const schema = MSUN_PARAM_SCHEMA[apiKey];
    if (!schema) return null;
    const url = msunHospitalApi() + '/mirror/data/' + apiKey + '?limit=50&offset=0';
    zqSetMeta('查询镜像库: ' + schema.title + '…');
    const t0 = Date.now();
    let result;
    try {
        result = await get(url);
    } catch (e) {
        result = { code: 500, msg: e.message };
    }
    const elapsed = Date.now() - t0;
    const mirrorKey = zqMirrorRespKey(apiKey);
    const title = '[镜像] ' + schema.logTitle;
    zqShowResult(mirrorKey, title, 'GET ' + url, result, elapsed);
    zqSetMeta('完成: ' + title);
    return result;
}

async function zqCallApi(apiKey) {
    if (!zqCheckLogin()) return null;
    const schema = MSUN_PARAM_SCHEMA[apiKey];
    const formParams = zqCollectStringParams(apiKey);
    const jsonOverride = zqGetJsonOverrideRaw(apiKey);
    if (!zqValidateRequired(apiKey, formParams)) return null;
    if (!zqValidateJsonOverrideSyntax(apiKey)) return null;
    let params = zqApplySchemaCallPolicy(schema, formParams);
    if (zqNeedsInvalidFlagSweep(schema, params)) {
        zqSetMeta('请求中: ' + schema.logTitle + '（invalidFlag 0+1）…');
        const pack0 = await zqInvokeProbeBackend(apiKey, Object.assign({}, params, { invalidFlag: '0' }), jsonOverride);
        const pack1 = await zqInvokeProbeBackend(apiKey, Object.assign({}, params, { invalidFlag: '1' }), jsonOverride);
        const merged = zqMergeInvalidFlagSweepResults(apiKey, schema, params, pack0, pack1);
        const title = schema.logTitle + '（invalidFlag 0+1 / ' + merged.rows + ' 条）';
        zqShowResult(apiKey, title, merged.requestLine, merged.result, merged.elapsed);
        zqSetMeta('完成: ' + title);
        return merged.result;
    }
    zqSetMeta('请求中: ' + schema.logTitle + '…');
    const pack = await zqInvokeProbeBackend(apiKey, params, jsonOverride);
    zqShowResult(apiKey, schema.logTitle, pack.requestLine, pack.result, pack.elapsed);
    zqSetMeta('完成: ' + schema.logTitle);
    return pack.result;
}

async function zqLoadEnv() {
    return zqInvoke('env', '当前环境', 'GET', '/env', null, null);
}

function zqCallDepts() { return zqCallApi('depts'); }
function zqCallIdentities() { return zqCallApi('identities'); }
function zqCallDrugDict() { return zqCallApi('drugDict'); }
function zqCallDictCategory() { return zqCallApi('dictCategory'); }
function zqCallSuppliers() { return zqCallApi('suppliers'); }
function zqCallProducers() { return zqCallApi('producers'); }
function zqCallMergeStocks() { return zqCallApi('mergeStocks'); }
function zqCallBatchStocks() { return zqCallApi('batchStocks'); }
function zqCallYkInstock() { return zqCallApi('ykInstock'); }

async function zqCallIdentitiesSample() {
    const roleType = zqGetFieldValue('identities', { key: 'roleType' }) || '0';
    return zqInvoke('identities', '2.1.12 人员身份', 'GET', '/identities/sample', { roleType: roleType }, null);
}

function zqSetField(apiKey, fieldKey, value) {
    const el = document.getElementById(zqFieldId(apiKey, fieldKey));
    if (el && value !== undefined && value !== null) {
        el.value = el.type === 'datetime-local' ? zqApiDtToLocal(value) : String(value);
    }
}

function zqFillFromLastDict() {
    if (!zqLastResult || !zqLastResult.data || !zqLastResult.data.hisBody) {
        alert('请先调用 2.5.44 药品材料字典'); return;
    }
    const data = zqLastResult.data.hisBody.data;
    if (!Array.isArray(data) || !data.length) { alert('字典 data 为空'); return; }
    const first = data[0];
    zqSetField('mergeStocks', 'drugId', first.drugId);
    zqSetField('batchStocks', 'drugId', first.drugId);
    if (first.drugSpecPackingId) {
        zqSetField('mergeStocks', 'drugSpecPackingId', first.drugSpecPackingId);
        zqSetField('batchStocks', 'drugSpecPackingId', first.drugSpecPackingId);
    } else if (first.specPackingList && first.specPackingList.length) {
        zqSetField('mergeStocks', 'drugSpecPackingId', first.specPackingList[0].drugSpecPackingId);
        zqSetField('batchStocks', 'drugSpecPackingId', first.specPackingList[0].drugSpecPackingId);
    }
    zqSwitchTab('tab-spd', document.querySelectorAll('.tabs .tab-btn')[2]);
    alert('已填充 batchStocks 的 drugId / drugSpecPackingId');
}

async function zqFillDeptFromLast() {
    const mirrorDeptId = await zqFetchFirstDeptIdFromMirror();
    if (mirrorDeptId) {
        zqApplyDeptIdToForms(mirrorDeptId);
        alert('已从镜像库填充 deptId=' + mirrorDeptId);
        return;
    }
    alert('请先调用 2.1.9 科室接口（镜像库暂无科室数据）');
}

function zqFillFromLastDictSilent(dictRes) {
    const data = dictRes.data && dictRes.data.hisBody && dictRes.data.hisBody.data;
    if (!Array.isArray(data) || !data.length) return;
    const first = data[0];
    zqSetField('mergeStocks', 'drugId', first.drugId);
    zqSetField('batchStocks', 'drugId', first.drugId);
    if (first.drugSpecPackingId) {
        zqSetField('mergeStocks', 'drugSpecPackingId', first.drugSpecPackingId);
        zqSetField('batchStocks', 'drugSpecPackingId', first.drugSpecPackingId);
    } else if (first.specPackingList && first.specPackingList.length) {
        zqSetField('mergeStocks', 'drugSpecPackingId', first.specPackingList[0].drugSpecPackingId);
        zqSetField('batchStocks', 'drugSpecPackingId', first.specPackingList[0].drugSpecPackingId);
    }
}

function zqRecordSkipped(api, reason) {
    zqTestLog.push({ time: zqFormatDt(new Date()), api: api, ok: false, hisCode: 'SKIP', message: reason, elapsedMs: 0, raw: { skipped: true } });
    zqRefreshChecklist(api, false);
    zqRenderTestLog();
}

async function zqRunAllTests() {
    if (zqRunningAll || zqRunningFetchAll || !zqCheckLogin()) return;
    if (!confirm('按推荐顺序测试全部查询接口？')) return;
    zqRunningAll = true;
    zqTestLog = [];
    zqInitChecklist();
    zqRenderTestLog();
    const btn = document.getElementById('btnRunAll');
    const btnFetch = document.getElementById('btnFetchAll');
    if (btn) btn.disabled = true;
    if (btnFetch) btnFetch.disabled = true;
    try {
        await zqLoadEnv(); await zqSleep(400);
        await zqCallDepts(); await zqSleep(400);
        const mirrorDeptId = await zqFetchFirstDeptIdFromMirror();
        if (mirrorDeptId) {
            zqApplyDeptIdToForms(mirrorDeptId);
        }
        await zqCallIdentitiesSample(); await zqSleep(400);
        const dictRes = await zqCallDrugDict(); await zqSleep(400);
        if (dictRes) zqFillFromLastDictSilent(dictRes);
        await zqCallDictCategory(); await zqSleep(400);
        await zqCallSuppliers(); await zqSleep(400);
        await zqCallProducers(); await zqSleep(400);
        const ms = zqCollectFromForm('mergeStocks');
        if (ms.deptId) {
            await zqCallMergeStocks();
            await zqSleep(400);
        } else {
            zqRecordSkipped('2.5.82 SPD合并库存', '缺少 deptId，已跳过');
        }
        const bs = zqCollectFromForm('batchStocks');
        if (bs.deptId && bs.drugId && bs.drugSpecPackingId) { await zqCallBatchStocks(); await zqSleep(400); }
        else if (!ms.deptId || String(ms.cascadeBatch || 'true') === 'false') {
            zqRecordSkipped('2.5.43 药房批次库存', '缺少三要素且未由2.5.82链式触发，已跳过');
        }
        await zqCallYkInstock();
        zqSwitchTab('tab-guide', document.querySelector('.tabs .tab-btn'));
        zqScrollToSection('guide-test-log');
    } finally {
        zqRunningAll = false;
        if (btn) btn.disabled = false;
        if (btnFetch) btnFetch.disabled = false;
    }
}

function zqExportFeedback() {
    const tester = (document.getElementById('fb_tester') || {}).value || '（未填写）';
    const note = (document.getElementById('fb_note') || {}).value || '';
    let env = (zqLastResult && zqLastResult.activeEnv) || '未知';
    const hospitalLabel = msunSelectedHospital
        ? (msunSelectedHospital.vendorName + ' / ' + msunSelectedHospital.hospitalName)
        : '众阳 / 枣强县中医院';
    const lines = ['【众阳HIS接口测试反馈】', '厂家/医院：' + hospitalLabel, '测试人：' + tester, '测试时间：' + zqFormatDt(new Date()),
        '环境：' + env, '前置机：' + window.location.host, '', '| 序号 | 接口 | 结果 | HIS code | message |',
        '|------|------|------|----------|---------|'];
    zqTestLog.forEach(function (item, idx) {
        lines.push('| ' + (idx + 1) + ' | ' + item.api + ' | ' + (item.ok ? '成功' : '失败') +
            ' | ' + (item.hisCode || '-') + ' | ' + (item.message || '').replace(/\|/g, '/') + ' |');
    });
    lines.push('', '补充说明：' + (note || '无'), '', '--- 最近回参 ---', zqFmtJson(zqLastResult));
    const ta = document.getElementById('feedbackExport');
    if (ta) { ta.value = lines.join('\n'); ta.style.display = 'block'; }
    zqSwitchTab('tab-guide', document.querySelector('.tabs .tab-btn'));
    zqScrollToSection('guide-feedback-section');
}

function zqCopyFeedback() {
    if (!(document.getElementById('feedbackExport') || {}).value) zqExportFeedback();
    const ta = document.getElementById('feedbackExport');
    ta.select();
    document.execCommand('copy');
    alert('已复制到剪贴板');
}

async function msunLoadHospitals() {
    const sel = document.getElementById('hospitalSelect');
    if (!sel) return;
    try {
        const res = await get(MSUN_VENDOR_API + '/hospitals');
        const vendorName = (res && res.data && res.data.vendorName) || '众阳健康';
        const list = res && res.data && res.data.hospitals ? res.data.hospitals : [];
        msunHospitals = list.filter(function (h) { return h.enabled; }).map(function (h) {
            h.vendorName = vendorName;
            return h;
        });
        if (!msunHospitals.length) {
            msunHospitals = [{
                vendorName: '众阳健康',
                hospitalKey: 'zaoqiang-tcm-001',
                hospitalName: '枣强县中医院',
                apiPrefix: '/api/vendor/msun/hospitals/zaoqiang-tcm-001',
                enabled: true
            }];
        }
        sel.innerHTML = msunHospitals.map(function (h) {
            return '<option value="' + h.hospitalKey + '">' + h.hospitalName + '</option>';
        }).join('');
        const saved = localStorage.getItem('msun_selected_hospital');
        const initial = msunHospitals.find(function (h) { return h.hospitalKey === saved; }) || msunHospitals[0];
        msunSelectHospital(initial.hospitalKey, false);
    } catch (e) {
        msunHospitals = [{
            vendorName: '众阳健康',
            hospitalKey: 'zaoqiang-tcm-001',
            hospitalName: '枣强县中医院',
            apiPrefix: '/api/vendor/msun/hospitals/zaoqiang-tcm-001',
            enabled: true
        }];
        sel.innerHTML = '<option value="zaoqiang-tcm-001">枣强县中医院</option>';
        msunSelectHospital('zaoqiang-tcm-001', false);
    }
}

function msunSelectHospital(hospitalKey, reloadEnv) {
    msunSelectedHospital = msunHospitals.find(function (h) { return h.hospitalKey === hospitalKey; }) || msunHospitals[0];
    localStorage.setItem('msun_selected_hospital', msunSelectedHospital.hospitalKey);
    const sel = document.getElementById('hospitalSelect');
    if (sel) sel.value = msunSelectedHospital.hospitalKey;
    const title = document.getElementById('pageTitle');
    if (title) {
        title.textContent = '众阳 · ' + msunSelectedHospital.hospitalName + ' — 接口联调';
    }
    const sub = document.getElementById('pageSub');
    if (sub) {
        sub.textContent = '厂家 ' + (msunSelectedHospital.vendorName || '众阳健康')
            + ' · 医院 ' + msunSelectedHospital.hospitalKey
            + ' · API ' + msunHospitalApi();
    }
    if (reloadEnv !== false) {
        zqLoadAllParams();
        zqLoadEnv();
    }
}

function msunOnHospitalChange() {
    const sel = document.getElementById('hospitalSelect');
    if (!sel) return;
    msunSelectHospital(sel.value, true);
}

document.addEventListener('DOMContentLoaded', async function () {
    if (!zqCheckLogin()) return;
    zqInitRespMeta();
    zqRenderAllForms();
    zqInitInlineRespSlots();
    zqInitYkDefaults();
    zqInitChecklist();
    zqRenderTestLog();
    const tester = document.getElementById('fb_tester');
    if (tester && !tester.value) tester.value = localStorage.getItem('username') || '';
    await msunLoadHospitals();
    zqLoadAllParams();
    zqLoadEnv();
    if (location.hash === '#bill-push') {
        var billTabBtn = document.querySelector('.tabs .tab-btn[data-tab="tab-bill-push"]');
        if (billTabBtn) zqSwitchTab('tab-bill-push', billTabBtn);
    }
});
