package com.scminterface.framework.domain.zs;

/**
 * 第三方订单主表（XML 导出用）
 */
public class ZsTpOrderXmlRow
{
    private String id;
    private String dh;
    private String supno;
    private String ck;
    private String ckno;
    private String ksbh;
    /** 供应商名称（第一方订单导出时来自 scm_order / scm_supplier） */
    private String supName;
    /** 科室名称（第一方订单） */
    private String ksmc;
    private String jsfs;
    private String zjly;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getDh()
    {
        return dh;
    }

    public void setDh(String dh)
    {
        this.dh = dh;
    }

    public String getSupno()
    {
        return supno;
    }

    public void setSupno(String supno)
    {
        this.supno = supno;
    }

    public String getCk()
    {
        return ck;
    }

    public void setCk(String ck)
    {
        this.ck = ck;
    }

    public String getCkno()
    {
        return ckno;
    }

    public void setCkno(String ckno)
    {
        this.ckno = ckno;
    }

    public String getKsbh()
    {
        return ksbh;
    }

    public void setKsbh(String ksbh)
    {
        this.ksbh = ksbh;
    }

    public String getSupName()
    {
        return supName;
    }

    public void setSupName(String supName)
    {
        this.supName = supName;
    }

    public String getKsmc()
    {
        return ksmc;
    }

    public void setKsmc(String ksmc)
    {
        this.ksmc = ksmc;
    }

    public String getJsfs()
    {
        return jsfs;
    }

    public void setJsfs(String jsfs)
    {
        this.jsfs = jsfs;
    }

    public String getZjly()
    {
        return zjly;
    }

    public void setZjly(String zjly)
    {
        this.zjly = zjly;
    }
}
