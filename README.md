Pustike Web   [![][Maven Central img]][Maven Central] [![][Javadocs img]][Javadocs] [![][license img]][license]
===========
Pustike Web provides API for creating application using RESTful Web Services with a central servlet `DispatcherServlet`, having an API similar to [JAX-RS](https://github.com/jakartaee/rest). Applications developed using this library can be deployed in any Servlet container like other web frameworks.

Following are some of its key features:
* RESTful web services using a central servlet `DispatcherServlet` with an API similar to [JAX-RS](https://github.com/jakartaee/rest).
* Provides resource class or method level annotation `@Path` to specify the relative path.
* Specify the HTTP request method of a resource using: `@GET`, `@PUT`, `@POST`, `@DELETE`, `@HEAD`.
* Specify the accepted request media type using `@Consumes`.
* Specify the response media type with `@Produces` (used for content negotiation).
* Annotation based method parameter to pull information out of the servlet request: `@PathParam`, `@QueryParam`, `@BeanParam`, `@CookieParam`, `@FormParam`, `@HeaderParam`, `JsonParam`. A default value can be specified using `@DefaultValue` which is used when the key is not found.
* Integration with [Pustike Inject](https://github.com/pustike/pustike-inject) during servlet context initialization.
* Scopes: `RequestScope` that stores created instances as attributes in the request.
* Static resource servlet to serve files or user defined static content.
* Support for JSON - object mapping with option to output selected fields only, based on user defined context.
* Requires Java 17 and it has dependencies to servlet-api and pustike inject, json libraries.

**Documentation:** Latest javadocs is available [here][Javadocs].

Download
--------
To add a dependency using Maven, use the following:
```xml
<dependency>
    <groupId>io.github.pustike</groupId>
    <artifactId>pustike-web</artifactId>
    <version>0.9.0</version>
</dependency>
```
Or, download the latest JAR(~60kB) from [Maven Central][latest-jar].

License
-------
This library is published under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)

[Maven Central]:https://maven-badges.herokuapp.com/maven-central/io.github.pustike/pustike-web
[Maven Central img]:https://maven-badges.herokuapp.com/maven-central/io.github.pustike/pustike-web/badge.svg
[latest-jar]:https://search.maven.org/remote_content?g=io.github.pustike&a=pustike-web&v=LATEST

[Javadocs]:https://javadoc.io/doc/io.github.pustike/pustike-web
[Javadocs img]:https://javadoc.io/badge/io.github.pustike/pustike-web.svg

[license]:LICENSE
[license img]:https://img.shields.io/badge/license-Apache%202-blue.svg
