package lk.eternal.ai.util;


public class ContentTypeUtil {

    public static String type(String fileName) {
        String type = fileName.substring(fileName.lastIndexOf(".") + 1);
        return switch (type) {
            case "pdf" -> "application/pdf";
            case "png" -> "application/x-png";
            case "jpg", "jpeg", "jpe", "jfif" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "mp4", "mpv2" -> "video/mpeg4";
            case "avi" -> "video/avi";
            case "mp3" -> "audio/mp3";
            case "txt" -> "text/plain";
            case "svg" -> "text/xml";
            case "html" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "text/javascript";
            default -> "application/octet-stream";
        };
    }
}
