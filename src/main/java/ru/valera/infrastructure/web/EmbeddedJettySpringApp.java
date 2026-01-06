package ru.valera.infrastructure.web;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;

import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class EmbeddedJettySpringApp {

    public static void main(String[] args) throws Exception {

        Server server = new Server(8080);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
        rootContext.register(WebConfig.class);
        rootContext.setServletContext(context.getServletContext());

        context.addEventListener(new ContextLoaderListener(rootContext));

        ServletHolder dispatcher = new ServletHolder("dispatcher", new DispatcherServlet(rootContext));
        dispatcher.setInitOrder(1);
        context.addServlet(dispatcher, "/*");

        server.setHandler(context);

        server.start();
        server.join();
    }
}
