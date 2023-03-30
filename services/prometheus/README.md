# Monitoring Setup

### Nginx/TLS Termination
Install Nginx + Certbot
```
apt-get update
apt-get install nginx
apt-get install certbot
apt-get install python3-certbot-nginx
```

Write the nginx config to `/etc/nginx/sites-enabled/monitoring.searchthesummits.com.conf`
```
server {
    server_name monitoring.searchthesummits.com;

    location / {
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Port $server_port;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_pass http://127.0.0.1:3000;
    }
}
```
Generate Certificate
```
nginx -t && nginx -s reload
certbot --nginx -d monitoring.searchthesummits.com
```

Generate the docker-compose.yml
```
version: '3'
services:
  prometheus:
    profiles: ['monitoring']
    image: prom/prometheus
    ports:
      - "9090:9090"
    volumes:
      - type: volume
        source: prometheus-volume
        target: /prometheus
      - type: bind
        source: /root/prometheus.yml
        target: /etc/prometheus/prometheus.yml

  grafana:
    profiles: ['monitoring']
    image: grafana/grafana
    volumes:
      - grafana-volume:/var/lib/grafana
    ports:
      - "3000:3000"

volumes:
  grafana-volume:
    external: false
  prometheus-volume:
    external: false
```
Start the services
```
docker-compose --profile monitoring up -d
```