package etu.sprint.framework;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import etu.sprint.framework.scanner.*;
import etu.sprint.framework.utils.*;

public class FrontServlet extends HttpServlet {

    private Map<String, MethodMapping> routeMap;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            String basePackage = "etu.sprint.controller";

            ScannerController scanner = new ScannerController();
            Map<String, MethodMapping> routes = scanner.scanControllers(basePackage);

            this.routeMap = routes;
            getServletContext().setAttribute("ROUTES", routes);

            System.out.println("=========== ROUTES DÉTECTÉES ===========");
            for (Map.Entry<String, MethodMapping> entry : routes.entrySet()) {
                System.out.println(
                    "URL: " + entry.getKey() + " => " +
                    entry.getValue().getController().getSimpleName() + "." +
                    entry.getValue().getMethod().getName()
                );
            }
            System.out.println("========================================");
        } catch (Exception e) {
            throw new ServletException("Erreur pendant l'initialisation du FrontServlet", e);
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (resourceExist(request)) {
            customServe(request, response);
        } else {
            try {
                defaultServe(request, response);
            } catch (Exception e) {
                throw new ServletException(e);
            }
        }
    }

    private boolean resourceExist(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return getServletContext().getResourceAsStream(path) != null;
    }

    private void customServe(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        request.getRequestDispatcher(request.getRequestURI().substring(request.getContextPath().length()))
                .forward(request, response);
    }

    private MethodMapping findMatchingRoute(String url, Map<String, MethodMapping> routes, Map<String, String> pathVariables) {
        // Vérifier d'abord les correspondances exactes
        if (routes.containsKey(url)) {
            return routes.get(url);
        }

        // Ensuite, vérifier les correspondances dynamiques (construire un regex robuste)
        for (Map.Entry<String, MethodMapping> entry : routes.entrySet()) {
            String routeKey = entry.getKey();
            String regex = "^" + routeKey.replaceAll("\\{[^/]+\\}", "([^/]+)") + "$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(url);

            if (matcher.matches()) {
                MethodMapping mapping = entry.getValue();

                // Récupérer les noms des variables dans l'ordre
                List<String> varNames = new ArrayList<>();
                Matcher nameMatcher = Pattern.compile("\\{([^/}]+)\\}").matcher(routeKey);
                while (nameMatcher.find()) {
                    varNames.add(nameMatcher.group(1));
                }

                // Associer chaque groupe regex (1-based) au nom de variable correspondant
                for (int i = 0; i < varNames.size(); i++) {
                    String value = null;
                    try {
                        value = matcher.group(i + 1);
                    } catch (IllegalStateException | IndexOutOfBoundsException ignored) {}
                    if (value != null) {
                        pathVariables.put(varNames.get(i), value);
                    }
                }

                return mapping;
            }
        }
        return null;
    }

    protected void defaultServe(HttpServletRequest request, HttpServletResponse response) throws IOException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, ServletException {
        response.setContentType("text/html;charset=UTF-8");
        String url = request.getRequestURI().substring(request.getContextPath().length());

        ServletContext context = getServletContext();
        Map<String, MethodMapping> routes = (Map<String, MethodMapping>) context.getAttribute("ROUTES");
        Map<String, String> pathVariables = new HashMap<>();

        MethodMapping mapping = findMatchingRoute(url, routes, pathVariables);

        if (mapping == null) {
            try (PrintWriter out = response.getWriter()) {
                out.println("<html><head><title>404</title></head><body>");
                out.println("<h1>404 - Not found</h1>");
                out.println("</body></html>");
            }
            return;
        }

        Class<?> cls = mapping.getController();
        Method method = mapping.getMethod();
        Object instance = cls.getDeclaredConstructor().newInstance();

        // Injecter les variables extraites dans les paramètres de la méthode
        Object[] parameters = new Object[method.getParameterCount()];
        Class<?>[] parameterTypes = method.getParameterTypes();

        // Obtenir la liste des noms de variables dans l'ordre depuis le pattern de la route
        List<String> varNames = new ArrayList<>();
        if (mapping.getUrlPattern() != null) {
            Matcher nm = Pattern.compile("\\{([^/}]+)\\}").matcher(mapping.getUrlPattern());
            while (nm.find()) {
                varNames.add(nm.group(1));
            }
        }

        for (int i = 0; i < parameters.length; i++) {
            if (i < varNames.size()) {
                String name = varNames.get(i);
                String value = pathVariables.get(name);
                if (value != null) {
                    parameters[i] = convertType(value, parameterTypes[i]);
                } else {
                    parameters[i] = null;
                }
            } else {
                parameters[i] = null; // pas de variable correspondante
            }
        }

        Object result = method.invoke(instance, parameters);

        if (method.getReturnType().equals(String.class)) {
            try (PrintWriter out = response.getWriter()) {
                out.println("<html><head><title>FrontServlet</title></head><body>");
                out.println("<h1>URL trouvée</h1>");
                out.println("<p>URL : " + url + "</p>");
                out.println("<p>Contrôleur : " + cls.getSimpleName() + "</p>");
                out.println("<p>Méthode : " + method.getName() + "()</p>");
                out.println("<p>Résultat : " + result + "</p>");
                out.println("</body></html>");
            }
        } else if (method.getReturnType().equals(ModelView.class)) {
            ModelView modelView = (ModelView) result;

            if (modelView.getData() != null) {
                for (Map.Entry<String, Object> entry : modelView.getData().entrySet()) {
                    request.setAttribute(entry.getKey(), entry.getValue());
                }
            }

            String v = "/WEB-INF/views/" + modelView.getView();
            RequestDispatcher rd = request.getRequestDispatcher(v);
            rd.forward(request, response);
        } else {
            try (PrintWriter out = response.getWriter()) {
                out.println("<html><body>");
                out.println("<h1>Erreur : la méthode retourne un type non géré</h1>");
                out.println("</body></html>");
            }
        }
    }

    private Object convertType(String value, Class<?> targetType) {
        if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
            return Integer.parseInt(value);
        } else if (targetType.equals(Double.class) || targetType.equals(double.class)) {
            return Double.parseDouble(value);
        } else if (targetType.equals(Boolean.class) || targetType.equals(boolean.class)) {
            return Boolean.parseBoolean(value);
        }
        return value;
    }
}
