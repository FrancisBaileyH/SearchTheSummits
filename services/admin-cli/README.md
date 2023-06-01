### Importing Data

1. Download latest PBF data from: https://download.geofabrik.de/
2. Filter for peaks and write to OSM file
```
osmosis --read-pbf ~/Downloads/north-america-latest.osm.pbf --node-key-value keyValueList="natural.peak,natural.volcano" --write-xml na-summits.osm
```
3. Run Admin CLI against the osm file