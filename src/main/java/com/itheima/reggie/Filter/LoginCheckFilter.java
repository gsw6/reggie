package com.itheima.reggie.Filter;

import com.alibaba.fastjson.JSON;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@WebFilter(filterName = "LoginCheckFilter" ,urlPatterns = "/*")
public class LoginCheckFilter implements Filter {
    public static final AntPathMatcher Path_Matcher = new AntPathMatcher();
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse)servletResponse;
        HttpServletRequest request =(HttpServletRequest)servletRequest;
        String requestURL = request.getRequestURI();
        log.info("本次拦截到请求：{}",requestURL);
        String [] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/common/**",
                "/user/login",
                "/user/sendMsg"
        };
        boolean check= check(urls,requestURL);
        if(check){
            log.info("本次请求不需要处理：{}",requestURL);
            filterChain.doFilter(request,response);
            return;
        }
        if(request.getSession().getAttribute("employee") !=null){
            log.info("用户已登录，用户id为：{}",request.getSession().getAttribute("employee"));
            BaseContext.setCurrentId((long)request.getSession().getAttribute("employee"));
            filterChain.doFilter(request,response);
            return;
        }
        //判断用户是否登录
        if(request.getSession().getAttribute("user") != null){
            log.info("用户已登录，用户id为：{}",request.getSession().getAttribute("user"));
            Long userId = (Long)request.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);
            filterChain.doFilter(request,response);
            return;
        }
        log.info("用户需要登录");
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
    }
    public boolean check(String[] urls,String requestURL){
        for (String url : urls) {
            boolean match = Path_Matcher.match(url, requestURL);
            if(match){
                return true;
            }
        }
        return false;
    }
}
