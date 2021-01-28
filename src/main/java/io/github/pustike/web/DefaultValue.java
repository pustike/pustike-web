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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the default value of request meta-data that is bound using one of the following annotations:
 * {@link PathParam},
 * {@link QueryParam},
 * {@link CookieParam},
 * {@link FormParam},
 * or {@link HeaderParam}.
 * The default value is used if the corresponding meta-data is not present in the request.
 * <p>
 * If the type of the annotated parameter is {@link java.util.List}, {@link java.util.Set}
 * or {@link java.util.SortedSet} then the resulting collection will have a single entry mapped
 * from the supplied default value.
 * </p>
 * <p>
 * If this annotation is not used and the corresponding meta-data is not
 * present in the request, the value will be an empty collection for
 * {@code List}, {@code Set} or {@code SortedSet}, {@code null} for
 * other object types, and the Java-defined default for primitive types.
 * </p>
 *
 * @see PathParam
 * @see QueryParam
 * @see FormParam
 * @see HeaderParam
 * @see CookieParam
 */
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DefaultValue {

    /**
     * The specified default value.
     * @return the default value
     */
    String value();
}
