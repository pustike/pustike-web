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
package io.github.pustike.web.servlet;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import io.github.pustike.web.HttpMethod;

/**
 * Info about Request Mapping defined at Controller methods.
 */
class HandlerMethod {
    private final Method method;
    private final Class<?> controllerClass;
    private final String pathPattern;
    private Set<String> httpMethods;

    public HandlerMethod(Class<?> controllerClass, Method method, String pathPattern) {
        this.method = method;
        this.controllerClass = controllerClass;
        this.pathPattern = pathPattern;
    }

    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public Method getMethod() {
        return method;
    }

    public boolean supportsMethod(String methodName) {
        if (getHttpMethods().isEmpty()) {
            return true;// as no specific method is defined, allow all!
        }
        for (String requestMethod : getHttpMethods()) {
            if(requestMethod.equals(methodName)){
                return true;
            }
        }
        return false;
    }

    public Set<String> getHttpMethods() {
        if (httpMethods == null) {
            httpMethods = new HashSet<>();
            Annotation[] annotations = method.getAnnotations();
            for (Annotation annotation : annotations) {
                HttpMethod httpMethod = annotation.getClass().getAnnotation(HttpMethod.class);
                if (httpMethod != null) {
                    httpMethods.add(httpMethod.value());
                }
            }
        }
        return httpMethods;
    }

    public String getPathPattern() {
        return pathPattern;
    }

    @Override
    public String toString() {
        return "HandlerMethod(path: " + pathPattern + "; method: " + method.toGenericString() + ")";
    }
}
