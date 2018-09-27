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
```sudo apt install apache2 php libapache2-mod-php php7.0-mysql openjdk-11-jre-headless mysql-server```

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
```sudo mysql_secure_installation```
Copy UDFs (`udf_median.so` and `stats_mode.so`) to plugin directory (usually `/usr/lib/mysql/plugin/`, or use `SHOW VARIABLES WHERE Variable_Name LIKE "%dir"` to find out)

Enter MySQL prompt `mysql -u root -p`

Create functions:
```
CREATE AGGREGATE FUNCTION median RETURNS REAL SONAME 'udf_median.so';
CREATE AGGREGATE FUNCTION stats_mode RETURNS REAL SONAME 'stats_mode.so';
```

Run database configuration script from `/src/resources/DatabbaseSetup.sql` (Change user account passwords at the very bottom)

##### 2.1. Setup PhpMyAdmin
Download source files and place under `/html/management/sql`
Make copy of config `cp /html/management/sql/config.sample.inc.php /html/management/sql/config.inc.php`
Add the 5 lines to the bottom of file
```
$i++;
$cfg['Servers'][$i]['host']       = ''; // IP and port
$cfg['Servers'][$i]['user']       = '';
$cfg['Servers'][$i]['password']   = '';
$cfg['Servers'][$i]['auth_type']  = 'config';
```

## 3. Firewall (assuming CloudFlare is set up and working)

##### 3.1. Whitelist port 22 so we don't get locked out
`ufw allow 22`

##### 3.2. Create batch script for whitelisting CloudFlare IPs
`nano ufw.sh`

```
#!/bin/bash
for i in `curl https://www.cloudflare.com/ips-v4`; do ufw allow from $i to any port 80; done
for i in `curl https://www.cloudflare.com/ips-v6`; do ufw allow from $i to any port 80; done
```

Original script by [raeesbhatti](https://gist.github.com/raeesbhatti/e336ab920ab523335937).

Set permissions `chmod 744 ufw.sh` and run the script `./ufw.sh`.

##### 3.3. Deny all other connections
`ufw default deny`

## 4. Artifact set up

##### 4.1. Obtain Gson `com.google.code.gson:gson:2.8.5` and MySQL connector `mysql:mysql-connector-java:8.0.12` from Maven

##### 4.2. Compile project and export artifacts

##### 4.3. Run the program `java -Xmx128M -jar poewatch.jar`
