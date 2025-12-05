package etu.sprint.framework.scanner;

import java.lang.reflect.Method;

public class MethodMapping {
    private final Class<?> controller;
    private final Method method;
    private final String urlPattern; // Nouveau champ pour le modèle d'URL dynamique

    public MethodMapping(Class<?> controller, Method method, String urlPattern) {
        this.controller = controller;
        this.method = method;
        this.urlPattern = urlPattern;
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
}
