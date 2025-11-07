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

            System.out.println("===========Debug===================");
            System.out.println("routes(" + routes.size() + " routes)");
        } catch (Exception e) {
            throw new ServletException("Erreur pendant l'initialisation du FrontServlet", e);
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.isEmpty()) path = "/index";

        MethodMapping mapping = routeMap.get(path);
        if (mapping != null) {
            try {
                Object controllerInstance = mapping.getController().getDeclaredConstructor().newInstance();
                Method method = mapping.getMethod();
                method.invoke(controllerInstance, request, response);
                return;
            } catch (Exception e) {
                throw new ServletException("Erreur lors de l'exécution du contrôleur pour : " + path, e);
            }
        }

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
}
