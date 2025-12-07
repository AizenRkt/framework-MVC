package etu.sprint.framework.scanner;

import etu.sprint.framework.annotation.AnnotationType;
import etu.sprint.framework.annotation.HttpMethod;
import etu.sprint.framework.annotation.AnnotationMethod;
import etu.sprint.framework.annotation.MyRequestParam;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.lang.reflect.Method;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ScannerController {

    public Map<String, MethodMapping> scanControllers(String basePackage) {
        
        Map<String, MethodMapping> routes = new HashMap<>();
        Set<Class<?>> controllers = findClassesWithAnnotation(basePackage, AnnotationType.class);

        for (Class<?> controller : controllers) {
            AnnotationType at = controller.getAnnotation(AnnotationType.class);
            String prefix = at.value();

            for (Method method : controller.getDeclaredMethods()) {
                if (method.isAnnotationPresent(AnnotationMethod.class)) {
                    AnnotationMethod am = method.getAnnotation(AnnotationMethod.class);
                    String fullPath = prefix + am.value();

                    // Inspect parameters for @MyRequestParam and build index->name map
                    java.lang.reflect.Parameter[] params = method.getParameters();
                    Map<Integer, String> paramMap = new HashMap<>();
                    for (int i = 0; i < params.length; i++) {
                        MyRequestParam rpa = params[i].getAnnotation(MyRequestParam.class);
                        if (rpa != null) {
                            String name = rpa.name();
                            if (name != null && !name.isEmpty()) {
                                paramMap.put(i, name);
                            }
                        }
                    }

                    HttpMethod http = method.getAnnotation(HttpMethod.class);
                    String httpMethod = (http != null) ? http.value().toUpperCase() : "GET";

                    String key = httpMethod + ":" + fullPath;

                    routes.put(key, new MethodMapping(controller, method, fullPath, paramMap, httpMethod));
                    System.out.println("Mapped route: " + fullPath + " [" + httpMethod + "] -> " + controller.getName() + "." + method.getName());
                }
            }
        }

        return routes;
    }

    private Set<Class<?>> findClassesWithAnnotation(String basePackage, Class<?> annotation) {
        Set<Class<?>> classes = new HashSet<>();
        try {
            String path = basePackage.replace('.', '/');
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String filePath = URLDecoder.decode(resource.getFile(), "UTF-8");
                File dir = new File(filePath);

                if (dir.exists()) {
                    for (File file : Objects.requireNonNull(dir.listFiles())) {
                        if (file.getName().endsWith(".class")) {
                            String className = basePackage + '.' + file.getName().replace(".class", "");
                            try {
                                Class<?> cls = Class.forName(className);
                                if (cls.isAnnotationPresent((Class) annotation)) {
                                    classes.add(cls);
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                } else if (filePath.contains(".jar!")) {
                    String jarPath = filePath.substring(5, filePath.indexOf("!"));
                    try (JarFile jar = new JarFile(jarPath)) {
                        Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            if (name.startsWith(path) && name.endsWith(".class")) {
                                String className = name.replace('/', '.').replace(".class", "");
                                try {
                                    Class<?> cls = Class.forName(className);
                                    if (cls.isAnnotationPresent((Class) annotation)) {
                                        classes.add(cls);
                                    }
                                } catch (Throwable ignored) {}
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classes;
    }
}
