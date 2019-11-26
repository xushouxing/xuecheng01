package com.xuecheng.auth.controller;

import com.xuecheng.api.auth.AuthControllerApi;
import com.xuecheng.auth.service.AuthService;
import com.xuecheng.framework.domain.ucenter.ext.AuthToken;
import com.xuecheng.framework.domain.ucenter.request.LoginRequest;
import com.xuecheng.framework.domain.ucenter.response.AuthCode;
import com.xuecheng.framework.domain.ucenter.response.JwtResult;
import com.xuecheng.framework.domain.ucenter.response.LoginResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.framework.utils.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
public class AuthController implements AuthControllerApi {
    @Autowired
    private AuthService authService;
    @Value("${auth.clientId}")
    String clientId;
    @Value("${auth.clientSecret}")
    String clientSecret;
    @Value("${auth.cookieDomain}")
    String cookieDomain;
    @Value("${auth.tokenValiditySeconds}")
    int tokenValiditySeconds;
    @Value("${auth.cookieMaxAge}")
    int cookieMaxAge;

    @PostMapping("/userlogin")
    @Override
    public LoginResult login(LoginRequest loginRequest) {
        if (loginRequest == null || StringUtils.isBlank(loginRequest.getPassword())) {
            ExceptionCast.cast(AuthCode.AUTH_PASSWORD_NONE);
        }
        //校验密码是否输入
        if (StringUtils.isEmpty(loginRequest.getPassword())) {
            ExceptionCast.cast(AuthCode.AUTH_PASSWORD_NONE);
        }
        AuthToken authToken = authService.login(loginRequest.getUsername(), loginRequest.getPassword(), clientId, clientSecret);
        //将令牌写入cookie
        String access_token = authToken.getAccess_token();
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) (RequestContextHolder.getRequestAttributes());
        HttpServletResponse response = requestAttributes.getResponse();
        CookieUtil.addCookie(response,cookieDomain,"/","uid",access_token,cookieMaxAge,false);
        return new LoginResult(CommonCode.SUCCESS,access_token);
    }
    @Override
    @PostMapping("/userlogout")
    public ResponseResult logout() {
        String tokenFormCookie = getTokenFormCookie();
        if (StringUtils.isBlank(tokenFormCookie)){
            return new ResponseResult(CommonCode.SUCCESS);
        }
        //删除redis
        Boolean boo=authService.deleteRedis(tokenFormCookie);
        //删除cookes
        clearCookie(tokenFormCookie);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    private void clearCookie(String tokenFormCookie ) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) (RequestContextHolder.getRequestAttributes());
        HttpServletResponse response = requestAttributes.getResponse();
        CookieUtil.addCookie(response,cookieDomain,"/","uid",tokenFormCookie,0,false);
    }

    @Override
    @GetMapping("/userjwt")
    public JwtResult userjwt() {
        //从cookies中获取身份令牌
        String access_token=getTokenFormCookie();
        //从redis中获取token
        if (StringUtils.isBlank(access_token)){
            return new JwtResult(CommonCode.FAIL,null);
        }
        AuthToken authToken=authService.getUserToken(access_token);
        if (authToken==null){
            return new JwtResult(CommonCode.FAIL,null);
        }
        return new JwtResult(CommonCode.SUCCESS,authToken.getJwt_token());
    }
    //从cookies中获取身份令牌
    private String getTokenFormCookie() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) (RequestContextHolder.getRequestAttributes());
        HttpServletRequest request = requestAttributes.getRequest();
        Map<String, String> map = CookieUtil.readCookie(request, "uid");
        if(map!=null){
            return map.get("uid");
        }
        return null;
    }
}
