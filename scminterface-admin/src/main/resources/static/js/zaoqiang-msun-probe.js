/**
 * 枣强中医院众阳接口统一联调页
 */
const ZQ_MSUN_API = '/api/customer/zaoqiang-tcm-001/msun';
const ZQ_LS_KEY = 'zq_msun_probe_params_v2';

var zqTestLog = [];
var zqLastResult = null;
var zqRunningAll = false;

function zqFieldId(apiKey, fieldKey) {
    return 'zq_' + apiKey + '_' + fieldKey;
}

function zqJsonId(apiKey) {
    return 'zq_json_' + apiKey;
}

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
    try { return JSON.stringify(obj, null, 2); } catch (e) { return String(obj); }
}

function zqSetMeta(text) {
    document.getElementById('metaInfo').textContent = text;
}

function zqEscapeHtml(s) {
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function zqHisSummary(result) {
    const hisBody = result && result.data && result.data.hisBody;
    if (hisBody && typeof hisBody === 'object') {
        let s = 'HIS success=' + hisBody.success + ', code=' + (hisBody.code || '') + ', message=' + (hisBody.message || '');
        if (Array.isArray(hisBody.data)) s += ', data.length=' + hisBody.data.length;
        return { text: s, ok: result.code === 200 && hisBody.success === true, hisCode: hisBody.code || '', message: hisBody.message || '' };
    }
    if (result && result.data && result.data.baseUrl) {
        return { text: '环境 activeEnv=' + (result.data.activeEnv || ''), ok: result.code === 200, hisCode: '', message: result.msg || '' };
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
        '2.5.62 供应商', '2.5.63 生产厂商', '2.5.43 药房批次库存', '2.5.102 一级库入退库'];
    const tbody = document.getElementById('checklistBody');
    if (!tbody) return;
    tbody.innerHTML = apis.map(function (api) {
        return '<tr data-api="' + api + '"><td>' + api + '</td><td><span class="chk-status pending">待测</span></td></tr>';
    }).join('');
}

function zqSwitchRightTab(tabId, btn) {
    document.querySelectorAll('.right-tab-panel').forEach(function (p) { p.classList.remove('active'); });
    document.querySelectorAll('.right-tabs .right-tab-btn').forEach(function (b) { b.classList.remove('active'); });
    document.getElementById(tabId).classList.add('active');
    if (btn) btn.classList.add('active');
}

function zqShowResult(title, requestLine, result, elapsedMs) {
    zqLastResult = result;
    zqRecordTest(title, result, elapsedMs);
    const box = document.getElementById('responseBox');
    const sum = zqHisSummary(result);
    box.innerHTML =
        '<div class="resp-head ' + (sum.ok ? 'ok' : 'err') + '">' +
        '<strong>' + zqEscapeHtml(title) + '</strong> · ' + elapsedMs + 'ms<br>' +
        '<code class="req-line">' + zqEscapeHtml(requestLine) + '</code><br>' +
        '<span class="summary">' + zqEscapeHtml(sum.text) + '</span>' +
        (result && result.activeEnv ? '<br><span class="env-tag">环境: ' + result.activeEnv + ' · ' + (result.msunBaseUrl || '') + '</span>' : '') +
        '</div><pre class="resp-json">' + zqEscapeHtml(zqFmtJson(result)) + '</pre>';
    zqSwitchRightTab('right-current', document.querySelector('.right-tabs .right-tab-btn'));
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
    document.getElementById('responseBox').innerHTML =
        '<div class="resp-head">' + zqEscapeHtml(item.api) + ' · ' + zqEscapeHtml(item.time) + '</div>' +
        '<pre class="resp-json">' + zqEscapeHtml(zqFmtJson(item.raw)) + '</pre>';
    zqSwitchRightTab('right-current', document.querySelector('.right-tabs .right-tab-btn'));
}

async function zqInvoke(title, method, path, queryParams, body) {
    if (!zqCheckLogin()) return null;
    const qs = queryParams ? zqBuildQuery(queryParams) : '';
    const url = ZQ_MSUN_API + path + qs;
    const requestLine = method + ' ' + url + (body ? '\nBody: ' + zqFmtJson(body) : '');
    zqSetMeta('请求中: ' + title + '…');
    const t0 = Date.now();
    let result;
    try {
        result = method === 'GET' ? await get(url) : await post(url, body || {});
    } catch (e) {
        result = { code: 500, msg: e.message };
    }
    const elapsed = Date.now() - t0;
    zqShowResult(title, requestLine, result, elapsed);
    zqSetMeta('完成: ' + title);
    return result;
}

function zqSwitchTab(tabId, btn) {
    document.querySelectorAll('.tab-panel').forEach(function (p) { p.classList.remove('active'); });
    document.querySelectorAll('.tabs .tab-btn').forEach(function (b) { b.classList.remove('active'); });
    document.getElementById(tabId).classList.add('active');
    if (btn) btn.classList.add('active');
}

function zqFormatDt(d) {
    const pad = function (n) { return n < 10 ? '0' + n : '' + n; };
    return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' +
        pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds());
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
    } else {
        input = '<input type="text" id="' + id + '" data-api="' + apiKey + '" data-key="' + field.key + '"' +
            (field.defaultValue !== undefined ? ' value="' + zqEscapeHtml(field.defaultValue) + '"' : '') + '>';
    }
    return '<div class="field"><label>' + field.label + req + '</label>' + input +
        '<span class="field-hint">' + zqEscapeHtml(field.hint || '') + '</span></div>';
}

function zqRenderApiForm(apiKey, schema) {
    const fieldsHtml = schema.fields.map(function (f) { return zqRenderField(apiKey, f); }).join('');
    let actions = '<button type="button" class="btn-call" onclick="zqCallApi(\'' + apiKey + '\')">调用</button>' +
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
        '<details class="json-editor-wrap"><summary>高级：JSON 入参编辑（覆盖表单，POST 接口可直接编辑）</summary>' +
        '<textarea id="' + zqJsonId(apiKey) + '" class="json-editor" rows="6" placeholder="{}"></textarea>' +
        '<button type="button" class="btn-call secondary" onclick="zqSyncFormFromJson(\'' + apiKey + '\')">JSON→表单</button></details>' +
        '<div class="tool-row">' + actions + '</div></div>';
}

function zqRenderAllForms() {
    document.getElementById('form-depts').innerHTML = zqRenderApiForm('depts', ZQ_PARAM_SCHEMA.depts);
    document.getElementById('form-identities').innerHTML = zqRenderApiForm('identities', ZQ_PARAM_SCHEMA.identities);
    ['drugDict', 'dictCategory', 'suppliers', 'producers', 'batchStocks', 'ykInstock'].forEach(function (key) {
        document.getElementById('form-' + key).innerHTML = zqRenderApiForm(key, ZQ_PARAM_SCHEMA[key]);
    });
}

function zqGetFieldValue(apiKey, field) {
    const el = document.getElementById(zqFieldId(apiKey, field.key));
    if (!el) return '';
    return el.value.trim();
}

function zqCollectFromForm(apiKey) {
    const schema = ZQ_PARAM_SCHEMA[apiKey];
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
    const schema = ZQ_PARAM_SCHEMA[apiKey];
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
        const schema = ZQ_PARAM_SCHEMA[apiKey];
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
    const schema = ZQ_PARAM_SCHEMA[apiKey];
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
    Object.keys(ZQ_PARAM_SCHEMA).forEach(function (apiKey) {
        data[apiKey] = { form: zqCollectFromForm(apiKey), json: (document.getElementById(zqJsonId(apiKey)) || {}).value || '' };
    });
    localStorage.setItem(ZQ_LS_KEY, JSON.stringify(data));
    zqSetMeta('参数已保存到浏览器本地');
}

function zqLoadAllParams() {
    const raw = localStorage.getItem(ZQ_LS_KEY);
    if (!raw) return;
    try {
        const data = JSON.parse(raw);
        Object.keys(data).forEach(function (apiKey) {
            if (!ZQ_PARAM_SCHEMA[apiKey]) return;
            const saved = data[apiKey];
            if (saved.form) {
                ZQ_PARAM_SCHEMA[apiKey].fields.forEach(function (f) {
                    const el = document.getElementById(zqFieldId(apiKey, f.key));
                    if (el && saved.form[f.key] !== undefined) el.value = saved.form[f.key];
                });
            }
            const jsonTa = document.getElementById(zqJsonId(apiKey));
            if (jsonTa && saved.json) jsonTa.value = saved.json;
        });
    } catch (e) { /* ignore */ }
}

function zqInitYkDefaults() {
    const now = new Date();
    const startEl = document.getElementById(zqFieldId('ykInstock', 'startTime'));
    const endEl = document.getElementById(zqFieldId('ykInstock', 'endTime'));
    if (startEl && !startEl.value) startEl.value = zqFormatDt(new Date(now.getTime() - 7 * 86400000));
    if (endEl && !endEl.value) endEl.value = zqFormatDt(now);
}

async function zqCallApi(apiKey) {
    const schema = ZQ_PARAM_SCHEMA[apiKey];
    let params = zqMergeJsonOverride(apiKey, zqCollectFromForm(apiKey));
    if (!params || !zqValidateRequired(apiKey, params)) return null;
    if (schema.bodyMode) {
        return zqInvoke(schema.logTitle, schema.method, schema.path, null, params);
    }
    return zqInvoke(schema.logTitle, schema.method, schema.path, params, null);
}

async function zqLoadEnv() {
    return zqInvoke('当前环境', 'GET', '/env', null, null);
}

function zqCallDepts() { return zqCallApi('depts'); }
function zqCallIdentities() { return zqCallApi('identities'); }
function zqCallDrugDict() { return zqCallApi('drugDict'); }
function zqCallDictCategory() { return zqCallApi('dictCategory'); }
function zqCallSuppliers() { return zqCallApi('suppliers'); }
function zqCallProducers() { return zqCallApi('producers'); }
function zqCallBatchStocks() { return zqCallApi('batchStocks'); }
function zqCallYkInstock() { return zqCallApi('ykInstock'); }

async function zqCallIdentitiesSample() {
    const roleType = zqGetFieldValue('identities', { key: 'roleType' }) || '0';
    return zqInvoke('2.1.12 人员身份', 'GET', '/identities/sample', { roleType: roleType }, null);
}

function zqSetField(apiKey, fieldKey, value) {
    const el = document.getElementById(zqFieldId(apiKey, fieldKey));
    if (el && value !== undefined && value !== null) el.value = String(value);
}

function zqFillFromLastDict() {
    if (!zqLastResult || !zqLastResult.data || !zqLastResult.data.hisBody) {
        alert('请先调用 2.5.44 药品材料字典'); return;
    }
    const data = zqLastResult.data.hisBody.data;
    if (!Array.isArray(data) || !data.length) { alert('字典 data 为空'); return; }
    const first = data[0];
    zqSetField('batchStocks', 'drugId', first.drugId);
    if (first.drugSpecPackingId) zqSetField('batchStocks', 'drugSpecPackingId', first.drugSpecPackingId);
    else if (first.specPackingList && first.specPackingList.length) {
        zqSetField('batchStocks', 'drugSpecPackingId', first.specPackingList[0].drugSpecPackingId);
    }
    zqSwitchTab('tab-spd', document.querySelectorAll('.tabs .tab-btn')[2]);
    alert('已填充 batchStocks 的 drugId / drugSpecPackingId');
}

function zqFillDeptFromLast() {
    for (let i = zqTestLog.length - 1; i >= 0; i--) {
        const raw = zqTestLog[i].raw;
        const list = raw && raw.data && raw.data.hisBody && raw.data.hisBody.data;
        if (!Array.isArray(list) || !list.length || !list[0].deptId) continue;
        const deptId = list[0].deptId;
        zqSetField('identities', 'deptId', deptId);
        zqSetField('batchStocks', 'deptId', deptId);
        zqSetField('ykInstock', 'deptId', deptId);
        alert('已填充 deptId=' + deptId);
        return;
    }
    alert('请先调用 2.1.9 科室接口');
}

function zqFillFromLastDictSilent(dictRes) {
    const data = dictRes.data && dictRes.data.hisBody && dictRes.data.hisBody.data;
    if (!Array.isArray(data) || !data.length) return;
    const first = data[0];
    zqSetField('batchStocks', 'drugId', first.drugId);
    if (first.drugSpecPackingId) zqSetField('batchStocks', 'drugSpecPackingId', first.drugSpecPackingId);
    else if (first.specPackingList && first.specPackingList.length) {
        zqSetField('batchStocks', 'drugSpecPackingId', first.specPackingList[0].drugSpecPackingId);
    }
}

function zqRecordSkipped(api, reason) {
    zqTestLog.push({ time: zqFormatDt(new Date()), api: api, ok: false, hisCode: 'SKIP', message: reason, elapsedMs: 0, raw: { skipped: true } });
    zqRefreshChecklist(api, false);
    zqRenderTestLog();
}

async function zqRunAllTests() {
    if (zqRunningAll || !zqCheckLogin()) return;
    if (!confirm('按推荐顺序测试全部查询接口？')) return;
    zqRunningAll = true;
    zqTestLog = [];
    zqInitChecklist();
    zqRenderTestLog();
    const btn = document.getElementById('btnRunAll');
    if (btn) btn.disabled = true;
    try {
        await zqLoadEnv(); await zqSleep(400);
        const deptRes = await zqCallDepts(); await zqSleep(400);
        if (deptRes && deptRes.data && deptRes.data.hisBody && deptRes.data.hisBody.data && deptRes.data.hisBody.data[0]) {
            zqSetField('identities', 'deptId', deptRes.data.hisBody.data[0].deptId);
            zqSetField('batchStocks', 'deptId', deptRes.data.hisBody.data[0].deptId);
        }
        await zqCallIdentitiesSample(); await zqSleep(400);
        const dictRes = await zqCallDrugDict(); await zqSleep(400);
        if (dictRes) zqFillFromLastDictSilent(dictRes);
        await zqCallDictCategory(); await zqSleep(400);
        await zqCallSuppliers(); await zqSleep(400);
        await zqCallProducers(); await zqSleep(400);
        const bs = zqCollectFromForm('batchStocks');
        if (bs.deptId && bs.drugId && bs.drugSpecPackingId) { await zqCallBatchStocks(); await zqSleep(400); }
        else zqRecordSkipped('2.5.43 药房批次库存', '缺少三要素，已跳过');
        await zqCallYkInstock();
        zqSwitchRightTab('right-log', document.querySelectorAll('.right-tabs .right-tab-btn')[1]);
    } finally {
        zqRunningAll = false;
        if (btn) btn.disabled = false;
    }
}

function zqExportFeedback() {
    const tester = (document.getElementById('fb_tester') || {}).value || '（未填写）';
    const note = (document.getElementById('fb_note') || {}).value || '';
    let env = (zqLastResult && zqLastResult.activeEnv) || '未知';
    const lines = ['【枣强中医院众阳接口测试反馈】', '测试人：' + tester, '测试时间：' + zqFormatDt(new Date()),
        '环境：' + env, '前置机：' + window.location.host, '', '| 序号 | 接口 | 结果 | HIS code | message |',
        '|------|------|------|----------|---------|'];
    zqTestLog.forEach(function (item, idx) {
        lines.push('| ' + (idx + 1) + ' | ' + item.api + ' | ' + (item.ok ? '成功' : '失败') +
            ' | ' + (item.hisCode || '-') + ' | ' + (item.message || '').replace(/\|/g, '/') + ' |');
    });
    lines.push('', '补充说明：' + (note || '无'), '', '--- 最近回参 ---', zqFmtJson(zqLastResult));
    const ta = document.getElementById('feedbackExport');
    if (ta) { ta.value = lines.join('\n'); ta.style.display = 'block'; }
    zqSwitchRightTab('right-feedback', document.querySelectorAll('.right-tabs .right-tab-btn')[2]);
}

function zqCopyFeedback() {
    if (!(document.getElementById('feedbackExport') || {}).value) zqExportFeedback();
    const ta = document.getElementById('feedbackExport');
    ta.select();
    document.execCommand('copy');
    alert('已复制到剪贴板');
}

document.addEventListener('DOMContentLoaded', function () {
    if (!zqCheckLogin()) return;
    zqRenderAllForms();
    zqInitYkDefaults();
    zqLoadAllParams();
    zqInitChecklist();
    zqRenderTestLog();
    const tester = document.getElementById('fb_tester');
    if (tester && !tester.value) tester.value = localStorage.getItem('username') || '';
    zqLoadEnv();
});
