package etu.sprint.framework;

import etu.sprint.framework.annotation.AnnotationType;
import etu.sprint.framework.annotation.AnnotationMethod;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class FrontServlet extends HttpServlet {

    // Map qui stocke les routes : URL -> (Classe, Méthode)
    private final Map<String, MethodMapping> routeMap = new HashMap<>();

    @Override
    public void init() throws ServletException {
        super.init();

        String basePackage = "etu.sprint.controller"; // ton package de contrôleurs
        Set<Class<?>> controllers = findClassesWithAnnotation(basePackage, AnnotationType.class);

        for (Class<?> controller : controllers) {
            AnnotationType at = controller.getAnnotation(AnnotationType.class);
            String prefix = at.value(); // préfixe de route de la classe

            for (Method method : controller.getDeclaredMethods()) {
                if (method.isAnnotationPresent(AnnotationMethod.class)) {
                    AnnotationMethod am = method.getAnnotation(AnnotationMethod.class);
                    String fullPath = prefix + am.value();
                    routeMap.put(fullPath, new MethodMapping(controller, method));
                    System.out.println("Mapped route: " + fullPath + " -> " + controller.getName() + "." + method.getName());
                }
            }
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.isEmpty()) path = "/index";

        MethodMapping mapping = routeMap.get(path);
        if (mapping != null) {
            try {
                Object controllerInstance = mapping.controller.getDeclaredConstructor().newInstance();
                mapping.method.invoke(controllerInstance, request, response);
                return;
            } catch (Exception e) {
                throw new ServletException("Erreur lors de l'exécution du contrôleur pour l'URL : " + path, e);
            }
        }

        // Si aucun contrôleur ne correspond, servir la page par défaut
        defaultServe(request, response);
    }

    private void defaultServe(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try (PrintWriter out = response.getWriter()) {
            out.println("<html><head><title>FrontServlet</title></head><body>");
            out.println("<h1>Voici la page par défaut</h1>");
            out.println("<p>Méthode HTTP : " + request.getMethod() + "</p>");
            out.println("<p>URL : " + request.getRequestURL() + "</p>");
            out.println("</body></html>");
        }
    }

    // Classe interne pour stocker la paire (Classe, Méthode)
    private static class MethodMapping {
        Class<?> controller;
        Method method;

        MethodMapping(Class<?> controller, Method method) {
            this.controller = controller;
            this.method = method;
        }
    }

    // Méthode pour trouver toutes les classes annotées @AnnotationType
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
