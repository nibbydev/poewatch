# Crude set-up instructions

### 1. Server set up

##### 1.1. Install perquisites
```sudo apt install apache2 php libapache2-mod-php openjdk-11-jre-headless mysql-server```

##### 1.2. Prepare sub-domains
```sudo nano /etc/apache2/sites-available/poe.watch.conf```
```
<VirtualHost *:80>
    ServerName poe.watch
    ServerAlias www.poe.watch
    DocumentRoot /home/ps/http/html

    <Directory /home/ps/http/html>
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
    DocumentRoot /home/ps/http/api

    <Directory /home/ps/http/api>
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
    DocumentRoot /home/ps/http/management

    <Directory /home/ps/http/management>
        Options Indexes FollowSymLinks
        AllowOverride All
        Require all granted
    </Directory>

    ErrorLog ${APACHE_LOG_DIR}/error.log
    CustomLog ${APACHE_LOG_DIR}/access.log combined
</VirtualHost>
```

##### 1.3. Final touches to web server
```
sudo a2enmod rewrite
sudo a2enmod headers
sudo a2dissite 000-default.conf
sudo a2ensite poe.watch.conf
sudo a2ensite api.poe.watch.conf
sudo a2ensite management.poe.watch.conf
sudo systemctl reload apache2.service
```

##### 1.4. Setup MySQL
```sudo mysql_secure_installation```

##### 1.5. Run database configuration script from `/src/resources/DatabbaseSetup.sql`

## 2. Firewall (assuming CloudFlare is set up and working)

##### 2.1. Whitelist port 22 so we don't get locked out
`ufw allow 22`

##### 2.2. Create batch script for whitelisting CloudFlare IPs
`nano ufw.sh`

```
#!/bin/bash
for i in `curl https://www.cloudflare.com/ips-v4`; do ufw allow from $i to any port 80; done
for i in `curl https://www.cloudflare.com/ips-v6`; do ufw allow from $i to any port 80; done
```

Original script by [raeesbhatti](https://gist.github.com/raeesbhatti/e336ab920ab523335937).

Set permissions `chmod 744 ufw.sh` and run the script `./ufw.sh`.

##### 2.3. Deny all other connections
`ufw default deny`

## 3. Artifact set up

##### 3.1. Obtain Gson `com.google.code.gson:gson:2.8.5` and MySQL connector `mysql:mysql-connector-java:8.0.12` from Maven

##### 3.2. Compile project and export artifacts

##### 3.3. Run the program `java -Xmx128M -jar poewatch.jar`
