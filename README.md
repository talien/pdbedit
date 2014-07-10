PatternDB Editor
================

Web-based syslog-ng patterndb file editor. You can upload patterndb files, edit them, and the download them.

Installation
------------

Install sbt:

    wget http://scalasbt.artifactoryonline.com/scalasbt/sbt-native-packages/org/scala-sbt/sbt//0.12.3/sbt.deb
    dpkg -i sbt.deb

cd into this directory (where this README.md resides).
run sbt:

    sbt

type play:

    play

type run:

    run

It will take a while, until sbt downloads everything.

Features
--------

It can handle only very simple pattern databases. Actions/correlation features are not implemented yet.


Notes
-----

Only Openjdk 1.6+ or Oracle jdk 1.6+ supported, no gcj or ecj!.
