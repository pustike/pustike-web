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
package io.github.pustike.web.scope;

import java.io.Closeable;
import jakarta.inject.Provider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.github.pustike.inject.BindingKey;
import io.github.pustike.inject.Scope;

/**
 * Request Scope that stores created instances as attributes in the request.
 * Example usage:
 * <pre>{@code
 * Injector injector = Injectors.create((Module) binder -> {
 *     RequestScope requestScope = new RequestScope();
 *     binder.bindScope(RequestScoped.class, requestScope);
 *     binder.bind(SomeClass.class).in(requestScope);
 * });
 * ...
 * Closeable scope = RequestScope.open(httpRequest, httpResponse);
 * ...
 * scope.close();
 * }</pre>
 * @see RequestScoped
 */
public final class RequestScope implements Scope {
    private static final ThreadLocal<Context> threadLocal = new ThreadLocal<>();
    /** A sentinel attribute value representing null. */
    private enum NullObject { INSTANCE }

    @Override
    public <T> Provider<T> scope(BindingKey<T> bindingKey, Provider<T> creator) {
        final String name = bindingKey.toString();
        return () -> {
            Context context = threadLocal.get();
            if (context == null) {
                throw new IllegalStateException("Request Context is not open in this scope, for the key:" + name);
            }
            HttpServletRequest request = context.getRequest();
            synchronized (request) {
                Object obj = request.getAttribute(name);
                if (NullObject.INSTANCE == obj) {
                    return null;
                }
                @SuppressWarnings("unchecked")
                T t = (T) obj;
                if (t == null) {
                    t = creator.get();
                    request.setAttribute(name, (t != null) ? t : NullObject.INSTANCE);
                }
                return t;
            }
        };
    }

    @Override
    public String toString() {
        return RequestScoped.class.getName();
    }

    /**
     * Sets Http Request and Response objects to the local thread context and returns a closeable handle
     * that should be closed to clear the context.
     * <p>
     * Preferably, close method should be called in a finally block to make sure that it executes,
     * and so to avoid possible memory leaks.
     * @param request the http servlet request
     * @param response the http servlet response
     * @return a closeable handle that should be closed to clear the context
     */
    public static Closeable open(HttpServletRequest request, HttpServletResponse response) {
        threadLocal.set(new Context(request, response));
        return threadLocal::remove;
    }

    /**
     * Get the HTTP servlet request from this context.
     * @return the http servlet request
     */
    public static HttpServletRequest getRequest() {
        return getContext().getRequest();
    }

    /**
     * Get the HTTP servlet response from this context.
     * @return the http servlet response
     */
    public static HttpServletResponse getResponse() {
        return getContext().getResponse();
    }

    private static Context getContext() {
        Context context = threadLocal.get();
        if (context == null) {
            throw new IllegalStateException("Cannot access request context!");
        }
        return context;
    }

    private static class Context {
        private final HttpServletRequest request;
        private final HttpServletResponse response;

        private Context(HttpServletRequest request, HttpServletResponse response) {
            this.request = request;
            this.response = response;
        }

        HttpServletRequest getRequest() {
            return request;
        }

        HttpServletResponse getResponse() {
            return response;
        }
    }
}
