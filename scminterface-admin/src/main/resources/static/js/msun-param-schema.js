/**
 * 众阳 HIS 联调页 — 各接口入参定义（与接口文档1/2 对齐，各医院客户共用）
 */
var MSUN_PARAM_SCHEMA = {
    depts: {
        title: '2.1.9 科室基本信息',
        method: 'GET',
        path: '/depts',
        logTitle: '2.1.9 科室',
        fields: [
            { key: 'invalidFlag', label: 'invalidFlag', hint: '0启用 1作废 -1全量', defaultValue: '-1', options: ['-1', '0', '1'] },
            { key: 'hospitalAreaId', label: 'hospitalAreaId', hint: '院区ID，取自2.1.18' },
            { key: 'deptId', label: 'deptId', hint: '科室ID' },
            { key: 'deptName', label: 'deptName', hint: '科室名称模糊' }
        ],
        actions: ['zqFillDeptFromLast']
    },
    identities: {
        title: '2.1.12 用户身份信息',
        method: 'GET',
        path: '/identities',
        logTitle: '2.1.12 人员身份',
        hint: 'roleType / deptId / identityId / userId 至少填一项',
        fields: [
            { key: 'roleType', label: 'roleType', hint: '0管理员 1门诊收款…8医技', defaultValue: '0',
              options: ['0', '1', '2', '3', '4', '5', '6', '7', '8'] },
            { key: 'deptId', label: 'deptId', hint: '科室ID，可来自2.1.9' },
            { key: 'identityId', label: 'identityId', hint: '用户身份ID' },
            { key: 'userId', label: 'userId', hint: '用户ID' }
        ],
        actions: ['zqCallIdentitiesSample', 'zqFillDeptFromLast']
    },
    drugDict: {
        title: '2.5.44 药品、材料字典',
        method: 'GET',
        path: '/spd/query/drug-dict-infos',
        logTitle: '2.5.44 药品材料字典',
        fields: [
            { key: 'drugCode', label: 'drugCode', hint: '药品编码' },
            { key: 'drugId', label: 'drugId', hint: '药品/材料字典ID' },
            { key: 'drugName', label: 'drugName', hint: '名称或拼音模糊' },
            { key: 'materialOrDrug', label: 'materialOrDrug', hint: '0药品 1材料', defaultValue: '0', options: ['0', '1'] },
            { key: 'limitCount', label: 'limitCount', hint: '查询条数', defaultValue: '10' },
            { key: 'invalidFlag', label: 'invalidFlag', hint: '0否 1是', defaultValue: '0', options: ['0', '1'] },
            { key: 'specialFlag', label: 'specialFlag', hint: '是否特殊药品 0否 1是', options: ['', '0', '1'] },
            { key: 'startTime', label: 'startTime', hint: 'yyyy-MM-dd HH:mm:ss' },
            { key: 'endTime', label: 'endTime', hint: 'yyyy-MM-dd HH:mm:ss' },
            { key: 'hospitalId', label: 'hospitalId', hint: '留空用服务端配置' },
            { key: 'orgId', label: 'orgId', hint: '留空用服务端配置' }
        ],
        actions: ['zqFillFromLastDict'],
        pagination: {
            pageSizeKey: 'limitCount',
            defaultPageSize: 100,
            maxPages: 500,
            cursorParam: 'drugId',
            cursorField: 'drugId',
            delayMs: 300,
            hint: 'limitCount 为每页条数；翻页时传入本页最大 drugId 作为游标'
        }
    },
    dictCategory: {
        title: '2.5.58 SPD 药品材料分类',
        method: 'GET',
        path: '/spd/query/dict-category',
        logTitle: '2.5.58 分类字典',
        fields: [
            { key: 'keyWord', label: 'keyWord', hint: '分类名称模糊', defaultValue: '西药' },
            { key: 'limitCount', label: 'limitCount', hint: '查询条数', defaultValue: '20' }
        ],
        pagination: {
            pageSizeKey: 'limitCount',
            defaultPageSize: 100,
            maxPages: 200,
            cursorParam: 'hisDictId',
            cursorField: 'hisDictId',
            delayMs: 300,
            hint: 'limitCount 为每页条数；翻页时传入本页最大 hisDictId 作为游标'
        }
    },
    suppliers: {
        title: '2.5.62 SPD 供应商',
        method: 'GET',
        path: '/spd/query/drug-suppliers',
        logTitle: '2.5.62 供应商',
        fields: [
            { key: 'keyWord', label: 'keyWord', hint: '名称/简拼模糊' },
            { key: 'limitCount', label: 'limitCount', hint: '≥1', defaultValue: '20' },
            { key: 'materialOrDrug', label: 'materialOrDrug', hint: '0药品 1或2材料', defaultValue: '0', options: ['0', '1', '2'] },
            { key: 'hospitalId', label: 'hospitalId', hint: '留空用服务端' },
            { key: 'orgId', label: 'orgId', hint: '留空用服务端' }
        ],
        pagination: {
            pageSizeKey: 'limitCount',
            defaultPageSize: 100,
            maxPages: 500,
            cursorParam: 'supplierId',
            cursorField: 'supplierId',
            delayMs: 300,
            hint: 'limitCount 为每页条数；翻页时传入本页最大 supplierId 作为游标'
        }
    },
    producers: {
        title: '2.5.63 SPD 生产厂商',
        method: 'GET',
        path: '/spd/query/drug-producers',
        logTitle: '2.5.63 生产厂商',
        fields: [
            { key: 'keyWord', label: 'keyWord', hint: '名称/简拼模糊' },
            { key: 'limitCount', label: 'limitCount', hint: '≥1', defaultValue: '20' },
            { key: 'materialOrDrug', label: 'materialOrDrug', hint: '0药品 1或2材料', defaultValue: '0', options: ['0', '1', '2'] },
            { key: 'hospitalId', label: 'hospitalId', hint: '留空用服务端' },
            { key: 'orgId', label: 'orgId', hint: '留空用服务端' }
        ],
        pagination: {
            pageSizeKey: 'limitCount',
            defaultPageSize: 100,
            maxPages: 500,
            cursorParam: 'producerId',
            cursorField: 'producerId',
            delayMs: 300,
            hint: 'limitCount 为每页条数；翻页时传入本页最大 producerId 作为游标'
        }
    },
    batchStocks: {
        title: '2.5.43 药房批次库存',
        method: 'GET',
        path: '/spd/query/drug-batch-stocks',
        logTitle: '2.5.43 药房批次库存',
        hint: 'deptId←2.1.9；drugId/drugSpecPackingId←2.5.44',
        fields: [
            { key: 'deptId', label: 'deptId', required: true, hint: '药房科室ID' },
            { key: 'drugId', label: 'drugId', required: true, hint: '药品材料ID' },
            { key: 'drugSpecPackingId', label: 'drugSpecPackingId', required: true, hint: '规格包装ID' }
        ],
        actions: ['zqFillFromLastDict', 'zqFillDeptFromLast']
    },
    ykInstock: {
        title: '2.5.102 一级库入退库记录',
        method: 'POST',
        path: '/spd/query/yk-instock',
        logTitle: '2.5.102 一级库入退库',
        bodyMode: true,
        fields: [
            { key: 'startTime', label: 'startTime', required: true, hint: 'yyyy-MM-dd HH:mm:ss' },
            { key: 'endTime', label: 'endTime', required: true, hint: 'yyyy-MM-dd HH:mm:ss' },
            { key: 'deptId', label: 'deptId', hint: '入退库科室' },
            { key: 'type', label: 'type', hint: '0入库 1退库', options: ['', '0', '1'] },
            { key: 'instockCode', label: 'instockCode', hint: '入退库单号' }
        ]
    }
};
/** @deprecated 兼容旧脚本别名 */
var ZQ_PARAM_SCHEMA = MSUN_PARAM_SCHEMA;
