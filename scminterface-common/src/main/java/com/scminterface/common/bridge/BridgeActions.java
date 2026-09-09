package com.scminterface.common.bridge;

/**
 * SPD ↔ 前置机 ↔ 云端 稳态桥 action 常量（业务只增 action，不改前置机路径）
 */
public final class BridgeActions
{
    private BridgeActions()
    {
    }

    public static final String SUPPLIER_LIST_BY_HOSPITAL = "supplier.listByHospital";
    public static final String SUPPLIER_PROFILE = "supplier.profile";
    public static final String DELIVERY_QUERY = "delivery.query";
    public static final String DELIVERY_DOWNLOAD = "delivery.download";
    public static final String ORDER_PUBLISH_PAYLOAD = "order.publishPayload";
    public static final String MATERIAL_ARCHIVE_PUSH = "material.archive.push";
    public static final String MATERIAL_ARCHIVE_PULL = "material.archive.pull";
    public static final String MATERIAL_ARCHIVE_SUBMIT_APPLY = "material.archive.submitApply";
    public static final String MATERIAL_ARCHIVE_AUDIT_APPLY = "material.archive.auditApply";
    public static final String MATERIAL_ARCHIVE_LIST_APPLIES = "material.archive.listApplies";
}
