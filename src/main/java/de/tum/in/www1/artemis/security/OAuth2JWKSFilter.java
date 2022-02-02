package de.tum.in.www1.artemis.security;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filters requests to jwksUrl and serves the public JWKSet related to all OAuth2 clients.
 */
public class OAuth2JWKSFilter extends OncePerRequestFilter {

    private final AntPathRequestMatcher requestMatcher;

    private final Logger log = LoggerFactory.getLogger(OAuth2JWKSFilter.class);

    private final OAuth2JWKSService jwksService;

    private final String jwksUrl;

    public OAuth2JWKSFilter(String jwksUrl, OAuth2JWKSService jwksService) {
        this.requestMatcher = new AntPathRequestMatcher(jwksUrl);
        this.jwksUrl = jwksUrl;
        this.jwksService = jwksService;
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain)
            throws ServletException, IOException {
        if (!this.requestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!HttpMethod.GET.matches(request.getMethod())) {
            log.error(request.getMethod() + " " + this.jwksUrl + " is not allowed");
            response.sendError(HttpStatus.METHOD_NOT_ALLOWED.value());
            return;
        }

        log.info("GET " + jwksUrl);

        response.setContentType("application/json;charset=utf-8");
        PrintWriter responseWriter = response.getWriter();
        responseWriter.print(jwksService.getJwkSet().toPublicJWKSet().toJSONObject());
        responseWriter.flush();
    }
}
