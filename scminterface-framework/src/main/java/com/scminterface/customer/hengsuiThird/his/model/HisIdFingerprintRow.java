package com.scminterface.customer.hengsuiThird.his.model;

/**
 * HIS 计费主键与行指纹（与 SPD his_*_mirror 去重查询一致）。衡水三院计费镜像同步使用。
 */
public class HisIdFingerprintRow
{
    private String hisChargeId;
    private String rowFingerprint;

    public String getHisChargeId()
    {
        return hisChargeId;
    }

    public void setHisChargeId(String hisChargeId)
    {
        this.hisChargeId = hisChargeId;
    }

    public String getRowFingerprint()
    {
        return rowFingerprint;
    }

    public void setRowFingerprint(String rowFingerprint)
    {
        this.rowFingerprint = rowFingerprint;
    }
}
