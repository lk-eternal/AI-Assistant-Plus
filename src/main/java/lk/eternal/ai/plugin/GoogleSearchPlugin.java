package lk.eternal.ai.plugin;


import lk.eternal.ai.dto.resp.SearchResponse;
import lk.eternal.ai.util.Mapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class GoogleSearchPlugin implements Plugin {

    private final static HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .proxy(ProxySelector.of(new InetSocketAddress("127.0.0.1", 1080)))
            .connectTimeout(Duration.ofMinutes(1))
            .build();

    private final String key;
    private final String cx;

    public GoogleSearchPlugin(String key, String cx) {
        this.key = key;
        this.cx = cx;
    }

    @Override
    public String name() {
        return "google";
    }

    @Override
    public String description() {
        return "基于谷歌API的搜索引擎,参数是搜索内容,返回相关内容网页的标题,简介和链接,可以进一步使用http工具打开链接查看具体内容;因此你获得了获取实时信息的能力,当用户需要查询实时信息时建议使用本工具.";
    }

    public String execute(String q) {
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s&start=1&num=10".formatted(this.key, this.cx, q)))
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
