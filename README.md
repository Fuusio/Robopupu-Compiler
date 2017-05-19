# Robopupu Compiler

<img src="https://github.com/Fuusio/Robopupu/blob/gh-pages/images/robopupu_header_image.png" alt="Robopupu mascot"/>

An Android library that provides a set of annotation processors used for generating code for the following **Robopupu APIs**:

* **Robopupu.MVP**: A Model-View-Presenter (MVP) API.
* **Robopupu.Dependency**: A dependency injection/pulling API.
* **Robopupu.FSM**: A simple library for implementing hierachical Finite State Machines that support most of the UML state diagram features, including: entry points, choice points, and history points.
* **Robopupu.Plugin**: A plugin framework that supports dependency injection (DI) and allows decouples communication between components without requiring the components to explicitly register with one another.
* **Robopupu.Feature**: A flow controller type of architectural design pattern for using components that encapsulate navigation and configuration logic for application features.

Robopupu APIs are used for architecting and developing Android applications. To minimize writing of boiler plate code, Robopupu utilises declarative annotations and annotation processors that generates code for using the libraries.

Check out the [Robopupu project website](http://robopupu.com/) and [Robopupu Github repository](https://github.com/Fuusio/Robopupu) for further information.

## Documentation
Robopupu documentation can be found in [Robopupu project website](http://robopupu.com/). 

## Installation
Please follow the installation instructions available in [Robopupu Github repository](https://github.com/Fuusio/Robopupu).

### 0.5.7
* Updated the version to match Robopupu Android library version 0.5.7

### 0.5.6
* Updated the version to match Robopupu Android library version 0.5.6

### 0.5.4
* Fixed a bug in code generation for PlugInvoker classes caused by static intercace methods supported in Java 8.

### 0.5.3
* Added support for using type parameters in interfaces annotated with @PluginInterface.

### 0.5.2
* Added support for generic methods with type parametersin Robopupu.Plugin API.
 
### 0.5.1
* Updated code generators to use the refactored field names and API changes in Robopupu API.

### 0.5.0
* Updated the version to match Robopupu Android library version 0.5.0

### 0.4.10
* Updated the version to match Robopupu Android library version 0.4.10

### 0.4.9
* Updated the version to match Robopupu Android library version 0.4.9
 
### 0.4.8
* Updated the code generation for Dependency API to be compatible with the changes in Robopupu Android library version 0.4.8
 
### 0.4.7
* Updated the compiler for Robopupu Android library version 0.4.7
 
### 0.4.6
* Updated the compiler to support the extended Dependency API of Robopupu version 0.4.6
 
### 0.4.5
* Updated the compiler to support the extended Dependency API of Robopupu version 0.4.5

### 0.4.4
* Updated the compiler to support Robopupu APIs version 0.4.4
* Fixed a bug in annotation processor for Dependency API when a DependencyScope does not have any declared @Provides annotated methods, classes, nor constructors.

### 0.3.1
* Initial open source release

## License
```
Copyright (C) 2015-2016 Marko Salmela

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at;

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
