package com.scminterface.common.bridge;

/**
 * 桥接协议常量
 */
public final class BridgeConstants
{
    private BridgeConstants()
    {
    }

    /** 院内 SPD / 前置机 → 云端可选共享密钥头 */
    public static final String HEADER_BRIDGE_TOKEN = "X-Scm-Bridge-Token";

    /** 院内 SPD → 前置机内部密钥（与现有 SPD 内部 Key 对齐，可选） */
    public static final String HEADER_SPD_INTERNAL_KEY = "X-Spd-Internal-Key";

    public static final String HOSPITAL_PATH_PREFIX = "/api/bridge/v1";

    public static final String CLOUD_PATH_PREFIX = "/api/cloud/spd/bridge/v1";

    /** 收件箱未消费 */
    public static final String INBOX_STATUS_PENDING = "0";

    /** 收件箱已确认 */
    public static final String INBOX_STATUS_ACKED = "1";
}
