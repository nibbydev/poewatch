# Crude set-up instructions for PoeWatch

Prerequisites: Maven, GCC (for compiling UDFs), the latest JDK and MySQL.

Note: PoeWatch relies on MySQL UD functions such as `MEDIAN` and `STATS_MODE`. These can be downloaded from 
[here](https://github.com/infusion/udf_infusion) and installed using the instructions provided there.

1. Compile Java app with `mvn clean install`
2. Prep SQL database by running the configuration script from `resources/DatabaseSetup.sql`
3. Run it `java -Xmx256M -jar poewatch-1.0-SNAPSHOT-jar-with-dependencies.jar`
