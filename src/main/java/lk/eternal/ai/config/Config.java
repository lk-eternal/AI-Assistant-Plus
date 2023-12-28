package lk.eternal.ai.config;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.net.InetSocketAddress;
import java.net.ProxySelector;

@Configurable
public class Config {

    @Bean
    public ProxySelector proxySelector(@Value("${proxy.url}") String proxyUrl, @Value("${proxy.port}") Integer proxyPort) {
        if (proxyUrl != null && !proxyUrl.isBlank() && proxyPort != null) {
            return ProxySelector.of(new InetSocketAddress(proxyUrl, proxyPort));
        }
        return ProxySelector.getDefault();
    }
}
