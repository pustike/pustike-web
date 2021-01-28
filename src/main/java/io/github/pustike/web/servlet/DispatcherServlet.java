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

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.System.Logger.Level;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import jakarta.inject.Singleton;
import jakarta.json.Json;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.github.pustike.inject.Injector;
import io.github.pustike.inject.Injectors;
import io.github.pustike.inject.bind.Module;
import io.github.pustike.json.ObjectMapper;
import io.github.pustike.json.TypeConverter;
import io.github.pustike.web.JsonContext;
import io.github.pustike.web.Path;
import io.github.pustike.web.scope.RequestScope;
import io.github.pustike.web.scope.RequestScoped;
import io.github.pustike.web.utils.AntPathMatcher;
import io.github.pustike.web.utils.ServletUtils;

/**
 * The Dispatcher servlet.
 */
public final class DispatcherServlet extends HttpServlet {
    private static final System.Logger logger = System.getLogger(DispatcherServlet.class.getName());
    private static final String INJECTOR_NAME = Injector.class.getSimpleName();
    /** Map to cache the path pattern handler methods */
    private final Map<String, HandlerMethod> patternHandlerMethodMap;
    /** Map to cache the path - pattern */
    private final Map<String, String> pathPatternUriCache;
    /** The static resource handler */
    private final StaticResourceHandler staticResourceHandler;
    /** The path prefix */
    private String pathPrefix;
    /** The injector */
    private Injector injector;
    /** The Ant patch matcher */
    private AntPathMatcher pathMatcher;
    /** The Json Object Mapper */
    private ObjectMapper objectMapper;
    /** The parameter resolver */
    private ParameterResolver parameterResolver;

    /**
     * Default Constructor.
     */
    public DispatcherServlet() {
        this.patternHandlerMethodMap = new ConcurrentHashMap<>();
        this.pathPatternUriCache = new ConcurrentHashMap<>();
        this.staticResourceHandler = new StaticResourceHandler();
    }

    @Override
    public final void init() {
        WebModuleConfigurer configurer = getWebModuleConfigurer();
        Path pathAnnotation = configurer.getClass().getDeclaredAnnotation(Path.class);
        this.pathPrefix = pathAnnotation != null ? pathAnnotation.value().trim() : "";
        List<Module> moduleList = new ArrayList<>();
        moduleList.add(createServletModule());
        moduleList.addAll(configurer.getModules());
        Injector parentInjector = (Injector) getServletContext().getAttribute(WebServletContextListener.INJECTOR_NAME);
        this.injector = parentInjector == null ? Injectors.create(moduleList)
                : parentInjector.createChildInjector(moduleList);
        getServletContext().setAttribute(INJECTOR_NAME, injector);
        //
        this.pathMatcher = new AntPathMatcher();
        this.objectMapper = injector.getInstance(ObjectMapper.class);
        TypeConverter typeConverter = injector.getInstance(TypeConverter.class);
        this.parameterResolver = new ParameterResolver(typeConverter, pathMatcher, objectMapper);
    }

    private WebModuleConfigurer getWebModuleConfigurer() {
        Object attributeValue = getServletContext().getAttribute(WebModuleConfigurer.class.getSimpleName());
        if (attributeValue instanceof WebModuleConfigurer) {
            return (WebModuleConfigurer) attributeValue;
        }
        String configurerClassName = getInitParameter("configurer");
        if (configurerClassName != null && !configurerClassName.isBlank()) {
            try {
                Class<?> configurerClass = Class.forName(configurerClassName);
                return (WebModuleConfigurer) configurerClass.getConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException("invalid servlet init parameter 'configurer':" + configurerClassName, e);
            }
        }
        throw new IllegalStateException("Web Module Configurer service is not defined!");
    }

    private Module createServletModule() {
        return binder -> {
            binder.setDefaultScope(Singleton.class);
            // bind custom scope -> RequestScope
            binder.bindScope(RequestScoped.class, new RequestScope());

            TypeConverter typeConverter = new TypeConverter();
            binder.bind(TypeConverter.class).toInstance(typeConverter);
            binder.bind(ObjectMapper.class).toInstance(new ObjectMapper(typeConverter));

            Predicate<Class<?>> predicate = targetType -> targetType.getDeclaredAnnotation(Path.class) != null;
            binder.addBindingListener(predicate, (bindingKey, controllerClass) -> registerController(controllerClass));
        };
    }

    private void registerController(Class<?> controllerClass) {
        Path resourceAnn = controllerClass.getDeclaredAnnotation(Path.class);
        String resourcePath = resourceAnn != null ? resourceAnn.value().trim() : null;
        if (resourcePath == null || resourcePath.length() == 0) {
            return;
        }
        Collection<Integer> visitedMethodHashCodes = new HashSet<>();
        for (Class<?> c = controllerClass; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method method : c.getDeclaredMethods()) {
                int hashCode = computeHashCode(c, method);
                if (visitedMethodHashCodes.contains(hashCode)) {
                    continue;
                }
                visitedMethodHashCodes.add(hashCode);
                Path pathAnnotation = method.getDeclaredAnnotation(Path.class);
                if (pathAnnotation == null) {
                    continue;
                }
                // Note: user should ensure that '/' is used in paths properly
                String pathPattern = pathPrefix + resourcePath + pathAnnotation.value().trim();
                HandlerMethod existingInfo = patternHandlerMethodMap.putIfAbsent(//
                        pathPattern, new HandlerMethod(controllerClass, method, pathPattern));
                if (existingInfo != null) {
                    throw new IllegalStateException("A request mapping is already registered: " + existingInfo);
                }
            }
        }
    }

    private static int computeHashCode(Class<?> clazz, Method method) {
        int hashCode = 31 + method.getName().hashCode();
        for (Class<?> parameterType : method.getParameterTypes()) {
            hashCode = 31 * hashCode + parameterType.hashCode();
        }
        int modifiers = method.getModifiers();
        if (!Modifier.isPublic(modifiers) && !Modifier.isProtected(modifiers)
                && !Modifier.isPrivate(modifiers)) { // package-private
            hashCode = 31 * hashCode + clazz.getPackage().hashCode();
        } else if (Modifier.isPrivate(modifiers)) {
            hashCode = 31 * hashCode + clazz.hashCode(); // private method
        }
        return hashCode;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String method = req.getMethod();
        if (method.equals("HEAD") || method.equals("OPTIONS") || method.equals("TRACE")) {
            super.service(req, resp);
        } else {
            processRequest(req, resp);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String relativePath = ServletUtils.getRelativePath(request);
        if (!relativePath.startsWith(pathPrefix)) {// check if static resource can be served
            staticResourceHandler.service(request, response);
            return;
        }
        // TODO read Consumes here and match accordingly
        String pathPattern = pathPatternUriCache.computeIfAbsent(request.getMethod() + '@' + relativePath,
                s -> findMatchingPathPattern(request.getMethod(), relativePath));
        if (pathPattern == null) { // No matching controller method found for the request
            // check if static resource can be served
            staticResourceHandler.service(request, response);
            return;
        }
        HandlerMethod handlerMethod = patternHandlerMethodMap.get(pathPattern);
        if (handlerMethod == null) {
            throw new IllegalStateException("failed to match any request handler for URI: " + request.getRequestURI());
        }
        Object controller = injector.getInstance(handlerMethod.getControllerClass());
        Method controllerMethod = handlerMethod.getMethod();
        if (!controllerMethod.trySetAccessible()) {
            throw new InaccessibleObjectException("couldn't enable access to method: " + controllerMethod);
        }
        try (Closeable ignored = RequestScope.open(request, response)) {
            Object[] parameterValues = parameterResolver.resolveParameters(request, handlerMethod);
            Object returnValue = controllerMethod.invoke(controller, parameterValues);
            handleReturnValue(response, handlerMethod, returnValue);
        } catch (Exception e) {
            Throwable error = e.getCause() != null ? e.getCause() : e;
            logger.log(Level.WARNING, "error when handing the request", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType(ServletUtils.JSON_MEDIA_TYPE.toString());
            try(PrintWriter writer = response.getWriter()) {
                Json.createWriter(writer).write(Json.createValue(error.getMessage()));
            }
        }
    }

    private void handleReturnValue(HttpServletResponse response, HandlerMethod handlerMethod, Object returnValue)
            throws IOException {
        // if @Produces is present, set content-type from that value
        Charset charset = ServletUtils.getCharset(ServletUtils.JSON_MEDIA_TYPE);
        response.setContentType(ServletUtils.JSON_MEDIA_TYPE.toString());
        response.setCharacterEncoding(charset.toString());
        try (OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream(), charset)) {
            JsonContext jsonContext = handlerMethod.getMethod().getAnnotation(JsonContext.class);
            String context = jsonContext != null ? jsonContext.value() : null;
            Json.createWriter(writer).write(objectMapper.toJsonValue(returnValue, context));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not write JSON: " + ex.getMessage(), ex);
        }
        response.getOutputStream().flush();
    }

    private String findMatchingPathPattern(String requestMethod, String requestUri) {
        int paramIndex = requestUri.indexOf('?');
        if (paramIndex != -1) {
            requestUri = requestUri.substring(0, paramIndex);
        }
        int longestPatternLength = 0;
        String matchingPathPattern = null;
        for (Map.Entry<String, HandlerMethod> mapEntry : patternHandlerMethodMap.entrySet()) {
            String pathPattern = mapEntry.getKey();
            if (pathMatcher.match(pathPattern, requestUri)) {
                HandlerMethod handlerMethod = mapEntry.getValue();
                if (handlerMethod.supportsMethod(requestMethod)) {
                    int patternLength = pathPattern.length();
                    if (patternLength > longestPatternLength) {
                        longestPatternLength = patternLength;
                        matchingPathPattern = pathPattern;
                    }
                }
            }
        }
        return matchingPathPattern;
    }

    @Override
    public void destroy() {
        pathPatternUriCache.clear();
        patternHandlerMethodMap.clear();
        injector.getInstance(TypeConverter.class).invalidate();
        Injectors.dispose(injector);
        getServletContext().removeAttribute(INJECTOR_NAME);
    }
}
