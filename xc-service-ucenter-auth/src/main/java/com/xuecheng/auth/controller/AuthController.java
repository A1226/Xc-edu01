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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequestMapping("/")
public class AuthController implements AuthControllerApi{

    @Value("${auth.clientId}")
    private String clientId;

    @Value("${auth.cookieDomain}")
    private String cookieDomain;

    @Value("${auth.cookieMaxAge}")
    private int cookieMaxAge;

    @Value("${auth.clientSecret}")
    private String clientSecret;

    @Autowired
    AuthService authService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    //用户登录认证
    @Override
    @PostMapping("/userlogin")
    public LoginResult login(LoginRequest loginRequest) {
        if (loginRequest == null || StringUtils.isEmpty(loginRequest.getUsername())){
            ExceptionCast.cast(AuthCode.AUTH_USERNAME_NONE);
        }
        if (loginRequest == null || StringUtils.isEmpty(loginRequest.getPassword())){
            ExceptionCast.cast(AuthCode.AUTH_PASSWORD_NONE);
        }

        //申请令牌
        AuthToken authToken = authService.login(loginRequest.getUsername(),loginRequest.getPassword(),clientId,clientSecret );
        //将令牌存储到cookie
        String access_token = authToken.getAccess_token();
        this.saveCookie(access_token);

        return new LoginResult(CommonCode.SUCCESS,access_token);
    }

    //将令牌存储到cookie
    private void saveCookie(String token){
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
        //添加cookie，认证令牌，最后一个参数设置为false，表示允许浏览器获取
        CookieUtil.addCookie(response,cookieDomain,"/","uid",token,cookieMaxAge,false);
    }

    //退出
    @Override
    @PostMapping("/userlogout")
    public ResponseResult logout() {
        //先查后删 取出身份令牌
        String uid = this.getTokenFormCookie();
        //删除Redis的token
        authService.delToken(uid);
        //清除cookie
        this.clearCookie(uid);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //清除cookie
    private void clearCookie(String cookie){
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
        CookieUtil.addCookie(response,cookieDomain,"/","uid",cookie,0,false);
    }

    //从Redis中获取令牌
    @Override
    @GetMapping("/userjwt")
    public JwtResult userjwt() {
        //取出cookie中的用户身份令牌
        String uid = this.getTokenFormCookie();
        if (uid == null){
            return new JwtResult(CommonCode.FAIL,null);
        }
        //拿身份令牌从Redis中查询jwt令牌
        AuthToken userToken = authService.getUserToken(uid);
        if (userToken == null){
            return new JwtResult(CommonCode.FAIL,null);
        }
        //将jwt令牌返回给用户
        return new JwtResult(CommonCode.SUCCESS,userToken.getJwt_token());
    }

    private String getTokenFormCookie(){
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        Map<String, String> map = CookieUtil.readCookie(request, "uid");
        if (map != null && map.get("uid") != null){
            String uid = map.get("uid");
            return uid;
        }
        return null;
    }
}
