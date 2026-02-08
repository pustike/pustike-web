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
package io.github.pustike.web.app;

import io.github.pustike.inject.bind.Module;
import io.github.pustike.web.Path;
import io.github.pustike.web.server.JettyApplicationServer;
import io.github.pustike.web.server.JettyContextConfigurer;
import io.github.pustike.web.server.ServerService;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.util.resource.URLResourceFactory;

import java.util.List;

/**
 * Sample Application with a simple {@link UserController} to list users. When run on a localhost, the API can be
 * accessed using this link: <a href="http://localhost:8080/api/user/list">localhost:8080/api/user/list</a>
 */
@Path("/api")
public class SampleApplication implements JettyContextConfigurer {
    public static void main(String[] args) {
        ServerService.run(new JettyApplicationServer(new SampleApplication()), args);
    }

    @Override
    public Handler configure(ServletContextHandler contextHandler, ServletHolder servletHolder) {
        // contextHandler.addEventListener(contextListener);
        contextHandler.setBaseResource(new URLResourceFactory().newResource(
                getClass().getResource("/META-INF/resources")));
        // servletHolder.getRegistration().setMultipartConfig(multipartConfig);
        /*
        CompressionHandler compressionHandler = new CompressionHandler();
        compressionHandler.setHandler(contextHandler);
        */
        return contextHandler;
    }

    @Override
    public List<Module> getModules() {
        Module controllerModule = binder -> {
            binder.bind(UserController.class);
            // add other controllers here.
        };
        return List.of(controllerModule);
    }
}
