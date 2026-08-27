package com.scminterface.framework.web.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;
import com.scminterface.framework.config.properties.ScmAppNotifyProperties;

/**
 * 首次写入 scm_order 后异步通知 SCM 发微信模板。失败不影响推单。
 */
@Component
public class ScmOrderWxNotifyClient
{
    private static final Logger log = LoggerFactory.getLogger(ScmOrderWxNotifyClient.class);

    private static final String PATH = "/api/internal/order/wxNotify";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "scm-order-wx-notify");
        t.setDaemon(true);
        return t;
    });

    @Autowired
    private ScmAppNotifyProperties scmAppNotifyProperties;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 当前事务提交后再回调，避免 SCM 读不到未提交订单。
     */
    public void notifyOrderCreatedAfterCommit(final Long orderId)
    {
        if (orderId == null)
        {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive())
        {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
            {
                @Override
                public void afterCommit()
                {
                    notifyOrderCreatedAsync(orderId);
                }
            });
            return;
        }
        notifyOrderCreatedAsync(orderId);
    }

    public void notifyOrderCreatedAsync(final Long orderId)
    {
        if (orderId == null)
        {
            return;
        }
        if (!scmAppNotifyProperties.isConfigured())
        {
            log.warn("未配置 scminterface.scm.app.url/api-key，跳过订单微信通知 orderId={}", orderId);
            return;
        }
        EXECUTOR.execute(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    String url = scmAppNotifyProperties.normalizedUrl() + PATH;
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("X-Scm-Internal-Key", scmAppNotifyProperties.getApiKey());
                    Map<String, Object> body = new HashMap<String, Object>();
                    body.put("orderId", orderId);
                    restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<Map<String, Object>>(body, headers),
                        String.class);
                    log.info("已回调 SCM 订单微信通知 orderId={}", orderId);
                }
                catch (Exception e)
                {
                    log.warn("回调 SCM 订单微信通知失败 orderId={}: {}", orderId, e.getMessage());
                }
            }
        });
    }
}
