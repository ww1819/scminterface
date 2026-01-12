package com.scminterface.framework.web.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.scminterface.common.constant.Constants;
import com.scminterface.common.utils.StringUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * token验证处理
 *
 * @author scminterface
 */
@Component
public class TokenService
{
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    // 令牌自定义标识
    @Value("${token.header}")
    private String header;

    // 令牌秘钥
    @Value("${token.secret}")
    private String secret;

    // 令牌有效期（默认120分钟，即2小时）
    @Value("${token.expireTime}")
    private int expireTime;

    protected static final long MILLIS_SECOND = 1000;

    protected static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;

    /**
     * 验证令牌
     *
     * @param token 令牌
     * @return 是否有效
     */
    public boolean validateToken(String token)
    {
        try
        {
            Claims claims = parseToken(token);
            if (claims != null)
            {
                // 检查是否过期
                Date expiration = claims.getExpiration();
                if (expiration != null && expiration.before(new Date()))
                {
                    log.warn("令牌已过期");
                    return false;
                }
                return true;
            }
        }
        catch (Exception e)
        {
            log.warn("验证令牌异常: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 从令牌中获取数据声明
     *
     * @param token 令牌
     * @return 数据声明
     */
    public Claims parseToken(String token)
    {
        try
        {
            return Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
        }
        catch (Exception e)
        {
            log.warn("解析令牌异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从令牌中获取用户名
     *
     * @param token 令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token)
    {
        Claims claims = parseToken(token);
        if (claims != null)
        {
            return claims.getSubject();
        }
        return null;
    }

    /**
     * 创建令牌
     *
     * @param username 用户名
     * @return 令牌
     */
    public String createToken(String username)
    {
        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.JWT_USERNAME, username);
        claims.put(Constants.JWT_CREATED, new Date());
        return createToken(claims);
    }

    /**
     * 从数据声明生成令牌
     *
     * @param claims 数据声明
     * @return 令牌
     */
    private String createToken(Map<String, Object> claims)
    {
        Date expirationDate = new Date(System.currentTimeMillis() + expireTime * MILLIS_MINUTE);
        String token = Jwts.builder()
                .setClaims(claims)
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS512, secret).compact();
        return token;
    }

    /**
     * 获取请求token
     *
     * @param request
     * @return token
     */
    public String getToken(HttpServletRequest request)
    {
        String token = request.getHeader(header);
        if (StringUtils.isNotEmpty(token) && token.startsWith(Constants.TOKEN_PREFIX))
        {
            token = token.replace(Constants.TOKEN_PREFIX, "");
        }
        return token;
    }
}

