### Setup

The FrontEnd is currently just a single Droplet with Nginx for TLS Termination and a Springboot service listening on port 8080.

docker-compose.yml
```
version: '3'
services:
  front-end-service:
    env_file: variables.env
    ports:
      - "8080:8080"
      - "9090:9090"
    image: francisbaileyh/search-the-summits:front-end-latest
    volumes:
      - front-end-volume:/service/var/logs

volumes:
  front-end-volume:
    external: false
```

```
sudo apt-get install nginx
sudo apt-get install certbot
sudo apt-get install python3-certbot-nginx
```

Update `/etc/nginx/conf/sites-enabled/searchthesummits.com`
```
server {
    server_name searchthesummits.com;

    location / {
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Port $server_port;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_pass http://127.0.0.1:8080;
    }
}
```

Run certbot
```
sudo certbot --nginx -d monitoring.searchthesummits.com

nginx -t && nginx -s reload
```

#### Kibana Cert Renewal
```
nginx -s stop
certbot certonly --standalone -d kibana.searchthesummits.com
```