[![Build Status](https://travis-ci.org/Esri/geometry-api-java.png?branch=master)](https://travis-ci.org/Esri/geometry-api-java)

# geometry-api-java

The Esri Geometry API for Java can be used to enable spatial data processing in 3rd-party data-processing solutions.  Developers of custom MapReduce-based applications for Hadoop can use this API for spatial processing of data in the Hadoop system.  The API is also used by the [Hive UDF’s](https://github.com/Esri/spatial-framework-for-hadoop) and could be used by developers building geometry functions for 3rd-party applications such as [Cassandra]( https://cassandra.apache.org/), [HBase](http://hbase.apache.org/), [Storm](http://storm-project.net/) and many other Java-based “big data” applications.

## Features
* API methods to create simple geometries directly with the API, or by importing from supported formats: JSON, WKT, and Shape
* API methods for spatial operations: union, difference, intersect, clip, cut, and buffer
* API methods for topological relationship tests: equals, within, contains, crosses, and touches

## Instructions

Building the source:

1. Download and unzip the .zip file, or clone the repository.
1. To build the jar, run the `mvn compile` command-line command from within the cloned directory.
1. Deploy the esri-geometry-api.jar to the target system, add a reference to it in a Java project.
1. To run the unit-tests, run the `mvn test` command-line command from within the cloned directory.

The project is also available as a [Maven](http://maven.apache.org/) dependency:

```xml
<dependency>
  <groupId>com.esri.geometry</groupId>
  <artifactId>esri-geometry-api</artifactId>
  <version>2.2.0</version>
</dependency>
```

## Requirements

* Java JDK 1.6 or greater.
* [Apache Maven](https://maven.apache.org/) build system.
* Experience developing MapReduce applications for [Apache Hadoop](http://hadoop.apache.org/).
* Familiarity with text-based spatial data formats such as JSON or WKT would be useful. 

## Documentation
* [geometry-api-java/Wiki](https://github.com/Esri/geometry-api-java/wiki/)
* [geometry-api-java/Javadoc](http://esri.github.com/geometry-api-java/javadoc/)

## Resources

* [ArcGIS Geodata Resource Center]( http://resources.arcgis.com/en/communities/geodata/)
* [ArcGIS Blog](http://blogs.esri.com/esri/arcgis/)
* [twitter@esri](http://twitter.com/esri)

## Issues

Find a bug or want to request a new feature?  Please let us know by submitting an issue.

## Contributing

Esri welcomes contributions from anyone and everyone. Please see our [guidelines for contributing](https://github.com/esri/contributing)

## Licensing
Copyright 2013-2018 Esri

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

A copy of the license is available in the repository's [license.txt](https://raw.github.com/Esri/geometry-api-java/master/license.txt) file.
