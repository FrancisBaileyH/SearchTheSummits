$(document).ready(function() {
    var queries = new URLSearchParams(window.location.search)

    var urlQuery = queries.get("query")
    var pagination = queries.get("next")

    if (urlQuery != null && urlQuery != "") {
         $(".search-container").addClass("search-container-with-results")
        updateSearchResults(urlQuery)
    }

    $('#search-form').submit(function(e) {
        e.preventDefault();
        query = $("#search-bar").val()

        if (query != urlQuery) {
            var startTime = new Date().getTime();
            window.history.pushState(null, null, "/?query=" + query.replace(" ", "+"))
            updateSearchResults(query)
        }
    });
});


function updateSearchResults(query) {
    var startTime = new Date().getTime();

    $("#search-bar").val(query)

    $.getJSON("api/summits?query=" + query)
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
         $(".search-container").addClass("search-container-with-results")

         json.hits.forEach(function(hit) {
            var searchResult = "<div class=\"search-result\">"
            searchResult += "<div class=\"search-result-link\">"
            searchResult += "<a href=\"" + hit.source + "\">" + hit.source + "</a>"
            searchResult += "</div>"
            searchResult += "<div class=\"search-result-title\">"
            searchResult += "<h5><a href=\""+ hit.source + "\">" + hit.title + "</a></h5>"
            searchResult += "</div>"
            searchResult += "<div class=\"search-result-highlight\">"
            searchResult += "<p>" + hit.highlight + "...</p>"
            searchResult += "</div>"
            searchResult += "</div>"
            $(".search-results-container").append(searchResult)
         });

         console.log(json)
         renderPagination(json)
    });
}

function renderPagination(response) {
    // current page
    // total to display
}