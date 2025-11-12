/*
 * Copyright (c) 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.pustike.web.server;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Scanner;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.github.pustike.web.servlet.DispatcherServlet;
import io.github.pustike.web.servlet.WebModuleConfigurer;
import org.eclipse.jetty.compression.server.CompressionHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;

/**
 * The Jetty Application server.
 */
public class JettyApplicationServer implements ApplicationServer {
    private static final Logger logger = System.getLogger(JettyApplicationServer.class.getName());
    private static final String CONTEXT_PATH = "/";
    private final WebApplication webApplication;
    private Server server;

    /**
     * Constructs an instance with the given web application configuration.
     * @param webApplication a user provided web application instance
     */
    public JettyApplicationServer(WebApplication webApplication) {
        this.webApplication = Objects.requireNonNull(webApplication);
    }

    @Override
    public void start(int serverPort) {
        try {
            logger.log(Level.DEBUG, "Starting server at port {}", serverPort);
            server = createServer(serverPort);
            server.start();
            Runtime.getRuntime().addShutdownHook(newServerStopperThread(false));
            logger.log(Level.DEBUG, "Server started at port {}", serverPort);
            server.join();
        } catch (Exception ex) {
            logger.log(Level.ERROR, "error while starting the server: ", ex);
            System.exit(1);
        }
    }

    private Server createServer(int serverPort) {
        Server server = new Server(serverPort);
        // configure the ProxyConnectionFactory when Jetty server is behind a Load Balancer
        CompressionHandler compressionHandler = new CompressionHandler();
        compressionHandler.setHandler(createServletContextHandler());
        server.setHandler(compressionHandler);
        return server;
    }

    private ServletContextHandler createServletContextHandler() {
        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.setContextPath(CONTEXT_PATH);
        contextHandler.setErrorHandler(null);
        ServletContextListener contextListener = webApplication.getContextListener();
        if (contextListener != null) {
            contextHandler.addEventListener(contextListener);
        }
        ServletHolder servletHolder = new ServletHolder(DispatcherServlet.class);
        MultipartConfigElement multipartConfig = webApplication.getMultipartConfig();
        if(multipartConfig != null) {
            servletHolder.getRegistration().setMultipartConfig(multipartConfig);
        }
        servletHolder.setInitOrder(1);
        contextHandler.addServlet(servletHolder, "/*");
        contextHandler.addServlet(new ServletHolder(new ServerStopperServlet(this)), "/stopServer");
        String resourceBase = webApplication.getResourceBase();
        if (resourceBase != null) {
            contextHandler.setBaseResourceAsString(resourceBase);
        }
        contextHandler.setAttribute(WebModuleConfigurer.class.getSimpleName(), webApplication);
        return contextHandler;
    }

    private Thread newServerStopperThread(boolean doExitOnStop) {
        return new Thread(() -> {
            int exitStatus = 0;
            if (server != null) {
                try {
                    if (doExitOnStop) {
                        Thread.sleep(100);// wait until the request is served!
                    }
                    if (server.isStopping()) {
                        return;
                    }
                    logger.log(Level.DEBUG, "stopping the application server...");
                    server.stop();
                    logger.log(Level.DEBUG,"the application server is stopped.");
                } catch (Exception ex) {
                    logger.log(Level.ERROR,"error while stopping the server: ", ex);
                    exitStatus = 1;
                }
                server = null;
            }
            if (doExitOnStop) {
                System.exit(exitStatus);
            }
        }, "server-stopper");
    }

    /**
     * Application Server stopper servlet.
     */
    private static final class ServerStopperServlet extends HttpServlet {
        private final JettyApplicationServer applicationServer;

        private ServerStopperServlet(JettyApplicationServer applicationServer) {
            this.applicationServer = applicationServer;
        }

        @Override
        protected void service(HttpServletRequest request, HttpServletResponse response) {
            String loopbackAddress = InetAddress.getLoopbackAddress().getHostAddress();
            if (loopbackAddress.equals(request.getRemoteAddr())) {
                // review: check if additional controls are required here
                applicationServer.newServerStopperThread(true).start();
            }
        }
    }

    @Override
    public void stop(int serverPort) {
        // Stop the currently running server on the given port.
        logger.log(System.Logger.Level.DEBUG, "sending request to stop the application server...");
        Scanner scanner = null;
        int exitStatus = 0;
        try {
            System.setProperty("java.net.preferIPv4Stack", "true");
            String loopbackAddress = InetAddress.getLoopbackAddress().getHostAddress();
            String pathUrl = "http://" + loopbackAddress + ':' + serverPort + '/'; // CONTEXT_PATH
            String stopServerUrl = pathUrl + (pathUrl.endsWith("/") ? "stopServer" : "/stopServer");
            URLConnection urlConnection = new URL(stopServerUrl).openConnection();
            urlConnection.setUseCaches(false);
            scanner = new Scanner(urlConnection.getInputStream(), StandardCharsets.UTF_8.name());
            // String response = scanner.useDelimiter("\\Z").next();
            // logger.debug("stopped the server with response: " + response);
        } catch (Exception ex) {
            logger.log(System.Logger.Level.ERROR, "error while stopping the server!", ex);
            exitStatus = 1;
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
        System.exit(exitStatus);
    }
}
