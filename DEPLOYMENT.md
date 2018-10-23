# Crude set-up instructions for PoeWatch development environment

This guide assumes you have ssh root access and plan to install everything on the same machine.
Requirements: [Maven](https://maven.apache.org), the latest JDK and minimal knowledge of MySQL.

### 1. Server set up

```sudo timedatectl set-timezone UTC```
```sudo apt install apache2 php libapache2-mod-php php-mysql zip unzip openjdk-11-jre-headless mysql-server```
`sudo mysql_secure_installation`

### 2. Create UDFs for calculating median and mode values

##### 2.1. Move files to plugin dir
```
resources/udf_median/udf_median.so
resources/udf_mode/stats_mode.so
```

(usually `/usr/lib/mysql/plugin/`. Use `SHOW VARIABLES WHERE Variable_Name LIKE "%dir"` to find out)

##### 2.2. Create functions:
```
CREATE AGGREGATE FUNCTION median RETURNS REAL SONAME 'udf_median.so';
CREATE AGGREGATE FUNCTION stats_mode RETURNS REAL SONAME 'stats_mode.so';
```

### 3. Run database configuration script 
```resources/DatabaseSetup.sql```

### 4. Compile artifact
```mvn clean install```

### 5. Run it `java -Xmx256M -jar poewatch-1.0-SNAPSHOT-jar-with-dependencies.jar`
