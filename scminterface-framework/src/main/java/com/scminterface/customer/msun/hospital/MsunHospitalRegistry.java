package com.scminterface.customer.msun.hospital;

import com.scminterface.customer.msun.MsunVendorConstants;

/**
 * 已接入的众阳 HIS 医院客户登记册。新增客户时在此追加一项，并在 {@code hospital} 下建独立子包。
 */
public enum MsunHospitalRegistry
{
    ZAOQIANG_TCM("zaoqiang-tcm-001", "枣强县中医院");

    private final String hospitalKey;

    private final String hospitalName;

    MsunHospitalRegistry(String hospitalKey, String hospitalName)
    {
        this.hospitalKey = hospitalKey;
        this.hospitalName = hospitalName;
    }

    public String getHospitalKey()
    {
        return hospitalKey;
    }

    public String getHospitalName()
    {
        return hospitalName;
    }

    public String getVendorCode()
    {
        return MsunVendorConstants.VENDOR_CODE;
    }

    public String getVendorName()
    {
        return MsunVendorConstants.VENDOR_NAME;
    }

    public String configPrefix()
    {
        return "scminterface.vendor.msun.hospitals." + hospitalKey;
    }

    public String apiPrefix()
    {
        return MsunVendorConstants.hospitalApiPrefix(hospitalKey);
    }

    public static MsunHospitalRegistry resolve(String hospitalKey)
    {
        if (hospitalKey == null)
        {
            return null;
        }
        for (MsunHospitalRegistry item : values())
        {
            if (item.hospitalKey.equalsIgnoreCase(hospitalKey.trim()))
            {
                return item;
            }
        }
        return null;
    }
}
