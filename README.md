PatternDB Editor
================

Installation
------------

Install sbt:

    wget http://scalasbt.artifactoryonline.com/scalasbt/sbt-native-packages/org/scala-sbt/sbt//0.12.3/sbt.deb
    dpkg -i sbt.deb

cd into this directory (where this project.txt resides).
run sbt:

    sbt

type play:

    play

type run:

    run

Directory hierarchy
-------------------

 * app/controllers: The main backend code
 * app/assets: Coffescript code, frontend logic, LESS CSS files
 * app/views: Frontend templates, mostly JADE files
 * app/lib: misc backend library files
 * conf/application.conf: application and dependent library specific configuration
 * conf/routes: route file, specifies which URL handled by which function in controllers
 * project/: project build definition
 * public/: Javascript libs and files, static file serving directory
 * test/: test specific js libraries
 * test/specs/: testcases for frontend in Coffeescript

More information at: http://www.playframework.com/documentation/2.1.1/Anatomy

Features
--------

It can handle only very simple pattern databases.
