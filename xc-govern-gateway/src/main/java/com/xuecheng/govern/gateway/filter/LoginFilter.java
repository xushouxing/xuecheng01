package com.xuecheng.govern.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.govern.gateway.service.AuthService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class LoginFilter extends ZuulFilter {
    @Autowired
    private AuthService authService;
    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.PRE_DECORATION_FILTER_ORDER - 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    /* 从cookie查询用户身份令牌是否存在，不存在则拒绝访问
       从http header查询jwt令牌是否存在，不存在则拒绝访问
       从Redis查询user_token令牌是否过期，过期则拒绝访问*/
    @Override
    public Object run() throws ZuulException {
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletRequest request = context.getRequest();
        //判断cookie中
        String tokenFromCookie = authService.getTokenFromCookie(request);
        if (StringUtils.isBlank(tokenFromCookie)){
            //拒接访问
            access_denied();
        }
        long expire = authService.getExpire(tokenFromCookie);
        if (expire<0){
            //拒接访问
            access_denied();
        }
        String jwtFromHeader = authService.getJwtFromHeader(request);
        if (StringUtils.isBlank(jwtFromHeader)){
            //拒接访问
            access_denied();
        }
        return null;
    }

    private void access_denied() {
        RequestContext context = RequestContext.getCurrentContext();
        context.setSendZuulResponse(false);
        //设置相应体内容
        ResponseResult responseResult=new ResponseResult(CommonCode.UNAUTHENTICATED);
        String s = JSON.toJSONString(responseResult);
        context.setResponseBody(s);
        //设置响应状态码
        context.setResponseStatusCode(org.springframework.http.HttpStatus.FORBIDDEN.value());
        //设置响应格式
        context.getResponse().setContentType("application/json;charset=utf-8");
    }
}
