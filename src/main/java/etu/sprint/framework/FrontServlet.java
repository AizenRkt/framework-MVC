package etu.sprint.framework;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FrontServlet extends HttpServlet {

    private static final String CONTROLLERS_PACKAGE = "com.example.controllers";

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        String url = requestURI.substring(contextPath.length());

        boolean matched = false;

        try {
            // Récupérer toutes les classes du package
            List<Class<?>> controllerClasses = getClasses(CONTROLLERS_PACKAGE);

            for (Class<?> controllerClass : controllerClasses) {
                Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();

                for (Method method : controllerClass.getMethods()) {
                    if (method.getReturnType() == String.class) {
                        String result = (String) method.invoke(controllerInstance);
                        String[] parts = result.split(";");
                        String route = parts[0];
                        String view = parts[1];

                        if (route.equals(url)) {
                            RequestDispatcher rd = request.getRequestDispatcher("/views/" + view);
                            rd.forward(request, response);
                            matched = true;
                            return; // stop dès qu'on trouve la route
                        }
                    }
                }
            }

            if (!matched) {
                response.setContentType("text/html;charset=UTF-8");
                try (PrintWriter out = response.getWriter()) {
                    out.println("<h2>URL inconnue : " + url + "</h2>");
                }
            }

        } catch (Exception e) {
            response.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = response.getWriter()) {
                out.println("<h2>Erreur dans le controller</h2>");
                e.printStackTrace(out);
            }
        }
    }

    // Méthode pour récupérer toutes les classes d'un package
    private List<Class<?>> getClasses(String packageName) throws ClassNotFoundException, IOException {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');
        URL resource = Thread.currentThread().getContextClassLoader().getResource(path);
        if (resource == null) return classes;

        File directory = new File(resource.getFile());
        if (!directory.exists()) return classes;

        for (File file : directory.listFiles()) {
            String fileName = file.getName();
            if (fileName.endsWith(".class")) {
                String className = packageName + '.' + fileName.substring(0, fileName.length() - 6);
                classes.add(Class.forName(className));
            }
        }

        return classes;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        service(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        service(request, response);
    }
}
