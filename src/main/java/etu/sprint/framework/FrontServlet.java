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

    /*private MethodMapping findMatchingRoute(String url, Map<String, MethodMapping> routes, Map<String, String> pathVariables) throws ServletException {
        //verifier d'abord les correspondances exactes
        if (routes.containsKey(url)) {
            return routes.get(url);
        }

        //verifier les correspondances dynamiques
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
                        String varName = varNames.get(i);
                        pathVariables.put(varName, value);
                        // mettre aussi la variable dans la requête pour que les contrôleurs puissent l'obtenir via request.getAttribute
                        // (le request utilisé ici sera celui passé dans defaultServe)
                    }
                }

                if (varNames.size() != matcher.groupCount()) {
                    throw new ServletException("Path variable manquante pour " + routeKey);
                }


                return mapping;
            }
        }
        return null;
    }*/

    private MethodMapping findMatchingRoute(String url, String requestMethod, Map<String, MethodMapping> routes, Map<String, String> pathVariables) throws ServletException {
        // Vérifier les correspondances exactes avec méthode HTTP
        String keyExact = requestMethod + ":" + url;
        if (routes.containsKey(keyExact)) {
            return routes.get(keyExact);
        }

        // Vérifier les correspondances dynamiques (avec {id}, etc.)
        for (Map.Entry<String, MethodMapping> entry : routes.entrySet()) {
            String routeKey = entry.getKey();

            // Filtrer uniquement les routes correspondant à la méthode HTTP de la requête
            if (!routeKey.startsWith(requestMethod + ":")) {
                continue;
            }

            // Retirer la méthode HTTP pour tester l'URL seule
            String routeUrl = routeKey.substring(requestMethod.length() + 1);

            // Générer le regex pour matcher les variables {x}
            String regex = "^" + routeUrl.replaceAll("\\{[^/]+\\}", "([^/]+)") + "$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(url);

            if (matcher.matches()) {
                MethodMapping mapping = entry.getValue();

                // Récupérer les noms des variables dans l'ordre
                List<String> varNames = new ArrayList<>();
                Matcher nameMatcher = Pattern.compile("\\{([^/}]+)\\}").matcher(routeUrl);
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
        Map<String, MethodMapping> routes = (Map<String, MethodMapping>) getServletContext().getAttribute("ROUTES");
        Map<String, String> pathVariables = new HashMap<>();

        String requestMethod = request.getMethod().toUpperCase();
        String key = requestMethod + ":" + url;

        MethodMapping mapping = routes.get(key);
        if (mapping == null) {
            mapping = findMatchingRoute(url, requestMethod, routes, pathVariables);
        }

        if (mapping == null) {
            try (PrintWriter out = response.getWriter()) {
                out.println("<html><head><title>404</title></head><body>");
                out.println("<h1>404 - Not found</h1>");
                out.println("</body></html>");
            }
            return;
        }

        String expectedHttpMethod = mapping.getHttpMethod();
        if (!requestMethod.equals(expectedHttpMethod)) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            try (PrintWriter out = response.getWriter()) {
                out.println("<html><body>");
                out.println("<h1>405 - Method Not Allowed</h1>");
                out.println("<p>Attendu : " + expectedHttpMethod + "</p>");
                out.println("<p>Reçu : " + requestMethod + "</p>");
                out.println("</body></html>");
            }
            return;
        }

        Class<?> cls = mapping.getController();
        Method method = mapping.getMethod();
        Object instance = cls.getDeclaredConstructor().newInstance();

        // exposer les variables d'URL dans la requête pour que les contrôleurs puissent les lire via request.getAttribute("name")
        for (Map.Entry<String, String> e : pathVariables.entrySet()) {
            request.setAttribute(e.getKey(), e.getValue());
        }

        // Injecter les variables extraites et objects request/response dans les paramètres de la méthode
        Object[] parameters = new Object[method.getParameterCount()];
        Class<?>[] parameterTypes = method.getParameterTypes();

        // Obtenir les noms des variables dans l'ordre
        List<String> varNames = new ArrayList<>();
        if (mapping.getUrlPattern() != null) {
            Matcher nm = Pattern.compile("\\{([^/}]+)\\}").matcher(mapping.getUrlPattern());
            while (nm.find()) {
                varNames.add(nm.group(1));
            }
        }

        Map<Integer, String> annotatedParams = mapping.getRequestParamNames();

        int varIndex = 0;

        for (int i = 0; i < parameters.length; i++) {
            Class<?> pType = parameterTypes[i];
            Object finalValue = null;

            //Injection spéciale : HttpServletRequest / HttpServletResponse
            if (HttpServletRequest.class.isAssignableFrom(pType)) {
                parameters[i] = request;
                continue;
            }
            if (HttpServletResponse.class.isAssignableFrom(pType)) {
                parameters[i] = response;
                continue;
            }

            //PRIORITÉ 1 : @MyRequestParam(name = "x")
            String annotatedName = annotatedParams.get(i);
            if (annotatedName != null) {
                String val = request.getParameter(annotatedName);
                if (val == null) val = pathVariables.get(annotatedName);
                finalValue = convertType(val, pType);
                parameters[i] = finalValue;
                continue;
            }

            //PRIORITÉ 2 : Path variables dans l’ordre
            if (varIndex < varNames.size()) {
                String varName = varNames.get(varIndex++);
                String val = pathVariables.get(varName);
                finalValue = convertType(val, pType);
                parameters[i] = finalValue;
                continue;
            }

            //PRIORITÉ 3 : Paramètres GET/POST standards
            String paramName = parameterTypes[i].getSimpleName().toLowerCase(); 
            String val = request.getParameter(paramName);
            if (val != null) {
                parameters[i] = convertType(val, pType);
                continue;
            }

            //Valeur par défaut ou null
            if (pType.isPrimitive()) {
                parameters[i] = defaultPrimitiveValue(pType);
            } else {
                parameters[i] = null;
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
        if (value == null) {
            if (targetType.isPrimitive()) {
                return defaultPrimitiveValue(targetType);
            }
            return null;
        }

        if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
            return Integer.parseInt(value);
        } else if (targetType.equals(Double.class) || targetType.equals(double.class)) {
            return Double.parseDouble(value);
        } else if (targetType.equals(Boolean.class) || targetType.equals(boolean.class)) {
            return Boolean.parseBoolean(value);
        }
        return value;
    }

    private Object defaultPrimitiveValue(Class<?> primitiveType) {
        if (primitiveType.equals(int.class)) {
            return 0;
        } else if (primitiveType.equals(double.class)) {
            return 0.0d;
        } else if (primitiveType.equals(boolean.class)) {
            return false;
        } else if (primitiveType.equals(long.class)) {
            return 0L;
        } else if (primitiveType.equals(float.class)) {
            return 0f;
        } else if (primitiveType.equals(short.class)) {
            return (short) 0;
        } else if (primitiveType.equals(byte.class)) {
            return (byte) 0;
        } else if (primitiveType.equals(char.class)) {
            return '\u0000';
        }
        return null;
    }
}
