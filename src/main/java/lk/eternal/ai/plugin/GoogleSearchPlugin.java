package lk.eternal.ai.plugin;


import lk.eternal.ai.dto.req.Parameters;
import lk.eternal.ai.dto.resp.SearchResponse;
import lk.eternal.ai.util.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class GoogleSearchPlugin implements Plugin {

    private final HttpClient HTTP_CLIENT;

    @Value("${google.search.key}")
    private String key;
    @Value("${google.search.cx}")
    private String cx;

    public GoogleSearchPlugin(ProxySelector proxySelector) {
        HTTP_CLIENT = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMinutes(1))
                .proxy(proxySelector).build();
    }

    @Override
    public String name() {
        return "google";
    }

    @Override
    public String prompt() {
        return "A search engine plugin based on Google API. When the user's question contains real-time content, it uses Google's powerful search capabilities to retrieve relevant information and return the title, summary, and link of the related web page. The plug-in can be used to query news, academic articles, technical documents, and other types of information to help users quickly find the content they need. Note: Use this tool only when the user's question contains real-time content. It is recommended to use English for searching.";
    }

    @Override
    public String description() {
        return "3.Google搜索";
    }

    @Override
    public Parameters parameters() {
        return Parameters.singleton("q", "string", "搜索内容");
    }

    @Override
    public String execute(Map<String, Object> args) {
        String exp = Optional.ofNullable(args.get("q"))
                .orElseGet(() -> args.get("value"))
                .toString();
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s&start=1&num=10".formatted(this.key, this.cx, URLEncoder.encode(exp, StandardCharsets.UTF_8))))
                    .build();
            final var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            final String body = response.body();
            if (body.isBlank()) {
                return String.valueOf(response.statusCode());
            }
            final var searchResponse = Mapper.readValueNotError(response.body(), SearchResponse.class);
            if (searchResponse == null) {
                return "无数据!";
            }
            return extractInformation(searchResponse);
        } catch (IOException | InterruptedException e) {
            return "请求出错了:" + e.getMessage();
        }
    }

    public String extractInformation(SearchResponse response) {
        StringBuilder sb = new StringBuilder();

        String title = response.getQueries().getRequest().get(0).getTitle();
        sb.append("title: ").append(title).append("\n\n");

        List<SearchResponse.Item> items = response.getItems();
        for (SearchResponse.Item item : items) {
            String link = item.getLink();
            String displayLink = item.getDisplayLink();
            String itemTitle = item.getTitle();
            String snippet = item.getSnippet();
            sb.append("link: ").append(link).append("\n");
            sb.append("displayLink: ").append(displayLink).append("\n");
            sb.append("title: ").append(itemTitle).append("\n");
            sb.append("snippet: ").append(snippet).append("\n\n");
        }

        return sb.toString();
    }
}
