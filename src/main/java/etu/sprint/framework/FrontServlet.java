package etu.sprint.framework;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Map;

import etu.sprint.framework.scanner.*;

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
                System.out.println("URL: " + entry.getKey()
                        + " => " + entry.getValue().getController().getSimpleName()
                        + "." + entry.getValue().getMethod().getName());
            }
            System.out.println("========================================");
        } catch (Exception e) {
            throw new ServletException("Erreur pendant l'initialisation du FrontServlet", e);
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.isEmpty()) path = "/index";

        try (PrintWriter out = response.getWriter()) {
            out.println("<html><head><title>FrontServlet Debug</title></head><body>");
            out.println("<h1>FrontServlet Demo</h1>");
            out.println("<p><strong>Requête :</strong> " + path + "</p>");

            out.println("<h2>Liste des méthodes connues :</h2>");
            out.println("<ul>");
            for (Map.Entry<String, MethodMapping> entry : routeMap.entrySet()) {
                MethodMapping map = entry.getValue();
                out.println("<li><strong>" + entry.getKey() + "</strong> → "
                        + map.getController().getSimpleName() + "."
                        + map.getMethod().getName() + "()</li>");
            }
            out.println("</ul>");

            MethodMapping mapping = routeMap.get(path);
            if (mapping != null) {
                out.println("<hr><h2>Exécution de la méthode cible :</h2>");
                try {
                    Object controllerInstance = mapping.getController().getDeclaredConstructor().newInstance();
                    Method method = mapping.getMethod();

                    if (method.getParameterCount() == 0) {
                        Object result = method.invoke(controllerInstance);
                        out.println("<p>Résultat : " + (result != null ? result.toString() : "(aucun retour)") + "</p>");
                    } else {
                        out.println("<p>La méthode " + method.getName()
                                + " attend des paramètres — exécution ignorée.</p>");
                    }

                } catch (Exception e) {
                    out.println("<p style='color:red;'>Erreur lors de l’exécution : "
                            + e.getMessage() + "</p>");
                    e.printStackTrace(out);
                }
            } else {
                out.println("<p>Aucune méthode correspondante trouvée pour l’URL.</p>");
            }

            out.println("</body></html>");
        }
    }
}
