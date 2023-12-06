package lk.eternal.ai.plugin;


import lk.eternal.ai.dto.req.Parameters;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;

public class HttpPlugin implements Plugin {

    private final static HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofMinutes(1))
            .build();

    @Override
    public String name() {
        return "http";
    }

    @Override
    public String description() {
        return "访问对应网址的工具,参数是网址.请求成功会返回整个网页文本内容,请求失败会返回错误码,从中提取需要的信息.因此你获得了获取实时网页信息的能力,当且仅当用户需要查询实时信息时使用本工具.";
    }

    @Override
    public Parameters parameters() {
        return Parameters.singleton("url", "string", "网址");
    }

    @Override
    public String execute(Map<String, Object> args) {
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(args.get("url").toString()))
                    .build();
            final var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            final String body = response.body();
            if (body.isBlank()) {
                return String.valueOf(response.statusCode());
            }
            return html2Text(body);
        } catch (IOException | InterruptedException e) {
            return "请求出错了:" + e.getMessage();
        }
    }

    public static String html2Text(String inputString) {
        String htmlStr = inputString;
        String textStr = "";
        java.util.regex.Pattern p_script, p_style, p_html;
        java.util.regex.Matcher m_script, m_style, m_html;
        try {
            String regEx_script = "<[\\s]*?script[^>]*?>[\\s\\S]*?<[\\s]*?\\/[\\s]*?script[\\s]*?>";
            String regEx_style = "<[\\s]*?style[^>]*?>[\\s\\S]*?<[\\s]*?\\/[\\s]*?style[\\s]*?>";
            String regEx_html = "<[^>]+>";
            p_script = Pattern.compile(regEx_script, Pattern.CASE_INSENSITIVE);
            m_script = p_script.matcher(htmlStr);
            htmlStr = m_script.replaceAll("");
            p_style = Pattern.compile(regEx_style, Pattern.CASE_INSENSITIVE);
            m_style = p_style.matcher(htmlStr);
            htmlStr = m_style.replaceAll("");
            p_html = Pattern.compile(regEx_html, Pattern.CASE_INSENSITIVE);
            m_html = p_html.matcher(htmlStr);
            htmlStr = m_html.replaceAll("");
            textStr = htmlStr;
        } catch (Exception e) {
            System.err.println("Html2Text: " + e.getMessage());
        }
        textStr = textStr.replaceAll(" [ ]+", " ");
        textStr = textStr.replaceAll(" (?m)^\\s*$(\\n|\\r\\n)", "");
        return textStr;
    }
}
