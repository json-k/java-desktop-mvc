# Java Desktop MVC
Model, view, controller base classes for Java desktop applications written in Swing.

I am using this as a starter project for building Java desktop applications - this working methodology uses a [MVC pattern](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93controller) to separate code into logic manageable blocks.

This has been used in production and produces some very usable applications

## Build

This has a limited Gradle build right now with one task to run the application. I will add something to build a Mac (and possibly Windows) desktop application later in the process.

## Philosophy

The base classes View, Model, and Controller are all stored in a single class - allowing them to be easily added to projects, mirrors the structure they are used in, and allows to better package privacy.

They are constructed like this: **View(Controller(Model()))**