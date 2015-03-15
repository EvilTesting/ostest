/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.security.Security;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Web application lifecycle listener.
 *
 * @author administrador
 */
public class ContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Security.setProperty("ssl.SocketFactory.provider", "security.SSLSocketFactorySimple");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
//        sce.getServletContext().
    }
}
