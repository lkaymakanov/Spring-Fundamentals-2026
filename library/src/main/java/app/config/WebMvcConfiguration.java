package app.config;

import app.security.SessionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers Spring MVC interceptors for the application.
 *
 * Currently wires up a single SessionInterceptor that enforces authentication
 * on protected routes (everything except static assets).
 *
 * To add more interceptors (logging, locale, etc.), chain additional
 * registry.addInterceptor(...).addPathPatterns(...) calls in addInterceptors().
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final SessionInterceptor sessionInterceptor;

    public WebMvcConfiguration(SessionInterceptor sessionInterceptor) {
        this.sessionInterceptor = sessionInterceptor;
    }

    /**
     * Configures which paths the SessionInterceptor applies to.
     *
     * Path patterns:
     *  - "/**"  → apply to every request
     *  - "/css/**", "/images/**" → exclude static assets so they're served without auth
     *
     * If you add more static folders (e.g. /js, /fonts), exclude them here too.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/images/**");
    }
}