# Incredibly crude set-up instructions page

## 1. Install prequisites
`sudo apt-get install apache2 php libapache2-mod-php openjdk-9-jre-headless -y`

## 2. Prepare subdomain
`sudo nano /etc/apache2/sites-available/api.poe.ovh.conf`

```
<VirtualHost *:80>
	ServerName api.poe.ovh
	DocumentRoot /home/ubuntu/http
	
	<Directory /home/ubuntu/http>
		Options Indexes FollowSymLinks
		AllowOverride All
		Require all granted
	</Directory>
	
	ErrorLog ${APACHE_LOG_DIR}/error.log
	CustomLog ${APACHE_LOG_DIR}/access.log combined
</VirtualHost>
```

## 3. Final touches to apache
`sudo a2enmod rewrite` and `sudo service apache2 restart`

## 4. Run the program
`java -jar StashAPILoader.jar -workers 3 -id new`
