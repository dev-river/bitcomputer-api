package com.bitcomputer.portal.security;

import com.bitcomputer.portal.employee.Employee;
import com.bitcomputer.portal.employee.EmployeeMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final EmployeeMapper employeeMapper;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, EmployeeMapper employeeMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.employeeMapper = employeeMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        JwtTokenProvider.Claims claims;
        try {
            claims = jwtTokenProvider.parse(header.substring(7));
        } catch (JwtException | IllegalArgumentException e) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "ERR_UNAUTHORIZED", "유효하지 않은 토큰입니다");
            return;
        }

        if ("EMPLOYEE".equals(claims.role())) {
            Employee employee = employeeMapper.findById(claims.employeeId());
            if (employee == null || "TERMINATED".equals(employee.getStatus())) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "ERR_FORBIDDEN", "퇴사 처리된 계정입니다");
                return;
            }
        }

        var principal = new AuthenticatedAccount(claims.accountId(), claims.role(), claims.employeeId());
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + claims.role()));
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, authorities));

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
            "{\"success\":false,\"data\":null,\"error\":{\"code\":\"%s\",\"message\":\"%s\"}}", code, message));
    }
}
