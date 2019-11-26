package com.xuecheng.govern.gateway.service;

import com.xuecheng.framework.utils.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Service
public class AuthService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    /* 从cookie查询用户身份令牌是否存在，不存在则拒绝访问
       从http header查询jwt令牌是否存在，不存在则拒绝访问
       从Redis查询user_token令牌是否过期，过期则拒绝访问*/
    public String  getTokenFromCookie(HttpServletRequest request){
        Map<String, String> map = CookieUtil.readCookie(request, "uid");
        if (map==null || map.get("uid")==null){
            return null;
        }
        return map.get("uid");
    }
    public long getExpire(String access_token){
        String key="user_token:"+access_token;
        Long expire = redisTemplate.getExpire(key);
        return expire;
    }
    public String getJwtFromHeader(HttpServletRequest request){
        String authorization = request.getHeader("Authorization");
        if (StringUtils.isBlank(authorization) || !authorization.startsWith("Bearer")){
            return null;
        }
        return authorization;
    }
}
