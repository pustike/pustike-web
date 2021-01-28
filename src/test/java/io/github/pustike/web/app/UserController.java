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

import io.github.pustike.json.JsonInclude;
import io.github.pustike.web.*;

import java.util.List;

/**
 * Sample resource at api/user/list.
 */
@Path("/user")
public class UserController {

    @GET
    @Path("/list")
    @JsonContext("default")
    public List<User> getUsers(@QueryParam("path") String path) {
        return List.of(new User("test1", "test1@test.com"), new User("test2", "test2@test.com"));
    }

    @JsonInclude(type = "default", fields = {"name", "email"})
    public static class User {
        private String name;
        private String email;

        public User(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }
    }
}
