# geometry-api-java

This is the Esri Geometry API that can be used to enable spatial data processing in 3rd-party data-processing solutions.  Developers of custom MapReduce-based applications for Hadoop can use this API for spatial processing of data in the Hadoop system.  The API is also used by the [Hive UDF’s](https://github.com/esri/hive-spatial) and could be used by developers building geometry functions for 3rd-party applications such as [Cassandra]( https://cassandra.apache.org/), [HBase](http://hbase.apache.org/), [Storm](http://storm-project.net/) and many other Java-based “big data” applications.

## Features
* API methods to create simple geometries directly with API, or by importing from supported formats: JSON, WKT, Shape
* API methods for spatial operations: union, difference, intersect, clip, cut, buffer
* API methods for topological relationship tests: equals, within, contains, crosses, touches

## Instructions

1. Download and unzip the .zip file or clone the repository.
2. Deploy esri-geometry-api.jar to the target system, add a reference to it in a Java project.
3. To build the jar, Javadoc, and run the unit-tests, run the “ant” command-line command from within the cloned directory. The ant tool runs the “build.xml” script which creates the jar, runs the unit tests, then creates the Javadoc documentation files.

## Requirements

* Java JDK 1.6 or greater
* Experience developing MapReduce applications for [Hadoop](http://hadoop.apache.org/).
* Familiarity with text formats of spatial data, such as JSON or WKT, would be useful. 

## Resources

* [ArcGIS Geodata Resource Center]( http://resources.arcgis.com/en/communities/geodata/)
* [ArcGIS Blog](http://blogs.esri.com/esri/arcgis/)
* [twitter@esri](http://twitter.com/esri)

## Issues

Find a bug or want to request a new feature?  Please let us know by submitting an issue.

## Contributing

Anyone and everyone is welcome to contribute. 

## Licensing
Copyright 2012 Esri

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

A copy of the license is available in the repository's [license.txt]( https://raw.github.com/Esri/geometry-api-java/master/license.txt) file.
