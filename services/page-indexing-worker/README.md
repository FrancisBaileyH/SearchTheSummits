# Page Indexing Worker
The page indexing worker is responsible for indexing content from various websites. It exposes a set of APIs for assigning queues to. From there it will continuously poll the queue for new tasks. The main "entry" point to the processing is the `PageIndexingTaskCoordinator`.

Queue assignment is handled by the PageIndexingCoordinator service and queue assignments are only kept if the instance is receiving heart beats from the coordinator.

## Building
```
./gradlew build
docker-compose --profile backend build
docker-compose --profile backend up -d // starts both coordinator and worker
```

## Publishing
```
docker tag search-the-summits/page-indexing-worker francisbaileyh/search-the-summits:worker-latest 
docker push francisbaileyh/search-the-summits:worker-latest
```

## Assigning Queues Manually
It's best to update the `index-source-store-test` (if running locally) to assign queues as the coordinator will handle this automatically. However, if you want to assign queues manually, you can do so as long as the worker is receiving heartbeats from the coordinator.
Run assignments like so:
```
curl --insecure -XPUT -H "Content-type: application/json" -d '{
    "assignments": [
        "https://sqs.us-west-2.amazonaws.com/259609947632/IndexingQueue-Test-skisickness-com"
    ]
}' 'http://localhost:8081/api/assignments'
```

## Prod Setup
### ElasticSearch
```
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-8.5.2-amd64.deb
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-8.5.2-amd64.deb.sha512
shasum -a 512 -c elasticsearch-8.5.2-amd64.deb.sha512
sudo dpkg -i elasticsearch-8.5.2-amd64.deb
```

Watch for the output and copy the `generated password`

```
--------------------------- Security autoconfiguration information ------------------------------

Authentication and authorization are enabled.
TLS for the transport and HTTP layers is enabled and configured.

The generated password for the elastic built-in superuser is : <PASSWORD HERE>

If this node should join an existing cluster, you can reconfigure this with
'/usr/share/elasticsearch/bin/elasticsearch-reconfigure-node --enrollment-token <token-here>'
after creating an enrollment token on your existing cluster.

You can complete the following actions at any time:

Reset the password of the elastic built-in superuser with 
'/usr/share/elasticsearch/bin/elasticsearch-reset-password -u elastic'.

Generate an enrollment token for Kibana instances with 
 '/usr/share/elasticsearch/bin/elasticsearch-create-enrollment-token -s kibana'.

Generate an enrollment token for Elasticsearch nodes with 
'/usr/share/elasticsearch/bin/elasticsearch-create-enrollment-token -s node'.
```

Start Elasticsearch
```
sudo systemctl daemon-reload
sudo systemctl enable elasticsearch.service 
sudo systemctl start elasticsearch
```

### Redis
Add this docker-compose.yml file on your redis host
```
version: '3'
services:
  redis:
    profiles: ['metadata']
    image: redis:7.0.5
    ports:
      - "6379:6379"
    volumes:
      - redis-volume:/data
    command: redis-server --requirepass <REDIS PW>

volumes:
  redis-volume:
    external: false
```
Start the service
```
docker-compose --profile metadata up -d
```

### PageIndexingWorker
Setup an IAM user with the following permissions:
```
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "VisualEditor0",
            "Effect": "Allow",
            "Action": [
                "sqs:DeleteMessage",
                "sqs:GetQueueUrl",
                "sqs:ListDeadLetterSourceQueues",
                "sqs:ChangeMessageVisibility",
                "sqs:ReceiveMessage",
                "sqs:SendMessage",
                "sqs:GetQueueAttributes"
            ],
            "Resource": [
                "arn:aws:sqs:*:259609947632:sts-index-queue-*"
            ]
        },
        {
            "Sid": "VisualEditor1",
            "Effect": "Allow",
            "Action": "sqs:ListQueues",
            "Resource": "*"
        }
    ]
}
```
*Note if using S3 in AWS add permissions for reading/writing buckets/objects. Otherwise generate a new Access Key and ID*

Add a variables.env file to your prod host with the following contents:
```
AWS_ACCESS_KEY_ID=<IAM USER>
AWS_SECRET_ACCESS_KEY=<IAM USER ACCESS KEY>
ES_ENDPOINT=<ES HOST IP ADDRESS>
ES_FINGERPRINT=<ES FINGER PRINT>
ES_PASSWORD=<ES PASSWORD> // better to create custom roles
ES_USERNAME=<ES USERNAME> // generated in kibana
REDIS_ENDPOINT=<REDIS ENDPOINT>
REDIS_PASSWORD=<REDIS PW>
REDIS_PORT=6379
REDIS_USERNAME=default
ENVIRONMENT_TYPE=PROD
S3_REGION=<S3 REGION>
S3_ENDPOINT=<S3 ENDPOINT>
S3_ACCESS_KEY=<S3 ACCESS KEY>
S3_SECRET_KEY=<S3 SECRET>
S3_STORE_NAME=<BUCKET NAME>
```

Generate the docker-compose.yml file on the prod server:
```
version: '3'
services:
    page-indexing-worker:
      image: francisbaileyh/search-the-summits:worker-latest
      env_file: variables.env
      environment:
        - JAVA_OPTS=-XX:MaxRam=1600m
      ports:
       - "8080:8080"
       - "9090:9090"
      volumes:
      - page-indexing-volume:/service/var/logs
      restart: unless-stopped

volumes:
  page-indexing-volume:
    external: false
```

```
docker-compose up -d
```