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

import etu.sprint.framework.scanner.*;
import etu.sprint.framework.utils.ModelView;

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

    private void defaultServe(HttpServletRequest request, HttpServletResponse response) throws IOException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, ServletException {

        response.setContentType("text/html;charset=UTF-8");
        String url = request.getRequestURI().substring(request.getContextPath().length());

        ServletContext context = getServletContext();
        Map<String, MethodMapping> routes = (Map<String, MethodMapping>) context.getAttribute("ROUTES");

        if (!routes.containsKey(url)) {
            try (PrintWriter out = response.getWriter()) {
                out.println("<html><head><title>404</title></head><body>");
                out.println("<h1>404 - Not found</h1>");
                out.println("</body></html>");
            }
            return;
        }

        MethodMapping mapping = routes.get(url);
        Class<?> cls = mapping.getController();
        Method method = mapping.getMethod();
        Object instance = cls.getDeclaredConstructor().newInstance();

        if (method.getReturnType().equals(String.class)) {
            Object result = method.invoke(instance);

            try (PrintWriter out = response.getWriter()) {
                out.println("<html><head><title>FrontServlet</title></head><body>");
                out.println("<h1>URL trouvée</h1>");
                out.println("<p>URL : " + url + "</p>");
                out.println("<p>Contrôleur : " + cls.getSimpleName() + "</p>");
                out.println("<p>Méthode : " + method.getName() + "()</p>");
                out.println("<p>Résultat : " + result + "</p>");
                out.println("</body></html>");
            }
            return;
        }

        if (method.getReturnType().equals(ModelView.class)) {
                
            System.out.println("===============model view ito=============");
            ModelView modelView = (ModelView) method.invoke(instance);

            if (modelView.getData() != null) {
                for (Map.Entry<String, Object> entry : modelView.getData().entrySet()) {
                    request.setAttribute(entry.getKey(), entry.getValue());
                }
            }

            String v = "/WEB-INF/views/" + modelView.getView();

            RequestDispatcher rd = request.getRequestDispatcher(v);
            rd.forward(request, response);
            return;
        }


        try (PrintWriter out = response.getWriter()) {
            out.println("<html><body>");
            out.println("<h1>Erreur : la méthode retourne un type non géré</h1>");
            out.println("</body></html>");
        }
    }
}
