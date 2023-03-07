var maxPaginatedResults = 1000;

// I'm not a front-end dev so please don't judge :(
$(document).ready(function() {
    var queries = new URLSearchParams(window.location.search)
    var urlQuery = queries.get("query")

    if (urlQuery != null && urlQuery != "") {
        $(".search-form-container").addClass("has-query");
        $(".search-form-container").removeClass("invisible");
        $("#search-bar").val(urlQuery);

        updateSearchResults();
    } else {
         $(".search-form-container").removeClass("invisible");
         $(".search-form-tagline").removeClass("invisible");
         $('#search-bar').focus();
         runSearchAnimations();
    }

    renderClearButton();

    $('#search-form').submit(function(e) {
        e.preventDefault();
        query = $("#search-bar").val()

        if (query != urlQuery) {
            window.location.href = "/?query=" + query.replaceAll(" ", "+")
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

function updateSearchResults() {
    $(".search-pagination-container").html("")

    var url = new URL(window.location.href);
    var searchParams = url.searchParams

    var resource = searchParams.get("resource")

    switch(resource) {
        case "images":
            updateImageResults(searchParams);
            break;
        default: updateDocumentResults(searchParams);
    }
}

function updateImageResults(searchParams) {
    var startTime = new Date().getTime();
    var endpoint = new URL(window.location.origin + "/api/images");
    var page = searchParams.get("page")

    endpoint.search = "?" + searchParams.toString();

    if (!isNaN(page) && page > 1) {
        endpoint.searchParams.set("next", ((page - 1) * perPageResult));
    }

    endpoint.searchParams.delete("page")

    $.getJSON(endpoint)
    .fail(function(xhr, status, errorThrown) {
        renderErrorMessage();
     })
    .done(function(json) {
        var totalTime = new Date().getTime() - startTime;
        var searchResults = "<div class=\"image-results-grid\">";

        json.hits.forEach(function(hit) {
            var sourceUrl = new URL(hit.referencingDocument);
            searchResults += "<div class=\"search-image-container\" data-fld-width=\"" + hit.imageWidth +"\" data-fld-height=\"" + hit.imageHeight + "\">"
            searchResults += "<figure>"
            searchResults += "<img class=\"search-image\" src=\"" + hit.thumbnail + "\" data-src=\"" + hit.source + "\" data-description=\"" + hit.description +"\" data-host=\"" + sourceUrl.host +"\" data-reference=\"" + hit.referencingDocument + "\"/>"
            searchResults += "<figcaption><a href=\"" + hit.referencingDocument + "\" target=\"_blank\"><span class=\"search-image-reference-host\">" + sourceUrl.host + "</span><br />" + hit.description + "</a></figcaption>"
            searchResults += "</figure>"
            searchResults += "</div>"
        });

        searchResults += "</div>"

        renderSearchResults({
            totalHits: json.totalHits,
            requestTime: totalTime,
            currentHits: json.hits.size,
            resultsPerPage: json.resultsPerPage,
            postRenderCallback: function() {
                var fldGrd = new FldGrd(document.querySelector('.image-results-grid'), {
                    rowHeight: 200,
                    rowHeightOrphan: rows => Math.round(rows.heightAvg),
                    itemSelector: '*',
                    objSelector: 'figure',
                    dataWidth: 'data-fld-width',
                    dataHeight: 'data-fld-height',
                });
                fldGrd.update();

                $('.search-image').on('click', function() {
                    var imageTarget = $(this);
                    renderImageModal(imageTarget);
                });
            }
        }, searchResults)
    });
}

function renderImageModal(imageTarget) {
    $(".search-image-modal").remove();
    var modalContent = "<div class=\"search-image-modal\">"
    modalContent += "<div class=\"centered\"><img src=\"" + imageTarget.data('src') + "\" /></div>"
    modalContent +=  "<p><a href=\"" + imageTarget.data('reference') + "\" target=\"_blank\"><span class=\"search-image-reference-host\">" + imageTarget.data('host') + "</span><br />" + imageTarget.data('description') + "</a></p>"
    modalContent += "</div>"
    $('.modal-holding-zone').html(modalContent);
    $(".search-image-modal").modal();
}

function updateDocumentResults(searchParams) {
    var startTime = new Date().getTime();
    var endpoint = new URL(window.location.origin + "/api/summits");
    var page = searchParams.get("page")

    endpoint.search = "?" + searchParams.toString();

    if (!isNaN(page) && page > 1) {
        endpoint.searchParams.set("next", ((page - 1) * perPageResult));
    }

    endpoint.searchParams.delete("page")

    $.getJSON(endpoint)
    .fail(function(xhr, status, errorThrown) {
        renderErrorMessage();
     })
    .done(function(json) {
        var totalTime = new Date().getTime() - startTime;
        var searchResults = "";

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

            searchResults += searchResult
        });

        renderSearchResults({
            totalHits: json.totalHits,
            requestTime: totalTime,
            currentHits: json.hits.size,
            postRenderCallback: null,
            resultsPerPage: json.resultsPerPage
        }, searchResults)
    });
}

function renderSearchResults(responseData, resultsHtml) {
      var container = $('.search-results-container');
      var searchFormContainer = $('.search-form-container')

      var searchDetails = "<div class=\"search-results-details\">";
      searchDetails += "<p>About " + responseData.totalHits + " result(s) found in " + responseData.requestTime + "ms</p>";
      searchDetails += "</div>";

      container.html(searchDetails)
      searchFormContainer.addClass("has-query");

     if (responseData.totalHits > 0) {
        renderSearchTools();
     }

     container.append(resultsHtml)

     if (responseData.postRenderCallback != null) {
        responseData.postRenderCallback();
     }

     if (responseData.totalHits > perPageResult) {
         renderPagination(responseData.totalHits, responseData.resultsPerPage)
     }

     if (responseData.totalHits < 1) {
        renderNoResultsMessage();
     }
}

function renderErrorMessage() {
    $(".search-results-container").html(
        "<div class='search-result-error'><p>Unable to process query at this time :(</p></div>"
    )
}

function renderNoResultsMessage() {
    var url = new URL(window.location.href);
    var query = url.searchParams.get("query");
    var type = url.searchParams.get("type");

    var noResultsMessage = "<div class=\"search-results-message\"><p>Your search for - <strong>" + query + "</strong> did not match any documents</p>"
    noResultsMessage += "<p>Suggestions:</p><ul>"

    if (type == null || type != "fuzzy") {
        url.searchParams.set("type", "fuzzy");
        noResultsMessage += "<li>Expand your search results with <a href=\"" + url + "\" >fuzzy match</a></li>"
    }

    noResultsMessage += "<li>Make sure all words are spelled correctly.</li>"
    noResultsMessage += "<li>Try fewer keywords.</li>"
    noResultsMessage += "<li>Try different keywords.</li></ul></div>"

    $(".search-results-container").append(noResultsMessage);
}

function renderSearchTools() {
    var url = new URL(window.location.href);
    var settingsSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" fill=\"currentColor\" class=\"bi bi-gear\" viewBox=\"0 0 16 16\"><path d=\"M8 4.754a3.246 3.246 0 1 0 0 6.492 3.246 3.246 0 0 0 0-6.492zM5.754 8a2.246 2.246 0 1 1 4.492 0 2.246 2.246 0 0 1-4.492 0z\"/><path d=\"M9.796 1.343c-.527-1.79-3.065-1.79-3.592 0l-.094.319a.873.873 0 0 1-1.255.52l-.292-.16c-1.64-.892-3.433.902-2.54 2.541l.159.292a.873.873 0 0 1-.52 1.255l-.319.094c-1.79.527-1.79 3.065 0 3.592l.319.094a.873.873 0 0 1 .52 1.255l-.16.292c-.892 1.64.901 3.434 2.541 2.54l.292-.159a.873.873 0 0 1 1.255.52l.094.319c.527 1.79 3.065 1.79 3.592 0l.094-.319a.873.873 0 0 1 1.255-.52l.292.16c1.64.893 3.434-.902 2.54-2.541l-.159-.292a.873.873 0 0 1 .52-1.255l.319-.094c1.79-.527 1.79-3.065 0-3.592l-.319-.094a.873.873 0 0 1-.52-1.255l.16-.292c.893-1.64-.902-3.433-2.541-2.54l-.292.159a.873.873 0 0 1-1.255-.52l-.094-.319zm-2.633.283c.246-.835 1.428-.835 1.674 0l.094.319a1.873 1.873 0 0 0 2.693 1.115l.291-.16c.764-.415 1.6.42 1.184 1.185l-.159.292a1.873 1.873 0 0 0 1.116 2.692l.318.094c.835.246.835 1.428 0 1.674l-.319.094a1.873 1.873 0 0 0-1.115 2.693l.16.291c.415.764-.42 1.6-1.185 1.184l-.291-.159a1.873 1.873 0 0 0-2.693 1.116l-.094.318c-.246.835-1.428.835-1.674 0l-.094-.319a1.873 1.873 0 0 0-2.692-1.115l-.292.16c-.764.415-1.6-.42-1.184-1.185l.159-.291A1.873 1.873 0 0 0 1.945 8.93l-.319-.094c-.835-.246-.835-1.428 0-1.674l.319-.094A1.873 1.873 0 0 0 3.06 4.377l-.16-.292c-.415-.764.42-1.6 1.185-1.184l.292.159a1.873 1.873 0 0 0 2.692-1.115l.094-.319z\"/></svg>"
    var searchToolsHtml = "<div class=\"search-results-tools\"><span class=\"search-tool-icon\">" + settingsSvg + "</span></div>"
    var container = $(".search-results-container")

    container.append(searchToolsHtml);

    var anchor = $(".search-results-tools")

    var tools = [
        {
            name: "sort",
            options: [
                { display: "Relevance", value: "relevance" },
                { display: "Date", value: "date" }
            ],
            resetPage: false
        },
        {
            name: "type",
            options: [
                { display: "Exact Match", value: "exact" },
                { display: "Fuzzy Match", value: "fuzzy" }
            ],
            resetPage: true
        }
    ]

    tools.forEach(function(tool) {
        renderSearchTool(anchor, tool, url)
    });
}

function renderSearchTool(anchor, tool, url) {
    var currentOption = url.searchParams.get(tool.name);
    var toolHtml = "<div class=\"tool-menu\" id=\"search-" + tool.name + "-menu\">"
    toolHtml += "<select id=\"search-" + tool.name + "\">"

    tool.options.forEach(function(option) {
        var selected = ""

        if (currentOption !== null && option.value.toLowerCase() == currentOption.toLowerCase()) {
            selected = "selected=\"true\""
        }

        toolHtml += "<option value=\"" + option.value + "\"" + selected + ">" + option.display + "</option>"
    });

    toolHtml += "</select>"
    toolHtml += "</div>"

    anchor.append(toolHtml);

    var select = new CustomSelect({
        elem: "search-" + tool.name
    });

    $('#search-' + tool.name).change(function() {
        var value = $('#search-' + tool.name).val();
        if (tool.resetPage) {
            url.searchParams.delete("page");
        }

        url.searchParams.set(tool.name, value);
        window.location.href = url;
    });
}

function renderPagination(totalHits, resultsPerPage) {
    var url = new URL(window.location.href);
    var page = url.searchParams.get("page")

    if (page == null || isNaN(page)) {
        page = 1
    }

    page = Number(page) // If we do "10" + 4 we get 104... why javascript?!

    var paginationData = getPaginationDisplayData(Math.min(totalHits, maxPaginatedResults), page, resultsPerPage)
    $(".search-pagination-container").html("");

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