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
 * Binds the value of a URI template parameter or a path segment containing the template parameter
 * to a resource method parameter, resource class field, or resource class bean property.
 * The value is URL decoded unless this is disabled using the {@link Encoded &#64;Encoded} annotation.
 * A default value can be specified using the {@link DefaultValue &#64;DefaultValue} annotation.
 *
 * The type of the annotated parameter, field or property must either:
 * <ul>
 * <li>Be a primitive type.</li>
 * <li>Have a constructor that accepts a single String argument.</li>
 * <li>Have a static method named {@code valueOf} or {@code fromString} that accepts a single
 * String argument (see, for example, {@link Integer#valueOf(String)}).</li>
 * <li>Have a registered source to target type converter in {@link io.github.pustike.json.TypeConverter}</li>
 * </ul>
 *
 * <p>The injected value corresponds to the latest use (in terms of scope) of
 * the path parameter. E.g. if a class and a sub-resource method are both
 * annotated with a {@link Path &#64;Path} containing the same URI template
 * parameter, use of {@code @PathParam} on a sub-resource method parameter
 * will bind the value matching URI template parameter in the method's
 * {@code @Path} annotation.</p>
 *
 * <p>Because injection occurs at object creation time, use of this annotation
 * on resource class fields and bean properties is only supported for the
 * default per-request resource class lifecycle. Resource classes using
 * other lifecycles should only use this annotation on resource method
 * parameters.</p>
 *
 * @see Encoded
 * @see DefaultValue
 */
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PathParam {

    /**
     * Defines the name of the URI template parameter whose value will be used
     * to initialize the value of the annotated method parameter, class field or
     * property. See {@link Path#value()} for a description of the syntax of
     * template parameters.
     *
     * <p>E.g. a class annotated with: {@code @Path("widgets/{id}")}
     * can have methods annotated whose arguments are annotated
     * with {@code @PathParam("id")}.
     * @return name of the path parameter
     */
    String value();
}
