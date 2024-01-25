package lk.eternal.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.List;

@Configuration
public class Config {

    @Bean
    public ProxySelector proxySelector(@Value("${proxy.url}") String proxyUrl, @Value("${proxy.port}") Integer proxyPort) {
        if (proxyUrl != null && !proxyUrl.isBlank() && proxyPort != null) {
            return new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    return Collections.singletonList(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyUrl, proxyPort)));
                }

                @Override
                public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                }
            };
        }
        return ProxySelector.getDefault();
    }
}
