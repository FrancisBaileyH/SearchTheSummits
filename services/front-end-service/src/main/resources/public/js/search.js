var perPageResult = 20;

$(document).ready(function() {
    var queries = new URLSearchParams(window.location.search)

    var urlQuery = queries.get("query")
    var page = queries.get("page")

    if (page == null || isNaN(page)) {
        page = 1
    }

    if (urlQuery != null && urlQuery != "") {
        $(".search-form-container").addClass("search-container-with-results")
        updateSearchResults(urlQuery, Number(page)) // If we do "10" + 4 we get 104...
    } else {

    }

    $('#search-form').submit(function(e) {
        e.preventDefault();
        query = $("#search-bar").val()

        if (query != urlQuery) {
            window.location.href = "/?query=" + query.replace(" ", "+")
        }
    });
});


function updateSearchResults(query, page) {
    $("#search-bar").val(query)
    $(".search-pagination-container").html("")

    var startTime = new Date().getTime();
    var endpoint = "api/summits?query=" + query

    if (page > 1) {
        endpoint += "&next=" + ((page - 1) * perPageResult)
    }

    $.getJSON(endpoint)
     .fail(function(xhr, status, errorThrown) {
        $(".search-results-container").html(
            "<div class='search-result-error'><p>Unable to process query at this time :(</p></div>"
        )
     })
     .done(function(json) {
         // update pagination
         var requestTime = new Date().getTime() - startTime;
         var searchDetails = "<div class=\"search-results-details\">";
         searchDetails += "<p>About " + json.totalHits + " result(s) found in " + requestTime + "ms</p>";
         searchDetails += "</div>";

         $(".search-results-container").html(searchDetails)

         json.hits.forEach(function(hit) {
            var searchResult = "<div class=\"search-result\">"
            searchResult += "<div class=\"search-result-link\">"
            searchResult += "<a href=\"" + hit.source + "\" target=\"_blank\" rel=\"noopener noreferrer\">" + hit.source + "</a>"
            searchResult += "</div>"
            searchResult += "<div class=\"search-result-title\">"
            searchResult += "<h5><a href=\""+ hit.source + "\" target=\"_blank\" rel=\"noopener noreferrer\">" + hit.title + "</a></h5>"
            searchResult += "</div>"
            searchResult += "<div class=\"search-result-highlight\">"
            searchResult += "<p>" + hit.highlight + "...</p>"
            searchResult += "</div>"
            searchResult += "</div>"
            $(".search-results-container").append(searchResult)
         });

         if (json.totalHits > perPageResult && json.hits.length > 0) {
             renderPagination(json, page, query)
         }
     });
}

function renderPagination(json, page, query) {
    var paginationData = getPaginationDisplayData(json.totalHits, page, perPageResult)
    $(".search-pagination-container").html("test")

    var paginationHtml = "<ul><li>Pages: </li>"

    for (i = paginationData.startPage; i <= paginationData.endPage; i++) {
        var cssClass = ""
        if (i == page) {
            cssClass = "current-search-page"
        }
        paginationHtml += "<li><a class='" + cssClass + "' href=\"/?query=" + query.replace(" ", "+") + "&page=" + i + "\">" + i + "</a></li>"
    }

    paginationHtml += "</ul>"

    $(".search-pagination-container").html(paginationHtml)
}


function getPaginationDisplayData(totalItems, currentPage, pageSize) {
    // default to first page
    currentPage = currentPage || 1;

    // default page size is 10
    pageSize = pageSize || 10;

    // calculate total pages
    var totalPages = Math.ceil(totalItems / pageSize);

    var startPage, endPage;
    if (totalPages <= 10) {
        // less than 10 total pages so show all
        startPage = 1;
        endPage = totalPages;
    } else {
        // more than 10 total pages so calculate start and end pages
        if (currentPage <= 6) {
            startPage = 1;
            endPage = 10;
        } else if (currentPage + 4 >= totalPages) {
            startPage = totalPages - 9;
            endPage = totalPages;
        } else {
            startPage = currentPage - 5;
            endPage = currentPage + 4;
        }
    }

    // return object with all pager properties required by the view
    return {
        currentPage: currentPage,
        pageSize: pageSize,
        totalPages: totalPages,
        startPage: startPage,
        endPage: endPage
    };
}