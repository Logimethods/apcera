# NATS / Camel Component

[Apache Camel](http://camel.apache.org) is a powerful open source integration framework based on known
Enterprise Integration Patterns with powerful Bean Integration.
[NATS messaging system](https://nats.io) (a highly performant cloud native messaging system).

[![MIT License](https://img.shields.io/npm/l/express.svg)](http://opensource.org/licenses/MIT)

## Release Notes
### Version 2.18-SNAPSHOT

- That connector uses [JNATS](https://github.com/nats-io/jnats) version 0.4.1, which requires a JVM 1.8.


## Installation

### Maven Central

#### Releases

The first version (0.1.0) of the NATS Spark connectors has been released, but without being already fully tested in large applications.

If you are embedding the NATS Spark connectors, add the following dependency to your project's `pom.xml`.

```xml
  <dependencies>
    ...
    <dependency>
      <groupId>com.logimethods</groupId>
      <artifactId>camel-nats</artifactId>
      <version>2.18-SNAPSHOT</version>
    </dependency>
  </dependencies>
```

## License

(The MIT License)

Copyright (c) 2016 Logimethods.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to
deal in the Software without restriction, including without limitation the
rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
IN THE SOFTWARE.
