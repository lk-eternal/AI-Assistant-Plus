package lk.eternal.ai.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.MappedInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${api.allowed_origins}")
    private String allowedOrigins;

    private final IpRateLimitInterceptor ipRateLimitInterceptor;

    public WebConfig(IpRateLimitInterceptor ipRateLimitInterceptor) {
        this.ipRateLimitInterceptor = ipRateLimitInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedHeaders("*")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("*");
        registry.addMapping("/user/**")
                .allowedHeaders("*")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/")
                .addResourceLocations("classpath:/static/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MappedInterceptor(new String[]{"/api/**", "/user/**"}, ipRateLimitInterceptor));
    }

    @Component
    public static class IpRateLimitInterceptor implements HandlerInterceptor, DisposableBean {

        private final Map<String, AtomicInteger> requestCountMap = new ConcurrentHashMap<>();

        private static final int MAX_REQUESTS_PER_SECOND = 10;
        private static final int INTERVAL_SECOND = 60;

        private final ScheduledFuture<?> cleanupFuture;

        public IpRateLimitInterceptor(TaskScheduler taskScheduler) {
            cleanupFuture = taskScheduler.scheduleAtFixedRate(requestCountMap::clear, Duration.ofSeconds(INTERVAL_SECOND));
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            String ip = request.getHeader("X-Real-IP");
            if (!StringUtils.hasText(ip)) {
                response.sendError(HttpStatus.CONFLICT.value(), "No IP!!!");
                return false;
            }
            AtomicInteger requestCount = requestCountMap.computeIfAbsent(ip, k -> new AtomicInteger(0));
            if (requestCount.incrementAndGet() > MAX_REQUESTS_PER_SECOND) {
                response.sendError(HttpStatus.TOO_MANY_REQUESTS.value());
                return false;
            }
            return true;
        }

        @Override
        public void destroy() {
            cleanupFuture.cancel(true);
        }
    }
}
