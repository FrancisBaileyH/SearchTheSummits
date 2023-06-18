var maxPaginatedResults = 1000;
let typingTimeout = null;
var timeoutMs = 0;

// I'm not a front-end dev so please don't judge :(
$(document).ready(function() {
    var queries = new URLSearchParams(window.location.search)
    var urlQuery = queries.get("query")
    var resource = queries.get("resource")

    if (urlQuery != null && urlQuery != "") {
        $(".search-form-container").addClass("has-query");
        $(".search-form-container").removeClass("invisible");
        $(".header-menu").addClass("invisible");
        $("#search-bar").val(urlQuery);
        renderResourceMenu(resource);
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
            var searchParams = new URLSearchParams();
            searchParams.set("query", query)
            if (resource != null) {
                searchParams.set("resource", resource)
            }

            window.location.href = "/?" + searchParams.toString();
        }
    });

    $('#search-bar').on('input', function() {
        renderClearButton();
    });

    $('#search-bar').keyup((e) => {
        if (e.originalEvent.code != "ArrowDown" && e.originalEvent.code != "ArrowUp") {
            clearTimeout(typingTimeout)
            typingTimeout = setTimeout(() => {
                timeoutMs = 300;
                value = $(e.target).val();
                $.get('/api/placenames/autocomplete', { query: $(e.target).val() })
                  .done(suggestions => renderAutoSuggestDropdown(suggestions))
                },
                timeoutMs
            )
        }
    });

    $(".search-form-container").keydown((e) => {
        var currentSuggestion = $("#search-suggestion-box li.current");
        var allSuggestions = $("#search-suggestion-box li");
        var hasSuggestions = $(".search-form-container").hasClass("has-suggestions")
        var searchBar = $("#search-bar");

        if (!hasSuggestions) {
            return;
        }

        if (e.originalEvent.code == "ArrowDown") {
            e.preventDefault();

            if (!currentSuggestion.length) {
                allSuggestions.first().addClass("current")
            } else if (currentSuggestion.next().length) {
                currentSuggestion.next().addClass("current");
                currentSuggestion.removeClass("current");
            }

            searchBar.val($("#search-suggestion-box li.current a").data("suggestion"));
        } else if (e.originalEvent.code == "ArrowUp") {
            e.preventDefault();

            if (currentSuggestion.length && currentSuggestion.prev().length) {
                currentSuggestion.prev().addClass("current");
                currentSuggestion.removeClass("current");
                searchBar.val($("#search-suggestion-box li.current a").data("suggestion"));
            }
        }
    });

    $('.search-clear-button').on('click', function() {
        $("#search-bar").val("");
        renderAutoSuggestDropdown({ placenames: [] });
        renderClearButton();
        $("#search-bar").focus();
    });

    $('.resource-menu li').on('click', function(e) {
        e.preventDefault();
        var selectedElement = $(this)
        var selectedResource = selectedElement.data('resource')
        var resource = queries.get("resource")

        if (resource != selectedElement.data('resource')) {
            var searchParams = new URLSearchParams()
            searchParams.set("query", urlQuery)
            searchParams.set("resource", selectedResource)
            window.location.href = "/?" + searchParams.toString();
        }
    });
});

function renderClearButton() {
    var hasContent = $("#search-bar").val();
    $('.search-clear-button').toggle(Boolean(hasContent));
}

function renderResourceMenu(resource) {
    $(".resource-menu").removeClass("invisible");
    $(".sub-menu-divider").removeClass("invisible");

    if (resource == null) {
        resource = "sites"
    }

    $('.resource-menu li[data-resource=' + resource + ']').addClass('current-nav-page');
}

function renderAutoSuggestDropdown(suggestions) {
    var dropdownHtml = "";
    $(".search-form-container").removeClass("has-suggestions")

    if (suggestions.placenames.length > 0) {
        $(".search-form-container").addClass("has-suggestions")
        dropdownHtml += "<ul>";
        suggestions.placenames.forEach(function(value, index) {
            dropdownHtml += "<li><a data-suggestion='" + value.suggestion +"' href='/?query=" + value.suggestion + "'>" + value.displayName + "</a></li>";
        });
        dropdownHtml += "</ul>";
    }

    $("#search-suggestion-box").html(dropdownHtml);
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
        default:
            updateDocumentResults(searchParams)
            break;
    }
}

function updateImageResults(searchParams) {
    var startTime = new Date().getTime();
    var endpoint = new URL(window.location.origin + "/api/images");

    endpoint.search = "?" + searchParams.toString();

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
            searchResults += "<figcaption style=\"max-width: " + hit.imageWidth + "px;\"><a href=\"" + hit.referencingDocument + "\" target=\"_blank\"><span class=\"search-image-reference-host\">" + sourceUrl.host + "</span><br />" + hit.description + "</a></figcaption>"
            searchResults += "</figure>"
            searchResults += "</div>"
        });

        searchResults += "</div>"

        renderSearchResults({
            totalHits: json.totalHits,
            requestTime: totalTime,
            currentHits: json.hits.length,
            resultsPerPage: json.resultsPerPage,
            postRenderCallback: function() {
                if (json.hits.length > 0) {
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
            }
        }, searchResults)
    });
}

function renderImageModal(imageTarget) {
    $(".search-image-modal").remove();
    var modalContent = "<div class=\"search-image-modal\">"
    modalContent += "<div class=\"centered\"><img src=\"" + imageTarget.data('src') + "\" /></div>"
    modalContent +=  "<p><a href=\"" + imageTarget.data('reference') + "\" target=\"_blank\"><span class=\"search-image-reference-host\">" + imageTarget.data('reference') + "</span><br />" + imageTarget.data('description') + "</a></p>"
    modalContent += "</div>"
    $('.modal-holding-zone').html(modalContent);
    $(".search-image-modal").modal({
        fadeDuration: 100
    });
}

function updateDocumentResults(searchParams) {
    var startTime = new Date().getTime();
    var endpoint = new URL(window.location.origin + "/api/summits");
    var previewEndpoint = new URL(window.location.origin + "/api/images/preview")
    var placeNameEndpoint = new URL(window.location.origin + "/api/placenames")

    endpoint.search = "?" + searchParams.toString();
    previewEndpoint.search = "?" + searchParams.toString();
    placeNameEndpoint.search = "?query=" + searchParams.get("query")

    var deferredSearch = $.getJSON(endpoint)
    var deferredPreviewSearch = $.Deferred().resolve(null);
    var deferredPlaceNameSearch = $.Deferred().resolve(null);

    if (searchParams.get("page") == 1 || searchParams.get("page") == null) {
        deferredPreviewSearch = $.getJSON(previewEndpoint)
        deferredPlaceNameSearch = $.getJSON(placeNameEndpoint)
    }

    $.when(deferredSearch, deferredPreviewSearch, deferredPlaceNameSearch).done(function(searchResponse, previewResponse, placeNameResponse) {
        var totalTime = new Date().getTime() - startTime;
        var searchResults = "";
        var previewMapData = null;
        var hasImages = false;
        var previewMapDescription = "";

        // Preview Container Start
        searchResults = "<div class='preview-container'>";

        if (placeNameResponse != null &&
            placeNameResponse.length > 0 &&
            placeNameResponse[0].placenames.length == 1
        ) {
            var placeName = placeNameResponse[0].placenames[0];
            searchResults += "<div class='preview-map-container'>";
            searchResults += "<div id='preview-map'></div>";
            previewMapDescription += "<div class='preview-map-description'>"
            previewMapDescription += "<strong>Name: </strong>" + placeName.name + "</br>"

            if (placeName.alternativeName != null) {
                previewMapDescription += "<strong>Alternative Name: </strong>" + placeName.alternativeName + "</br>"
            }

            if (placeName.elevation != null) {
                previewMapDescription += "<strong>Elevation: </strong>" + placeName.elevation + "m</br>"
            }

            previewMapDescription += "</div>"
            searchResults += previewMapDescription;
            searchResults += "</div>"

            previewMapData = {
                latitude: placeName.latitude,
                longitude: placeName.longitude
            };
        }

        if (previewResponse != null && previewResponse[0].hits.length >= 3) {
            var count = 0;
            var url = new URL(window.location.href);
            var classes = "preview-image-container"
            hasImages = true;

            url.searchParams.set("resource", "images")

            if (previewMapData != null) {
                classes += " has-preview-map"
            }
            searchResults += "<div class=\"" + classes + "\">"

            previewResponse[0].hits.forEach(function(hit) {
                var sourceUrl = new URL(hit.referencingDocument);

                if (count < 3 || ($(window).width() > 700 && previewMapData == null)) {
                    searchResults += "<div class=\"search-result-image-preview-thumbnail\">"
                    searchResults += "<img class=\"search-image\" src=\"" + hit.thumbnail + "\" data-src=\"" + hit.source + "\" data-description=\"" + hit.description +"\" data-host=\"" + sourceUrl.host +"\" data-reference=\"" + hit.referencingDocument + "\" />"
                    searchResults += "</div>"
                }

                count += 1;
            })
            searchResults += "</div>"
        }

        if (previewMapData != null) {
            searchResults += previewMapDescription;
        }

        searchResults += "</div>";

        if (hasImages) {
            searchResults += "<div class=\"search-result-image-preview-button\"><a href=\"" + url + "\"><span>View Images</span><hr class=\"search-result-image-preview-divider\"></hr></a></div>"
        } else if (previewMapData != null) {
            searchResults += "<hr></hr>"
        }

        // Preview Container End

        searchResponse[0].hits.forEach(function(hit) {
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

            if (hit.source.toLowerCase().includes(".pdf")) {
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
            totalHits: searchResponse[0].totalHits,
            requestTime: totalTime,
            currentHits: searchResponse[0].hits.size,
            postRenderCallback: function() {
                $('.search-image').on('click', function() {
                    var imageTarget = $(this);
                    renderImageModal(imageTarget);
                });

                if (previewMapData != null) {
                    var map = L.map('preview-map', {
                        center: [previewMapData.latitude, previewMapData.longitude],
                        zoomControl: false,
                        zoom: 13
                    });

                    L.marker([
                        previewMapData.latitude,
                        previewMapData.longitude
                    ]).addTo(map);

                    L.tileLayer('https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png', {
                        maxZoom: 18,
                        minZoom: 6,
                        attribution: 'OT:Basemap &copy; <a href="https://www.opentopomap.org/">OpenTopoMap</a> contributors, <a href="https://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>'
                    }).addTo(map);
                }
            },
            resultsPerPage: searchResponse[0].resultsPerPage
        }, searchResults)

    }).fail(function() {
        renderErrorMessage();
    });
}

function renderSearchResults(responseData, resultsHtml) {
      var container = $('.search-results-container');
      var searchFormContainer = $('.search-form-container')

      if (responseData.totalHits > 0) {
          var searchDetails = "<div class=\"search-results-details\">";
          searchDetails += "<p>About " + responseData.totalHits + " result(s) found in " + responseData.requestTime + "ms</p>";
          searchDetails += "</div>";
      }

      container.html(searchDetails)
      searchFormContainer.addClass("has-query");

     if (responseData.totalHits > 0) {
        renderSearchTools(responseData.totalHits);
     }

     container.append(resultsHtml)

     if (responseData.postRenderCallback != null) {
        responseData.postRenderCallback();
     }

     if (responseData.totalHits > responseData.resultsPerPage) {
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

    var noResultsMessage = "<div class=\"search-results-message\">"
    noResultsMessage += "<p>Your search for - <strong>" + query + "</strong> did not match any documents.</p>"
    noResultsMessage += "<p>Suggestions:</p><ul>"
    noResultsMessage += "<li>Try fewer keywords - <strong>\"Ember\"</strong> instead of <strong>\"Ember Mountain\"</strong>.</li>"
    noResultsMessage += "<li>Try different keywords - <strong>\"Serac Peak\"</strong> instead of <strong>\"Serac Mountain\"</strong></li>"
    noResultsMessage += "<li>Make sure all words are spelled correctly.</li>"
    noResultsMessage += "</ul></div>"

    $(".search-results-container").append(noResultsMessage);
}

function renderSearchTools(resultCount) {
    var url = new URL(window.location.href);
    var searchToolsHtml = "<div class=\"search-results-tools\"><span class=\"search-tool-icon\">About " + resultCount + " result(s)</span></div>"
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
        "Parkhurst Mountain Couloir",
        "Judge Howay",
        "Mazama Dome",
        "Serra V",
        "Mount Asperity",
        "Bellicose Peak"
    ]);

    var animatedPhrases = [firstPhrase]
    animatedPhrases.push(...phrases.slice(0,3));
    animatedPhrases.push(finalPhrase);

    printPhrases(animatedPhrases, $('#search-bar'));
}