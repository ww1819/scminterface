package com.scminterface.common.core.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 档案推送数据传输对象
 * 
 * @author scminterface
 */
public class MaterialArchiveDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 供应商ID */
    private Long supplierId;

    /** 供应商名称 */
    private String supplierName;

    /** 档案列表 */
    private List<MaterialArchiveItem> archiveList;

    public Long getSupplierId()
    {
        return supplierId;
    }

    public void setSupplierId(Long supplierId)
    {
        this.supplierId = supplierId;
    }

    public String getSupplierName()
    {
        return supplierName;
    }

    public void setSupplierName(String supplierName)
    {
        this.supplierName = supplierName;
    }

    public List<MaterialArchiveItem> getArchiveList()
    {
        return archiveList;
    }

    public void setArchiveList(List<MaterialArchiveItem> archiveList)
    {
        this.archiveList = archiveList;
    }

    /**
     * 档案项
     */
    public static class MaterialArchiveItem implements Serializable
    {
        private static final long serialVersionUID = 1L;

        /** 产品名称 */
        private String materialName;

        /** 规格 */
        private String specification;

        /** 型号 */
        private String model;

        /** 单价 */
        private BigDecimal price;

        /** 注册证号 */
        private String registerNo;

        /** 注册证名称 */
        private String registerName;

        /** 生产厂家 */
        private String manufacturerName;

        /** UDI码 */
        private String udiCode;

        /** 有效期 */
        private Date expireDate;

        /** 单位 */
        private String unit;

        /** 销售价 */
        private BigDecimal salePrice;

        /** 中标价格 */
        private BigDecimal bidPrice;

        /** 产品类别 */
        private String productCategory;

        /** 医保名称 */
        private String medicalName;

        /** 医保编码 */
        private String medicalNo;

        /** 品牌 */
        private String brand;

        /** 用途 */
        private String useto;

        /** 材质 */
        private String quality;

        /** 功能 */
        private String function;

        /** 储存方式 */
        private String isWay;

        /** 国家编码 */
        private String countryNo;

        /** 国家医保名称 */
        private String countryName;

        /** 商品说明 */
        private String description;

        /** 拼音简码 */
        private String pinyinCode;

        public String getMaterialName()
        {
            return materialName;
        }

        public void setMaterialName(String materialName)
        {
            this.materialName = materialName;
        }

        public String getSpecification()
        {
            return specification;
        }

        public void setSpecification(String specification)
        {
            this.specification = specification;
        }

        public String getModel()
        {
            return model;
        }

        public void setModel(String model)
        {
            this.model = model;
        }

        public BigDecimal getPrice()
        {
            return price;
        }

        public void setPrice(BigDecimal price)
        {
            this.price = price;
        }

        public String getRegisterNo()
        {
            return registerNo;
        }

        public void setRegisterNo(String registerNo)
        {
            this.registerNo = registerNo;
        }

        public String getRegisterName()
        {
            return registerName;
        }

        public void setRegisterName(String registerName)
        {
            this.registerName = registerName;
        }

        public String getManufacturerName()
        {
            return manufacturerName;
        }

        public void setManufacturerName(String manufacturerName)
        {
            this.manufacturerName = manufacturerName;
        }

        public String getUdiCode()
        {
            return udiCode;
        }

        public void setUdiCode(String udiCode)
        {
            this.udiCode = udiCode;
        }

        public Date getExpireDate()
        {
            return expireDate;
        }

        public void setExpireDate(Date expireDate)
        {
            this.expireDate = expireDate;
        }

        public String getUnit()
        {
            return unit;
        }

        public void setUnit(String unit)
        {
            this.unit = unit;
        }

        public BigDecimal getSalePrice()
        {
            return salePrice;
        }

        public void setSalePrice(BigDecimal salePrice)
        {
            this.salePrice = salePrice;
        }

        public BigDecimal getBidPrice()
        {
            return bidPrice;
        }

        public void setBidPrice(BigDecimal bidPrice)
        {
            this.bidPrice = bidPrice;
        }

        public String getProductCategory()
        {
            return productCategory;
        }

        public void setProductCategory(String productCategory)
        {
            this.productCategory = productCategory;
        }

        public String getMedicalName()
        {
            return medicalName;
        }

        public void setMedicalName(String medicalName)
        {
            this.medicalName = medicalName;
        }

        public String getMedicalNo()
        {
            return medicalNo;
        }

        public void setMedicalNo(String medicalNo)
        {
            this.medicalNo = medicalNo;
        }

        public String getBrand()
        {
            return brand;
        }

        public void setBrand(String brand)
        {
            this.brand = brand;
        }

        public String getUseto()
        {
            return useto;
        }

        public void setUseto(String useto)
        {
            this.useto = useto;
        }

        public String getQuality()
        {
            return quality;
        }

        public void setQuality(String quality)
        {
            this.quality = quality;
        }

        public String getFunction()
        {
            return function;
        }

        public void setFunction(String function)
        {
            this.function = function;
        }

        public String getIsWay()
        {
            return isWay;
        }

        public void setIsWay(String isWay)
        {
            this.isWay = isWay;
        }

        public String getCountryNo()
        {
            return countryNo;
        }

        public void setCountryNo(String countryNo)
        {
            this.countryNo = countryNo;
        }

        public String getCountryName()
        {
            return countryName;
        }

        public void setCountryName(String countryName)
        {
            this.countryName = countryName;
        }

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }

        public String getPinyinCode()
        {
            return pinyinCode;
        }

        public void setPinyinCode(String pinyinCode)
        {
            this.pinyinCode = pinyinCode;
        }
    }
}

