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
package io.github.pustike.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the media types that the methods of a resource class can accept. If
 * not specified, a container will assume that any media type is acceptable.
 * Method level annotations override a class level annotation. A container
 * is responsible for ensuring that the method invoked is capable of consuming
 * the media type of the HTTP request entity body. If no such method is
 * available the container must respond with a HTTP "415 Unsupported Media Type"
 * as specified by RFC 2616.
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Consumes {

    /**
     * A list of media types. Each entry may specify a single type or consist
     * of a comma separated list of types, with any leading or trailing white-spaces
     * in a single type entry being ignored. For example:
     * <pre>
     *  {"image/jpeg, image/gif ", " image/png"}
     * </pre>
     * Use of the comma-separated form allows definition of a common string constant
     * for use on multiple targets.
     * @return a list of media types
     */
    String[] value() default "*/*";
}
