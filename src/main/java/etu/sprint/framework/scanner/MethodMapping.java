package etu.sprint.framework.scanner;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

public class MethodMapping {
    private final Class<?> controller;
    private final Method method;
    private final String urlPattern; 
    private final Map<Integer, String> requestParamNames;
    private final String httpMethod; 

    public MethodMapping(Class<?> controller, Method method, String urlPattern, Map<Integer, String> requestParamNames, String httpMethod) {
        this.controller = controller;
        this.method = method;
        this.urlPattern = urlPattern;
        this.requestParamNames = requestParamNames == null ? Collections.emptyMap() : requestParamNames;
        this.httpMethod = httpMethod == null ? "GET" : httpMethod.toUpperCase(); 
    }

    public MethodMapping(Class<?> controller, Method method, String urlPattern) {
        this(controller, method, urlPattern, Collections.emptyMap(), "GET");
    }

    public MethodMapping(Class<?> controller, Method method, String urlPattern, Map<Integer, String> requestParamNames) {
        this(controller, method, urlPattern, requestParamNames, "GET");
    }

    public Class<?> getController() {
        return controller;
    }

    public Method getMethod() {
        return method;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public Map<Integer, String> getRequestParamNames() {
        return requestParamNames;
    }

    public String getHttpMethod() { // <-- Getter pour la méthode HTTP
        return httpMethod;
    }
}
