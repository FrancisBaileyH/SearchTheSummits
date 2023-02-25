rootProject.name = "search-the-summits"
include("services:page-indexing-coordinator")
include("services:page-indexing-worker")
include("services:front-end-service")
include("lib:search-index-service")
include("lib:service-common")
include("lib:kotlin-htmldate")
include("lib:indexing-queue-client")
include("api:page-indexing-worker")