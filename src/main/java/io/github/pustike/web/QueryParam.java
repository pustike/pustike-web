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
 * Binds the value(s) of a HTTP query parameter to a resource method parameter,
 * resource class field, or resource class bean property.
 * Values are URL decoded unless this is disabled using the {@link Encoded}
 * annotation. A default value can be specified using the {@link DefaultValue} annotation.
 *
 * The type {@code T} of the annotated parameter, field or property must either:
 * <ol>
 * <li>Be a primitive type</li>
 * <li>Have a constructor that accepts a single {@code String} argument</li>
 * <li>Have a static method named {@code valueOf} or {@code fromString}
 * that accepts a single
 * {@code String} argument (see, for example, {@link Integer#valueOf(String)})</li>
 * <li>Have a registered source to target type converter in {@link io.github.pustike.json.TypeConverter}</li>
 * <li>Be {@code List<T>}, {@code Set<T>} or {@code SortedSet<T>}, where {@code T} satisfies 2, 3 or 4 above.
 * The resulting collection is read-only.</li>
 * </ol>
 *
 * <p>If the type is not one of the collection types listed in 5 above and the
 * query parameter is represented by multiple values then the first value (lexically)
 * of the parameter is used.</p>
 *
 * <p>Because injection occurs at object creation time, use of this annotation
 * on resource class fields and bean properties is only supported for the
 * default per-request resource class lifecycle. Resource classes using
 * other lifecycles should only use this annotation on resource method
 * parameters.</p>
 *
 * @see DefaultValue
 * @see Encoded
 */
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface QueryParam {

    /**
     * Defines the name of the HTTP query parameter whose value will be used
     * to initialize the value of the annotated method argument, class field or
     * bean property. The name is specified in decoded form, any percent encoded
     * literals within the value will not be decoded and will instead be
     * treated as literal text. E.g. if the parameter name is "a b" then the
     * value of the annotation is "a b", <i>not</i> "a+b" or "a%20b".
     * @return name of the HTTP query parameter
     */
    String value();
}
