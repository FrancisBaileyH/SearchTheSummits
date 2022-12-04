# Search the Summits
A custom search engine for all things mountaineering. A custom built distributed web crawler pulls from a curated list of web page sources and indexes their content for searching.

## Testing

### Exporting Test Data
```
docker run --rm --volumes-from pageindexingworker-elasticsearch-1 -v %cd%:/backup busybox tar -cvf /backup/es-test-data.tar /usr/share/elasticsearch/data
```

### Importing Test Data and Running

1. Download the ElasticSearch test data: https://www.francisbaileyh.com/wp-content/uploads/2022/12/es-test-data.tar
2. Open the command line and change to the directory of your download
3. Create the start up data for ES
```
docker volume create es-volume
docker create --mount source=es-volume,target=/usr/share/elasticsearch/data --name ES-TEST-DATA-RESTORE busybox true
docker run --rm --volumes-from ES-TEST-DATA-RESTORE -v %cd%:/backup busybox tar xvf /backup/es-test-data.tar usr/share/elasticsearch/data
```
4. From the main project directory run:
```
.\gradlew build
docker-compose build
docker-compose --profile frontend up
```
You can now query the Search API with:
```
curl localhost:8080/api/summits?query=mount+adams
```

5. To test the crawler run:
```
docker-compose --profile backend up
```


#### Architecture
Overall architecture is as follows:
![image](images/searchthesummits.png)

How each IndexingWorker handles tasks:
![image](images/searchthesummits-worker.png)