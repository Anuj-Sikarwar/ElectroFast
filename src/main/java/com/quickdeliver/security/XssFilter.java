package com.quickdeliver.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Pattern;

@Component
public class XssFilter implements Filter {

    private static final Pattern XSS_PATTERN = Pattern.compile(
            "<script>(.*?)</script>|src[\r\n]*=[\r\n]*\\'(.*?)\\'|src[\r\n]*=[\r\n]*\\\"(.*?)\\\"|" +
                    "eval\\((.*?)\\)|expression\\((.*?)\\)|javascript:|vbscript:|onload(.*?)=",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(new XssRequestWrapper((HttpServletRequest) request), response);
    }

    public static class XssRequestWrapper extends HttpServletRequestWrapper {

        public XssRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String[] getParameterValues(String parameter) {
            String[] values = super.getParameterValues(parameter);
            if (values == null) return null;
            String[] sanitized = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitized[i] = stripXss(values[i]);
            }
            return sanitized;
        }

        @Override
        public String getParameter(String parameter) {
            return stripXss(super.getParameter(parameter));
        }

        @Override
        public String getHeader(String name) {
            return stripXss(super.getHeader(name));
        }

        private String stripXss(String value) {
            if (value == null) return null;
            return XSS_PATTERN.matcher(value).replaceAll("");
        }
    }
}