# Incredibly crude set-up instructions page

## 0. Compile project and export artifacts

## 1. Install prequisites
```sudo apt-get install apache2 php libapache2-mod-php openjdk-9-jre-headless -y```

## 2. Prepare subdomains
```sudo nano /etc/apache2/sites-available/poe-stats.com.conf```
```
<VirtualHost *:80>
    ServerName poe-stats.com
    ServerAlias www.poe-stats.com
    DocumentRoot /home/ubuntu/http/html

    <Directory /home/ubuntu/http/html>
        Options Indexes FollowSymLinks
        AllowOverride All
        Require all granted
    </Directory>

    ErrorLog ${APACHE_LOG_DIR}/error.log
    CustomLog ${APACHE_LOG_DIR}/access.log combined
</VirtualHost>
```

```sudo nano /etc/apache2/sites-available/api.poe-stats.com.conf```
```
<VirtualHost *:80>
    ServerName api.poe-stats.com
    DocumentRoot /home/ubuntu/http/api

    <Directory /home/ubuntu/http/api>
        Options Indexes FollowSymLinks
        AllowOverride All
        Require all granted
    </Directory>

    ErrorLog ${APACHE_LOG_DIR}/error.log
    CustomLog ${APACHE_LOG_DIR}/access.log combined
</VirtualHost>
```

`sudo nano /etc/apache2/sites-available/dev.poe-stats.com.conf`
```
<VirtualHost *:80>
    ServerName dev.poe-stats.com
    DocumentRoot /home/ubuntu/http/dev

    <Directory /home/ubuntu/http/dev>
        Options Indexes FollowSymLinks
        AllowOverride All
        Require all granted
    </Directory>

    ErrorLog ${APACHE_LOG_DIR}/error.log
    CustomLog ${APACHE_LOG_DIR}/access.log combined
</VirtualHost>
```

## 3. Final touches to apache
```
sudo a2enmod rewrite
sudo a2enmod headers
sudo a2ensite poe-stats.com.conf
sudo a2ensite dev.poe-stats.com.conf
sudo a2ensite api.poe-stats.com.conf
sudo a2dissite 000-default.conf
sudo service apache2 restart
```

## 4. Run the program
```java -jar Stashy.jar -workers 3 -id new```
