# PoeWatch

![Prices page](resources/images/img01.png)

## Overview

PoeWatch is a Path of Exile statistics and price data collection page that's been in the works since 2017. It gathers data over time for various items (such as uniques, gems, currency, you name it) from public trade listings and finds the average prices.

The repository contains the web frontend, PHP backend and the app written in Java. A live version can be seen on [Poe.watch](http://poe.watch).

## The general idea

The general goal was to make a statistics website with everything in one place. Users can check prices of almost any item type from the current or past leagues and look up character names.

## Setup

Prerequisites: Maven, GCC (for compiling UDFs), the latest JDK and MySQL.

Note: PoeWatch relies on MySQL UD functions such as `MEDIAN` and `STATS_MODE`. These are provided by
[infusion](https://github.com/infusion/udf_infusion) and can be installed using the instructions provided there.

1. Compile Java app with `mvn clean install`
2. Prep SQL database by running the configuration script from `resources/DatabaseSetup.sql`
3. Run app `java -Xmx256M -jar poewatch-1.0-SNAPSHOT-jar-with-dependencies.jar`
