# -*- coding: utf-8 -*-
"""一次性脚本：从探针回参 txt 生成 03_seed_probe_sample.sql（生成后可删除本文件）"""
import json
import os
import re

def camel_to_snake(name):
    s1 = re.sub(r'(.)([A-Z][a-z]+)', r'\1_\2', name)
    return re.sub(r'([a-z0-9])([A-Z])', r'\1_\2', s1).lower()

def sql_val(v):
    if v is None:
        return 'NULL'
    if isinstance(v, bool):
        return '1' if v else '0'
    if isinstance(v, (int, float)):
        return str(v)
    if isinstance(v, (list, dict)):
        return "'" + json.dumps(v, ensure_ascii=False).replace("'", "''") + "'"
    return "'" + str(v).replace("'", "''").replace('\\', '\\\\') + "'"

def meta_cols(api_code, root, batch='PROBE-20260605-001'):
    trace = root.get('data', {}).get('hisBody', {}).get('traceId')
    req = root.get('data', {}).get('requestParams')
    return {
        'hospital_key': root.get('hospitalKey', 'zaoqiang-tcm-001'),
        'active_env': root.get('activeEnv', 'prod'),
        'api_code': api_code,
        'sync_batch_no': batch,
        'his_trace_id': trace,
        'request_params_json': json.dumps(req, ensure_ascii=False) if req else None,
        'mirror_source': 'manual_probe',
    }

def meta_prefix(meta):
    cols = ['hospital_key', 'active_env', 'api_code', 'sync_batch_no', 'his_trace_id',
            'request_params_json', 'raw_item_json', 'mirror_source', 'mirror_time']
    vals = [sql_val(meta[k]) for k in ['hospital_key', 'active_env', 'api_code', 'sync_batch_no', 'his_trace_id', 'request_params_json']]
    return cols, vals

def main():
    base = r'd:\abs\爱斯普特\枣强中医院\接口'
    out_lines = [
        '-- 众阳HIS接口镜像库：探针样本数据手工落库（实施人员核对后执行）',
        '-- 库名：msun_his_mirror',
        '-- 注意：含唯一键，重复执行前请先清理本批次或改 @sync_batch_no',
        'USE `msun_his_mirror`;',
        '',
        "SET @sync_batch_no = 'PROBE-20260605-001';",
        'SET @mirror_time = NOW();',
        '',
        "INSERT INTO m_sync_batch (sync_batch_no,hospital_key,active_env,api_code,mirror_source,record_count,remark,mirror_time)",
        "VALUES (@sync_batch_no,'zaoqiang-tcm-001','prod',NULL,'manual_probe',0,'枣强探针样本落库',@mirror_time)",
        'ON DUPLICATE KEY UPDATE remark=VALUES(remark), mirror_time=VALUES(mirror_time);',
        '',
    ]
    batch = 'PROBE-20260605-001'

    with open(os.path.join(base, '2.1.9 科室基本信息回参.txt'), encoding='utf-8') as f:
        root = json.load(f)
    meta = meta_cols('2.1.9', root, batch)
    for row in root['data']['hisBody']['data']:
        row = dict(row)
        cats = row.pop('categoryIdList', None)
        cols, vals = meta_prefix(meta)
        vals.append(sql_val(json.dumps(row, ensure_ascii=False)))
        vals.append(sql_val(meta['mirror_source']))
        vals.append('@mirror_time')
        field_map = {
            'deptId': 'dept_id', 'deptCode': 'dept_code', 'deptName': 'dept_name', 'categoryId': 'category_id',
            'invalidFlag': 'invalid_flag', 'hospitalId': 'hospital_id', 'aliasName': 'alias_name', 'inputCode': 'input_code',
            'inputAliasCode': 'input_alias_code', 'fullCode': 'full_code', 'fullAliasCode': 'full_alias_code', 'wbCode': 'wb_code',
            'registerFlag': 'register_flag', 'level': 'level', 'sortOrder': 'sort_order', 'startTime': 'start_time', 'endTime': 'end_time',
            'parentId': 'parent_id', 'roomAddress': 'room_address', 'msunOrgId': 'msun_org_id', 'accountDeptId': 'account_dept_id',
            'nationalDeptInsuranceCode': 'national_dept_insurance_code', 'introduction': 'introduction', 'deptClassifyId': 'dept_classify_id',
            'hospitalAreaId': 'hospital_area_id', 'phone': 'phone', 'emergPhone': 'emerg_phone', 'hisOrgId': 'his_org_id',
            'hisCreaterId': 'his_creater_id', 'hisCreaterName': 'his_creater_name', 'hisCreateTime': 'his_create_time',
            'hisUpdaterId': 'his_updater_id', 'hisUpdateTime': 'his_update_time', 'orgId': 'org_id',
        }
        for k, col in field_map.items():
            cols.append(col)
            vals.append(sql_val(row.get(k)))
        out_lines.append('INSERT INTO m_dept (' + ','.join(cols) + ') VALUES (' + ','.join(vals) + ');')
        if cats:
            for cid in cats:
                out_lines.append(
                    'INSERT INTO m_dept_category_rel (hospital_key,active_env,sync_batch_no,dept_id,category_id,mirror_time) VALUES ('
                    + ','.join([sql_val(meta['hospital_key']), sql_val(meta['active_env']), '@sync_batch_no',
                                sql_val(row.get('deptId')), sql_val(cid), '@mirror_time']) + ');'
                )

    with open(os.path.join(base, '2.1.12 用户身份信息回参.txt'), encoding='utf-8') as f:
        root = json.load(f)
    meta = meta_cols('2.1.12', root, batch)
    for row in root['data']['hisBody']['data']:
        row = dict(row)
        accounts = row.pop('accountList', None)
        raw = dict(row)
        if accounts:
            raw['accountList'] = accounts
        cols, vals = meta_prefix(meta)
        vals.append(sql_val(json.dumps(raw, ensure_ascii=False)))
        vals.append(sql_val(meta['mirror_source']))
        vals.append('@mirror_time')
        for k, col in {
            'identityId': 'identity_id', 'deptId': 'dept_id', 'deptCode': 'dept_code', 'deptName': 'dept_name',
            'userId': 'user_id', 'userName': 'user_name', 'roleId': 'role_id', 'roleName': 'role_name',
            'userCode': 'user_code', 'staffCode': 'staff_code', 'roleType': 'role_type',
            'hospitalId': 'hospital_id', 'orgId': 'org_id',
        }.items():
            cols.append(col)
            vals.append(sql_val(row.get(k)))
        out_lines.append('INSERT INTO m_user_identity (' + ','.join(cols) + ') VALUES (' + ','.join(vals) + ');')
        if accounts:
            for acc in accounts:
                out_lines.append(
                    'INSERT INTO m_user_identity_account (hospital_key,active_env,sync_batch_no,identity_id,account_no,mirror_time) VALUES ('
                    + ','.join([sql_val(meta['hospital_key']), sql_val(meta['active_env']), '@sync_batch_no',
                                sql_val(row.get('identityId')), sql_val(acc), '@mirror_time']) + ');'
                )

    with open(os.path.join(base, '2.5.44 药品、材料字典回参.txt'), encoding='utf-8') as f:
        root = json.load(f)
    meta = meta_cols('2.5.44', root, batch)
    json_list_cols = {
        'dictDrugPharmacologyCategoryCodeList': 'dict_drug_pharmacology_category_code_list',
        'dictDrugPharmacologyCategoryIdList': 'dict_drug_pharmacology_category_id_list',
        'dictDrugPharmacologyCategoryNameList': 'dict_drug_pharmacology_category_name_list',
    }
    for row in root['data']['hisBody']['data']:
        cols, vals = meta_prefix(meta)
        vals.append(sql_val(json.dumps(row, ensure_ascii=False)))
        vals.append(sql_val(meta['mirror_source']))
        vals.append('@mirror_time')
        for k, v in row.items():
            col = json_list_cols.get(k, camel_to_snake(k))
            if k in json_list_cols:
                vals.append(sql_val(json.dumps(v, ensure_ascii=False) if v is not None else None))
            else:
                vals.append(sql_val(v))
            cols.append(col)
        out_lines.append('INSERT INTO m_drug_dict (' + ','.join(cols) + ') VALUES (' + ','.join(vals) + ');')

    with open(os.path.join(base, '2.5.58 SPD 药品材料分类回参.txt'), encoding='utf-8') as f:
        root = json.load(f)
    meta = meta_cols('2.5.58', root, batch)
    for row in root['data']['hisBody']['data']:
        cols, vals = meta_prefix(meta)
        vals.append(sql_val(json.dumps(row, ensure_ascii=False)))
        vals.append(sql_val(meta['mirror_source']))
        vals.append('@mirror_time')
        cols += ['his_dict_id', 'his_dict_name', 'org_id', 'hospital_id']
        vals += [sql_val(row.get('hisDictId')), sql_val(row.get('hisDictName')), sql_val(row.get('orgId')), sql_val(row.get('hospitalId'))]
        out_lines.append('INSERT INTO m_dict_category (' + ','.join(cols) + ') VALUES (' + ','.join(vals) + ');')

    with open(os.path.join(base, '2.5.62 SPD 供应商回参.txt'), encoding='utf-8') as f:
        root = json.load(f)
    meta = meta_cols('2.5.62', root, batch)
    field_map = {k: camel_to_snake(k) for k in root['data']['hisBody']['data'][0].keys()}
    for row in root['data']['hisBody']['data']:
        cols, vals = meta_prefix(meta)
        vals.append(sql_val(json.dumps(row, ensure_ascii=False)))
        vals.append(sql_val(meta['mirror_source']))
        vals.append('@mirror_time')
        for k, col in field_map.items():
            cols.append(col)
            vals.append(sql_val(row.get(k)))
        out_lines.append('INSERT INTO m_supplier (' + ','.join(cols) + ') VALUES (' + ','.join(vals) + ');')

    with open(os.path.join(base, '2.5.63 SPD 生产厂商回参.txt'), encoding='utf-8') as f:
        root = json.load(f)
    meta = meta_cols('2.5.63', root, batch)
    field_map = {k: camel_to_snake(k) for k in root['data']['hisBody']['data'][0].keys()}
    for row in root['data']['hisBody']['data']:
        cols, vals = meta_prefix(meta)
        vals.append(sql_val(json.dumps(row, ensure_ascii=False)))
        vals.append(sql_val(meta['mirror_source']))
        vals.append('@mirror_time')
        for k, col in field_map.items():
            cols.append(col)
            vals.append(sql_val(row.get(k)))
        out_lines.append('INSERT INTO m_producer (' + ','.join(cols) + ') VALUES (' + ','.join(vals) + ');')

    with open(os.path.join(base, '2.5.102 一级库入退库记录回参.txt'), encoding='utf-8') as f:
        root = json.load(f)
    meta = meta_cols('2.5.102', root, batch)
    header_map = {
        'instockCode': 'instock_code', 'storageInstockId': 'storage_instock_id', 'type': 'type',
        'supplierName': 'supplier_name', 'supplierId': 'supplier_id', 'instockTime': 'instock_time',
        'recordUserSysName': 'record_user_sys_name', 'hisCreaterName': 'his_creater_name',
        'deptId': 'dept_id', 'deptName': 'dept_name',
    }
    for row in root['data']['hisBody']['data']:
        row = dict(row)
        details = row.pop('stockDetailList', None)
        raw = dict(row)
        if details:
            raw['stockDetailList'] = details
        cols, vals = meta_prefix(meta)
        vals.append(sql_val(json.dumps(raw, ensure_ascii=False)))
        vals.append(sql_val(meta['mirror_source']))
        vals.append('@mirror_time')
        for k, col in header_map.items():
            cols.append(col)
            vals.append(sql_val(row.get(k)))
        out_lines.append('INSERT INTO m_yk_instock (' + ','.join(cols) + ') VALUES (' + ','.join(vals) + ');')
        if details:
            for d in details:
                dcols = ['hospital_key', 'active_env', 'sync_batch_no', 'storage_instock_id', 'instock_code', 'mirror_time']
                dvals = [sql_val(meta['hospital_key']), sql_val(meta['active_env']), '@sync_batch_no',
                         sql_val(row.get('storageInstockId')), sql_val(row.get('instockCode')), '@mirror_time']
                for k, col in {k: camel_to_snake(k) for k in d.keys()}.items():
                    dcols.append(col)
                    dvals.append(sql_val(d.get(k)))
                out_lines.append('INSERT INTO m_yk_instock_detail (' + ','.join(dcols) + ') VALUES (' + ','.join(dvals) + ');')

    out_path = os.path.join(os.path.dirname(__file__), '03_seed_probe_sample.sql')
    with open(out_path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(out_lines) + '\n')
    print('written', out_path, 'lines', len(out_lines))

if __name__ == '__main__':
    main()
