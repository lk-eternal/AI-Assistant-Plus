package lk.eternal.ai.plugin;


import lk.eternal.ai.dto.req.Parameters;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;

public class HttpPlugin implements Plugin {

    private final static HttpClient HTTP_CLIENT;

    static {
        final var builder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMinutes(1));
        if (ProxySelector.getDefault() != null) {
            builder.proxy(ProxySelector.getDefault());
        }
        HTTP_CLIENT = builder.build();
    }

    @Override
    public String name() {
        return "http";
    }

    @Override
    public String description() {
        return "A web page access tool that retrieves the full text content of a webpage by sending a request to a specified URL. If the request fails, an error code is returned instead. With this tool, you can obtain real-time information from web pages and extract the necessary information from it. Note: Use this tool only when the user needs to query real-time information.";
    }

    @Override
    public Parameters parameters() {
        return Parameters.singleton("url", "string", "网址");
    }

    @Override
    public String execute(Object args) {
        String exp;
        if(args instanceof Map<?,?>){
            exp = ((Map<String, Object>)args).get("url").toString();
        }else{
            exp = args.toString();
        }
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(exp))
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
