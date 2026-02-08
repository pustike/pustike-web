package io.github.pustike.web.server;

import io.github.pustike.web.servlet.WebModuleConfigurer;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Handler;

/**
 * Interface to make additional configuration changes to Jetty Context handler and dispatcher servlet holder.
 */
public interface JettyContextConfigurer extends WebModuleConfigurer {
    /**
     * Make additional configurations to context handler and dispatcher servlet holder.
     * @param contextHandler the servlet context holder
     * @param servletHolder the dispatcher servlet holder
     */
    Handler configure(ServletContextHandler contextHandler, ServletHolder servletHolder);
}
