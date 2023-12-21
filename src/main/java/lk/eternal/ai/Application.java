package lk.eternal.ai;

import com.sun.net.httpserver.HttpServer;
import lk.eternal.ai.handller.ApiHandler;
import lk.eternal.ai.handller.PoeHandler;
import lk.eternal.ai.handller.ResourceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Executors;

public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws IOException {
        initProperties(args);
        initProxy();

        HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
        server.createContext("/api", new ApiHandler());
        server.createContext("/poe", new PoeHandler());
        server.createContext("/", new ResourceHandler("static"));
        server.setExecutor(Executors.newFixedThreadPool(1000));
        server.start();
    }

    private static void initProperties(String[] args) {
        //项目内默认配置文件
        try (final var inputStream = Application.class.getClassLoader().getResourceAsStream("application.properties")) {
            final var properties = new Properties();
            properties.load(inputStream);
            for (String key : properties.stringPropertyNames()) {
                String value = properties.getProperty(key);
                System.setProperty(key, value);
            }
        } catch (IOException e) {
            LOGGER.warn("Read inner application.properties error:{}", e.getMessage());
        }

        //项目外默认配置文件
        try (final var inputStream = new FileInputStream("application.properties")) {
            final var properties = new Properties();
            properties.load(inputStream);
            for (String key : properties.stringPropertyNames()) {
                String value = properties.getProperty(key);
                System.setProperty(key, value);
            }
        } catch (IOException e) {
            LOGGER.warn("Read outer application.properties error:{}", e.getMessage());
        }

        final var active = Arrays.stream(args)
                .filter(a -> a.startsWith("profiles.active="))
                .findFirst()
                .map(a -> a.split("=")[1])
                .orElseGet(() -> System.getenv("profiles.active"));
        if (active != null && !active.isBlank()) {
            LOGGER.info("profiles.active: {}", active);
            final var filePath = "application-" + active + ".properties";
            LOGGER.info("Read properties file: {}", filePath);
            //项目内指定配置文件
            try (final var inputStream = Application.class.getClassLoader().getResourceAsStream(filePath)) {
                final var properties = new Properties();
                properties.load(inputStream);
                for (String key : properties.stringPropertyNames()) {
                    String value = properties.getProperty(key);
                    System.setProperty(key, value);
                }
            } catch (IOException e) {
                LOGGER.warn("Read inner {} error:{}", filePath, e.getMessage());
            }

            //项目外指定配置文件
            try (final var inputStream = new FileInputStream(filePath)) {
                final var properties = new Properties();
                properties.load(inputStream);
                for (String key : properties.stringPropertyNames()) {
                    String value = properties.getProperty(key);
                    System.setProperty(key, value);
                }
            } catch (IOException e) {
                LOGGER.warn("Read outer {} error:{}", filePath, e.getMessage());
            }
        }

        //启动参数
        for (String arg : args) {
            final var split = arg.split("=");
            if (split.length == 2) {
                System.setProperty(split[0], split[1]);
            }
        }
    }

    private static void initProxy() {
        final var proxyUrl = System.getProperty("proxy.url");
        final var proxyPort = System.getProperty("proxy.port");
        if (proxyUrl != null && !proxyUrl.isBlank() && proxyPort != null && !proxyPort.isBlank()) {
            ProxySelector.setDefault(ProxySelector.of(new InetSocketAddress(proxyUrl, Integer.parseInt(proxyPort))));
        }
    }


}
