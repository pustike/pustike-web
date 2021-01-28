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

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Map;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import io.github.pustike.json.ObjectMapper;
import io.github.pustike.json.TypeConverter;
import io.github.pustike.web.BeanParam;
import io.github.pustike.web.CookieParam;
import io.github.pustike.web.DefaultValue;
import io.github.pustike.web.FormParam;
import io.github.pustike.web.HeaderParam;
import io.github.pustike.web.PathParam;
import io.github.pustike.web.QueryParam;
import io.github.pustike.web.utils.AntPathMatcher;
import io.github.pustike.web.utils.MediaType;
import io.github.pustike.web.utils.ServletUtils;

class ParameterResolver {
    private final TypeConverter typeConverter;
    private final AntPathMatcher pathMatcher;
    private final ObjectMapper objectMapper;
    private final MediaType starPlusJsonMediaType;

    ParameterResolver(TypeConverter typeConverter, AntPathMatcher pathMatcher, ObjectMapper objectMapper) {
        this.typeConverter = typeConverter;
        this.pathMatcher = pathMatcher;
        this.objectMapper = objectMapper;
        this.starPlusJsonMediaType = MediaType.get("application/*+json");
    }

    Object[] resolveParameters(HttpServletRequest request, HandlerMethod handlerMethod) throws Exception {
        final String pathPattern = handlerMethod.getPathPattern();
        Parameter[] parameters = handlerMethod.getMethod().getParameters();
        Class<?>[] parameterTypes = handlerMethod.getMethod().getParameterTypes();
        Type[] genericParameterTypes = handlerMethod.getMethod().getGenericParameterTypes();
        Object[] parameterValues = new Object[parameters.length];
        Map<String, String> pathVariableMap = null;
        for (int i = 0; i < parameterValues.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> parameterType = parameterTypes[i];
            Object paramValue;
            if(parameter.isAnnotationPresent(QueryParam.class)) {
                paramValue = getQueryParam(request, parameter.getAnnotation(QueryParam.class).value());
            } else if(parameter.isAnnotationPresent(PathParam.class)) {
                if (pathVariableMap == null) {
                    String relativePath = ServletUtils.getRelativePath(request);
                    pathVariableMap = pathMatcher.extractUriTemplateVariables(pathPattern, relativePath);
                }
                paramValue = getPathParam(pathVariableMap, parameter.getAnnotation(PathParam.class).value());
            } else if(parameter.isAnnotationPresent(HeaderParam.class)) {
                paramValue = getHeaderParam(request, parameter.getAnnotation(HeaderParam.class).value());
            } else if(parameter.isAnnotationPresent(CookieParam.class)) {
                paramValue = getCookieParam(request, parameter.getAnnotation(CookieParam.class).value());
            } else if(parameter.isAnnotationPresent(FormParam.class)) {
                paramValue = getFormParam(request, parameter.getAnnotation(FormParam.class).value());
            } else if(parameter.isAnnotationPresent(BeanParam.class)) {
                paramValue = getBeanParam(request, parameterType, pathPattern);
            } else { // if json content-type, then read json parameter
                MediaType contentType = ServletUtils.getContentType(request);
                if (contentType == null || ServletUtils.JSON_MEDIA_TYPE.includes(contentType)
                        || starPlusJsonMediaType.includes(contentType)) {
                    try (Reader jsonReader = new InputStreamReader(ServletUtils.getBody(request),
                            ServletUtils.getCharset(contentType))) {
                        paramValue = objectMapper.readValue(jsonReader, genericParameterTypes[i]);
                    } catch (Exception ex) {
                        throw new IllegalArgumentException("Could not read JSON: " + ex.getMessage(), ex);
                    }
                } else {
                    throw new IllegalStateException("No supporting parameter Resolver found for URI: " //
                            + request.getRequestURI());
                }
            }
            if (paramValue == null) {
                DefaultValue defaultValue = parameter.getAnnotation(DefaultValue.class);
                paramValue = defaultValue != null ? defaultValue.value() : null;
            }
            // REVIEW: what if more than one value is present here, should a list be supported!
            parameterValues[i] = paramValue == null || parameterType.isInstance(paramValue) ? paramValue
                    : typeConverter.convert(paramValue, parameterType);
        }
        return parameterValues;
    }

    private String getQueryParam(HttpServletRequest request, String parameterKey) {
        return parameterKey.isBlank() ? null : request.getParameter(parameterKey);
    }

    private String getPathParam(Map<String, String> pathVariableMap, String parameterKey) {
        return parameterKey.isBlank() ? null : pathVariableMap.get(parameterKey);
    }

    private String getHeaderParam(HttpServletRequest request, String parameterKey) {
        return parameterKey.isBlank() ? null : request.getHeader(parameterKey);
    }

    private String getCookieParam(HttpServletRequest request, String parameterKey) {
        Cookie[] cookies = request.getCookies();
        if (parameterKey.isBlank() || cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(parameterKey)) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String getFormParam(HttpServletRequest request, String parameterKey) {
        if (parameterKey.isBlank()) {
            return null;
        }
        String[] formParamValues = request.getParameterMap().get(parameterKey);
        return formParamValues != null && formParamValues.length > 0 ? formParamValues[0] : null;
    }

    private Object getBeanParam(HttpServletRequest request, Class<?> parameterType, String pathPattern) throws Exception {
        // TODO support constructor with parameters
        Object instance = parameterType.getConstructor().newInstance();
        Field[] fields = parameterType.getDeclaredFields();
        Map<String, String> pathVariableMap = null;
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())
                    || field.getName().startsWith("this$")) {
                continue;
            }
            String fieldValue = null;
            if (field.isAnnotationPresent(QueryParam.class)) {
                fieldValue = getQueryParam(request, field.getAnnotation(QueryParam.class).value());
            } else if (field.isAnnotationPresent(PathParam.class)) {
                if (pathVariableMap == null) {
                    String relativePath = ServletUtils.getRelativePath(request);
                    pathVariableMap = pathMatcher.extractUriTemplateVariables(pathPattern, relativePath);
                }
                fieldValue = getPathParam(pathVariableMap, field.getAnnotation(PathParam.class).value());
            } else if (field.isAnnotationPresent(HeaderParam.class)) {
                fieldValue = getHeaderParam(request, field.getAnnotation(HeaderParam.class).value());
            } else if (field.isAnnotationPresent(CookieParam.class)) {
                fieldValue = getCookieParam(request, field.getAnnotation(CookieParam.class).value());
            } else if (field.isAnnotationPresent(FormParam.class)) {
                fieldValue = getFormParam(request, field.getAnnotation(FormParam.class).value());
            }
            if (fieldValue == null) {
                DefaultValue defaultValue = field.getAnnotation(DefaultValue.class);
                fieldValue = defaultValue != null ? defaultValue.value() : null;
            }
            if (fieldValue != null) {
                if (!field.trySetAccessible()) {
                    throw new InaccessibleObjectException("couldn't enable access to field: " + field);
                }
                field.set(instance, typeConverter.convert(fieldValue, field.getType()));
            }
        }
        return instance;
    }
}
