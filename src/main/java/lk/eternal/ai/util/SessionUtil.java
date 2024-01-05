package lk.eternal.ai.util;


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.*;

public class SessionUtil {

    public static UUID getSessionId(HttpServletRequest request) {
        return Optional.ofNullable(request.getCookies())
                .map(Arrays::stream)
                .flatMap(cs -> cs
                        .filter(c -> c.getName().equals("LKSESSIONID"))
                        .findFirst()
                        .map(Cookie::getValue)
                        .map(TryUtils.all(UUID::fromString)))
                .orElse(null);
    }

    public static void setSessionId(UUID userId, HttpServletResponse response) {
        Cookie sessionCookie = new Cookie("LKSESSIONID", userId.toString());
        sessionCookie.setPath("/");
        sessionCookie.setSecure(true);
        sessionCookie.setHttpOnly(false);
        sessionCookie.setAttribute("SameSite", "None");
        response.addCookie(sessionCookie);
    }

}
