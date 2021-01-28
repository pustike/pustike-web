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
import io.github.pustike.web.server.ServerService;
import io.github.pustike.web.server.WebApplication;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Sample Application with a simple {@link UserController} to list users.
 *
 * When run on a localhost, the API can be accessed using: http://localhost:8080/api/user/list
 */
@Path("/api")
public class SampleApplication extends WebApplication {
    public static void main(String[] args) {
        ServerService.run(new JettyApplicationServer(new SampleApplication()), args);
    }

    @Override
    public List<Module> getModules() {
        Module controllerModule = binder -> {
            binder.bind(UserController.class);
            // add other controllers here.
        };
        return List.of(controllerModule);
    }

    @Override
    public String getResourceBase() {
        try {
            return SampleApplication.class.getResource("/webapp").toURI().toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }
}
