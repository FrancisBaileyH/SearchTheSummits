var perPageResult = 10;
var maxPaginatedResults = 1000;

// I'm not a front-end dev so please don't judge :(
$(document).ready(function() {
    var queries = new URLSearchParams(window.location.search)

    var urlQuery = queries.get("query")
    var page = queries.get("page")
    var sort = queries.get("sort")
    var type = queries.get("type")

    if (page == null || isNaN(page)) {
        page = 1
    }

    if (urlQuery != null && urlQuery != "") {
        $(".search-form-container").addClass("has-query");
        $(".search-form-container").removeClass("invisible");
        updateSearchResults(urlQuery, Number(page), sort, type) // If we do "10" + 4 we get 104... why javascript?!
    } else {
         $(".search-form-container").removeClass("invisible");
         $(".search-form-tagline").removeClass("invisible");
         runSearchAnimations();
    }

    renderClearButton();


    $('#search-form').submit(function(e) {
        e.preventDefault();
        query = $("#search-bar").val()

        if (query != urlQuery) {
            window.location.href = "/?query=" + query.replace(" ", "+")
        }
    });

    $('#search-bar').on('input', function() {
        renderClearButton();
    });

    $('.search-clear-button').on('click', function() {
        $("#search-bar").val("");
        renderClearButton();
        $("#search-bar").focus();
    });
});

function renderClearButton() {
    var hasContent = $("#search-bar").val();
    $('.search-clear-button').toggle(Boolean(hasContent));
}

function updateSearchResults(query, page, sort, type) {
    $("#search-bar").val(query)
    $(".search-pagination-container").html("")

    var startTime = new Date().getTime();
    var endpoint = "api/summits?query=" + query

    if (page > 1) {
        endpoint += "&next=" + ((page - 1) * perPageResult)
    }

    if (sort != "") {
        endpoint += "&sort=" + sort
    }

    if (type != "") {
        endpoint += "&type=" + type
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
         $(".search-form-container").addClass("has-query");

         renderSearchTools();

         json.hits.forEach(function(hit) {
            var thumbnailHtml = ""
            var mobileThumbnailHtml = ""
            var linkHtml = ""

            if (hit.thumbnail != null) {
                thumbnailHtml += "<div class=\"search-highlight-image-group\">"
                thumbnailHtml += "  <a href=\""+ hit.source + "\" target=\"_blank\" rel=\"noopener noreferrer\"><img class=\"search-highlight-thumbnail\" src=\""+ hit.thumbnail + "\" /></a>"
                thumbnailHtml += "</div>"
                mobileThumbnailHtml += "<div class=\"search-highlight-mobile-image-group\">"
                mobileThumbnailHtml += "<a href=\""+ hit.source + "\" target=\"_blank\" rel=\"noopener noreferrer\"><img class=\"search-highlight-thumbnail\" src=\""+ hit.thumbnail + "\" /></a>"
                mobileThumbnailHtml += "</div>"
            }

            linkHtml += "  <div class=\"search-result-link\">"
            linkHtml += "    <div class=\"search-result-link-value\">"
            linkHtml += "        <a href=\"" + hit.source + "\" target=\"_blank\" rel=\"noopener noreferrer\">" + hit.source + "</a>"
            linkHtml += "    </div>"

            if (hit.source.toLowerCase().endsWith(".pdf")) {
                linkHtml += "<div class=\"search-result-link-label\">"
                linkHtml += "  <span>PDF</span>"
                linkHtml += "</div>"
            }

            linkHtml += "</div>"

            var searchResult = "<div class=\"search-result\">"
            searchResult += linkHtml
            searchResult += "  <div class=\"search-result-highlight-group\">"
            searchResult += "    <div class=\"search-result-highlight-text-group\">"
            searchResult += "      <div class=\"search-result-title\">"
            searchResult += "        <h5><a href=\""+ hit.source + "\" target=\"_blank\" rel=\"noopener noreferrer\">" + hit.title + "</a></h5>"
            searchResult += "      </div>"
            searchResult += "      <div class=\"search-result-highlight\">"
            searchResult += "        <p>" + hit.highlight + "...</p>"
            searchResult +=          mobileThumbnailHtml
            searchResult += "      </div>"
            searchResult += "    </div>"
            searchResult += thumbnailHtml
            searchResult += "  </div>"
            searchResult += "</div>"
            $(".search-results-container").append(searchResult)
         });

         if (json.totalHits > perPageResult && json.hits.length > 0) {
             renderPagination(json, page, query)
         }
     });
}

function renderSearchTools() {
    var url = new URL(window.location.href);
    var settingsSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" fill=\"currentColor\" class=\"bi bi-gear\" viewBox=\"0 0 16 16\"><path d=\"M8 4.754a3.246 3.246 0 1 0 0 6.492 3.246 3.246 0 0 0 0-6.492zM5.754 8a2.246 2.246 0 1 1 4.492 0 2.246 2.246 0 0 1-4.492 0z\"/><path d=\"M9.796 1.343c-.527-1.79-3.065-1.79-3.592 0l-.094.319a.873.873 0 0 1-1.255.52l-.292-.16c-1.64-.892-3.433.902-2.54 2.541l.159.292a.873.873 0 0 1-.52 1.255l-.319.094c-1.79.527-1.79 3.065 0 3.592l.319.094a.873.873 0 0 1 .52 1.255l-.16.292c-.892 1.64.901 3.434 2.541 2.54l.292-.159a.873.873 0 0 1 1.255.52l.094.319c.527 1.79 3.065 1.79 3.592 0l.094-.319a.873.873 0 0 1 1.255-.52l.292.16c1.64.893 3.434-.902 2.54-2.541l-.159-.292a.873.873 0 0 1 .52-1.255l.319-.094c1.79-.527 1.79-3.065 0-3.592l-.319-.094a.873.873 0 0 1-.52-1.255l.16-.292c.893-1.64-.902-3.433-2.541-2.54l-.292.159a.873.873 0 0 1-1.255-.52l-.094-.319zm-2.633.283c.246-.835 1.428-.835 1.674 0l.094.319a1.873 1.873 0 0 0 2.693 1.115l.291-.16c.764-.415 1.6.42 1.184 1.185l-.159.292a1.873 1.873 0 0 0 1.116 2.692l.318.094c.835.246.835 1.428 0 1.674l-.319.094a1.873 1.873 0 0 0-1.115 2.693l.16.291c.415.764-.42 1.6-1.185 1.184l-.291-.159a1.873 1.873 0 0 0-2.693 1.116l-.094.318c-.246.835-1.428.835-1.674 0l-.094-.319a1.873 1.873 0 0 0-2.692-1.115l-.292.16c-.764.415-1.6-.42-1.184-1.185l.159-.291A1.873 1.873 0 0 0 1.945 8.93l-.319-.094c-.835-.246-.835-1.428 0-1.674l.319-.094A1.873 1.873 0 0 0 3.06 4.377l-.16-.292c-.415-.764.42-1.6 1.185-1.184l.292.159a1.873 1.873 0 0 0 2.692-1.115l.094-.319z\"/></svg>"
    var searchToolsHtml = "<div class=\"search-results-tools\"><span class=\"search-tool-icon\">" + settingsSvg + "</span></div>"
    var container = $(".search-results-container")

    container.append(searchToolsHtml);

    var anchor = $(".search-results-tools")

    renderSearchTool(anchor, "sort", [ "Relevance", "Date" ], url)
    renderSearchTool(anchor, "type", [ "Strict Search", "Fuzzy Search" ], url)
}

function renderSearchTool(anchor, name, options, url) {
    var currentOption = url.searchParams.get(name);
    var toolHtml = "<div class=\"tool-menu\" id=\"search-" + name + "-menu\">"
    toolHtml += "<select id=\"search-" + name + "\">"

    options.forEach(function(option) {
        var selected = ""

        if (currentOption !== null && option.toLowerCase() == currentOption.toLowerCase()) {
            selected = "selected=\"true\""
        }

        toolHtml += "<option value=\"" + option.toLowerCase() + "\"" + selected + ">" + option + "</option>"
    });

    toolHtml += "</select>"
    toolHtml += "</div>"

    anchor.append(toolHtml);

    var select = new CustomSelect({
        elem: "search-" + name
    });

    $('#search-' + name).change(function() {
        var value = $('#search-' + name).val();

        url.searchParams.set(name, value);
        window.location.href = url;
    });
}

function renderPagination(json, page, query) {
    var url = new URL(window.location.href);
    var paginationData = getPaginationDisplayData(Math.min(json.totalHits, maxPaginatedResults), page, perPageResult)
    $(".search-pagination-container").html("test")

    var paginationHtml = "<ul><li>Pages: </li>"

    for (i = paginationData.startPage; i <= paginationData.endPage; i++) {
        var cssClass = ""
        if (i == page) {
            cssClass = "current-search-page"
        }

        url.searchParams.set("page", i);
        paginationHtml += "<li><a class='" + cssClass + "' href=\"" +  url + "\">" + i + "</a></li>"
    }

    paginationHtml += "</ul>"

    $(".search-pagination-container").html(paginationHtml)
}

// Start typing
function runSearchAnimations() {
    let firstPhrase = "Search for a summit..."
    let finalPhrase = firstPhrase
    let phrases = shuffle([
        "Mendenhall Towers",
        "Lincoln Peak",
        "Devils Throne",
        "Phyllis's Engine",
        "South Early Winters Spire",
        "Locomotive Mountain",
        "Ember Mountain",
        "Mount Baker",
        "Liberty Bell",
        "Mount Cayley",
        "Nursery Peak",
        "Mount Robson",
        "Mount Bonnycastle",
        "Nivalis",
        "Sinister Peak",
        "Harvey North Ramp",
        "Parkhurst Peak Couloir",
        "Judge Howay",
        "Mazama Dome"
    ]);

    var animatedPhrases = [firstPhrase]
    animatedPhrases.push(...phrases.slice(0,3));
    animatedPhrases.push(finalPhrase);

    printPhrases(animatedPhrases, $('#search-bar'));
}