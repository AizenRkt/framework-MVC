package etu.sprint.framework.scanner;

import java.lang.reflect.Method;

public class MethodMapping {
    private final Class<?> controller;
    private final Method method;

    public MethodMapping(Class<?> controller, Method method) {
        this.controller = controller;
        this.method = method;
    }

    public Class<?> getController() {
        return controller;
    }

    public Method getMethod() {
        return method;
    }
}
