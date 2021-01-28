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
 * The annotation that may be used to inject custom "parameter aggregator" object into a resource method parameter.
 * <p>
 * For example:
 * <pre>
 * public class MyBean {
 *   &#64;FormParam("myData")
 *   private String data;
 *
 *   &#64;HeaderParam("myHeader")
 *   private String header;
 *
 *   &#64;PathParam("id")
 *   public void setResourceId(String id) {...}
 *
 *   ...
 * }
 *
 * &#64;Path("myresources")
 * public class MyResources {
 *   &#64;POST
 *   &#64;Path("{id}")
 *   public void post(&#64;BeanParam MyBean myBean) {...}
 *
 *   ...
 * }
 * </pre>
 */
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BeanParam {
}
