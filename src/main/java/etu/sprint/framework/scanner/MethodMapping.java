package etu.sprint.framework.scanner;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

public class MethodMapping {
    private final Class<?> controller;
    private final Method method;
    private final String urlPattern; // Nouveau champ pour le modèle d'URL dynamique
    // Map parameter index -> request parameter name (from @MyRequestParam)
    private final Map<Integer, String> requestParamNames;

    public MethodMapping(Class<?> controller, Method method, String urlPattern) {
        this(controller, method, urlPattern, Collections.emptyMap());
    }

    public MethodMapping(Class<?> controller, Method method, String urlPattern, Map<Integer, String> requestParamNames) {
        this.controller = controller;
        this.method = method;
        this.urlPattern = urlPattern;
        this.requestParamNames = requestParamNames == null ? Collections.emptyMap() : requestParamNames;
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
}
