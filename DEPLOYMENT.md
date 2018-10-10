# Crude set-up instructions

This guide assumes you have ssh root access and plan to install everything on the same machine.

### 1. Server set up

##### 1.1. Set timezone
```sudo timedatectl set-timezone UTC```

##### 1.2. Add user account
```
adduser pw
usermod -aG sudo pw
su pw
cd ~
mkdir .ssh
nano .ssh/authorized_keys
```

##### 1.3. Install perquisites
```sudo apt install apache2 php libapache2-mod-php php-mysql zip unzip openjdk-11-jre-headless mysql-server```

##### 1.4. Prepare sub-domains
```sudo nano /etc/apache2/sites-available/poe.watch.conf```
```
<VirtualHost *:80>
    ServerName poe.watch
    ServerAlias www.poe.watch
    DocumentRoot /home/pw/http/html

    <Directory /home/pw/http/html>
        Options Indexes FollowSymLinks
        AllowOverride All
        Require all granted
    </Directory>

    ErrorLog ${APACHE_LOG_DIR}/error.log
    CustomLog ${APACHE_LOG_DIR}/access.log combined
</VirtualHost>
```

```sudo nano /etc/apache2/sites-available/api.poe.watch.conf```
```
<VirtualHost *:80>
    ServerName api.poe.watch
    DocumentRoot /home/pw/http/api

    <Directory /home/pw/http/api>
        Options Indexes FollowSymLinks
        AllowOverride All
        Require all granted
        Header set Access-Control-Allow-Origin "*"
    </Directory>

    ErrorLog ${APACHE_LOG_DIR}/error.log
    CustomLog ${APACHE_LOG_DIR}/access.log combined
</VirtualHost>
```

```sudo nano /etc/apache2/sites-available/management.poe.watch.conf```
```
<VirtualHost *:80>
    ServerName management.poe.watch
    DocumentRoot /home/pw/http/management

    <Directory /home/pw/http/management>
        Options Indexes FollowSymLinks
        AllowOverride All
        Require all granted
    </Directory>

    ErrorLog ${APACHE_LOG_DIR}/error.log
    CustomLog ${APACHE_LOG_DIR}/access.log combined
</VirtualHost>
```

##### 1.5. Final touches to web server
```
sudo a2enmod rewrite
sudo a2enmod headers
sudo a2dissite 000-default.conf
sudo a2ensite poe.watch.conf
sudo a2ensite api.poe.watch.conf
sudo a2ensite management.poe.watch.conf
sudo systemctl reload apache2.service
```

## 2. Setup MySQL
##### 2.1. ```sudo mysql_secure_installation```

##### 2.2. Copy UDFs (`udf_median.so` and `stats_mode.so`) to plugin directory (usually `/usr/lib/mysql/plugin/`, or use `SHOW VARIABLES WHERE Variable_Name LIKE "%dir"` to find out)

##### 2.3. Enter MySQL prompt `mysql -u root -p`

##### 2.4. Enable root login via password `ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'test';`

##### 2.5. Create functions:
```
CREATE AGGREGATE FUNCTION median RETURNS REAL SONAME 'udf_median.so';
CREATE AGGREGATE FUNCTION stats_mode RETURNS REAL SONAME 'stats_mode.so';
```

##### 2.6. Run database configuration script from `resources/DatabaseSetup.sql` (Change user account passwords at the very bottom)

##### 2.7. Set environment variables `sudo nano /etc/mysql/my.cnf`
```
[client]
default-character-set = utf8mb4

[mysql]
default-character-set = utf8mb4

[mysqld]
character-set-client-handshake = FALSE
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci

event_scheduler=1
group_concat_max_len = 1000000

query_cache_type=0
long_query_time=2
slow_query_log=ON
table_open_cache=256M
max_heap_table_size=128M
tmp_table_size=128M
innodb_sort_buffer_size=32M
innodb_buffer_pool_size=512M
innodb_log_file_size=128M
```

## 3. Export and import database

##### 3.1. Export from origin server `mysqldump --opt -u root -p pw > pw_backup.sql`

(Archiving will reduce file size round 75% `zip pw_backup.zip pw_backup.sql`)

##### 3.2. Import to branch server `mysql -u root -p pw < pw_backup.sql`


## 4. Setup PhpMyAdmin

##### 4.1. Download source files from `https://www.phpmyadmin.net` and place in `/html/management/sql`

##### 4.2. Create config
```
cd ~/http/management/sql
cp config.sample.inc.php config.inc.php
```
2. Add blowfish secret to `$cfg['blowfish_secret']`

3. Change `localhost` to `127.0.0.1` for `$cfg['Servers'][$i]['host']` to allow root PMA logins

## 5. Firewall (assuming CloudFlare is set up and working)

##### 5.1. Whitelist port 22 so we don't get locked out
`sudo ufw allow 22`

##### 5.2. Create batch script for whitelisting CloudFlare IPs
1. Create CloudFlare whitelist script `cd ~ && nano ufw.sh`
```
#!/bin/bash
for i in `curl https://www.cloudflare.com/ips-v4`; do ufw allow from $i to any port 80; done
for i in `curl https://www.cloudflare.com/ips-v6`; do ufw allow from $i to any port 80; done
```

Original script by [raeesbhatti](https://gist.github.com/raeesbhatti/e336ab920ab523335937).

2. Set permissions `chmod 744 ufw.sh` and run the script `sudo ./ufw.sh`.

##### 5.3. Deny all other connections
`sudo ufw default deny`

##### 5.4. Enable firewall
`sudo ufw enable`

## 6. Artifact set up

##### 6.1. Obtain Gson `com.google.code.gson:gson:2.8.5` and MySQL connector `mysql:mysql-connector-java:8.0.12` from Maven

##### 6.2. Compile project and export artifacts

##### 6.3. Run the program `java -Xmx128M -jar poewatch.jar`
