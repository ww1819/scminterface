package com.scminterface.customer.hengsuiThird.his.service;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.scminterface.customer.hengsuiThird.his.HisBillingTenantConstants;
import com.scminterface.framework.web.mapper.SpdSysConfigMapper;

/**
 * 镜像同步完成后调用 SPD 内部接口，执行与 SPD 抓取后相同的自动低值消耗/退费逻辑。
 */
@Service
public class SpdPatientChargeInternalClient
{
    private static final Logger log = LoggerFactory.getLogger(SpdPatientChargeInternalClient.class);

    private static final String SPD_HEADER_INTERNAL_KEY = "X-Spd-Internal-Key";

    private static final String DEFAULT_SPD_BASE_URL = "http://127.0.0.1:8080";

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private SpdSysConfigMapper spdSysConfigMapper;
    @Autowired
    private TenantBillingSettingService tenantBillingSettingService;

    public void processFetchBatchAfterSync(String tenantId, String fetchBatchId, String visitKind)
    {
        if (StringUtils.isBlank(fetchBatchId) || StringUtils.isBlank(visitKind))
        {
            return;
        }
        String tid = StringUtils.isNotBlank(tenantId) ? tenantId.trim() : HisBillingTenantConstants.TENANT_HENGSHUI_THIRD;
        if (!HisBillingTenantConstants.TENANT_HENGSHUI_THIRD.equals(tid))
        {
            return;
        }
        if (!tenantBillingSettingService.isAnyAutoProcessEnabled(tid))
        {
            log.debug("租户 {} 未开启计费自动处理开关，跳过 SPD 内部处理", tid);
            return;
        }
        String baseUrl = resolveSpdBaseUrl();
        String apiKey = resolveInternalApiKey();
        if (StringUtils.isBlank(apiKey))
        {
            log.warn("未配置 sys_config.{}，无法调用 SPD 计费自动处理", HisBillingTenantConstants.CONFIG_INTERNAL_API_KEY);
            return;
        }
        String url = baseUrl.replaceAll("/+$", "") + "/his/internal/patientCharge/processFetchBatch";
        Map<String, Object> body = new HashMap<>();
        body.put("tenantId", tid);
        body.put("fetchBatchId", fetchBatchId.trim());
        body.put("visitKind", visitKind.trim().toUpperCase());
        try
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(SPD_HEADER_INTERNAL_KEY, apiKey);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            log.info("调用 SPD 计费自动处理 tenantId={} batch={} visitKind={} url={}", tid, fetchBatchId, visitKind, url);
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            if (resp.getBody() != null)
            {
                Object code = resp.getBody().get("code");
                if (code != null && !Integer.valueOf(200).equals(code) && !"200".equals(String.valueOf(code)))
                {
                    log.warn("SPD 计费自动处理返回非成功: {}", resp.getBody());
                }
            }
        }
        catch (RestClientException e)
        {
            log.warn("调用 SPD 计费自动处理失败 batch={} err={}", fetchBatchId, e.toString());
        }
    }

    private String resolveSpdBaseUrl()
    {
        try
        {
            String v = spdSysConfigMapper.selectValueByKey(HisBillingTenantConstants.CONFIG_SPD_INTERNAL_BASE_URL);
            if (StringUtils.isNotBlank(v))
            {
                return v.trim();
            }
        }
        catch (Exception e)
        {
            log.debug("读取 sys_config.spd.internal.base_url 失败: {}", e.getMessage());
        }
        return DEFAULT_SPD_BASE_URL;
    }

    private String resolveInternalApiKey()
    {
        try
        {
            return StringUtils.trimToNull(spdSysConfigMapper.selectValueByKey(HisBillingTenantConstants.CONFIG_INTERNAL_API_KEY));
        }
        catch (Exception e)
        {
            log.debug("读取 sys_config.his.internal.api_key 失败: {}", e.getMessage());
            return null;
        }
    }
}
