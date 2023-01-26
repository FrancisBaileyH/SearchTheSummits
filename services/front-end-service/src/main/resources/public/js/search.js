var perPageResult = 10;
var maxPaginatedResults = 1000;

// I'm not a front-end dev so please don't judge :(
$(document).ready(function() {
    var queries = new URLSearchParams(window.location.search)

    var urlQuery = queries.get("query")
    var page = queries.get("page")

    if (page == null || isNaN(page)) {
        page = 1
    }

    if (urlQuery != null && urlQuery != "") {
        $(".search-form-container").addClass("has-query");
        $(".search-form-container").removeClass("invisible");
        updateSearchResults(urlQuery, Number(page)) // If we do "10" + 4 we get 104... why javascript?!
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
         $(".search-form-container").addClass("has-query");

         json.hits.forEach(function(hitGroup) {
            var searchResult = "<div class=\"search-result-group\">"
            hitGroup.forEach(function(hit, index) {
                var thumbnailHtml = ""

                if (hit.thumbnail != null) {
                    thumbnailHtml += "<div class=\"search-highlight-image-group\">"
                    thumbnailHtml += "  <a href=\""+ hit.source + "\" target=\"_blank\" rel=\"noopener noreferrer\"><img class=\"search-highlight-thumbnail\" src=\""+ hit.thumbnail + "\" /></a>"
                    thumbnailHtml += "</div>"
                }

                searchResult += "<div class=\"search-result\">"
                searchResult += "  <div class=\"search-result-link\">"
                searchResult += "    <a href=\"" + hit.source + "\" target=\"_blank\" rel=\"noopener noreferrer\">" + hit.source + "</a>"
                searchResult += "  </div>"
                searchResult += "  <div class=\"search-result-highlight-group\">"
                searchResult += "    <div class=\"search-result-highlight-text-group\">"
                searchResult += "      <div class=\"search-result-title\">"
                searchResult += "        <h5><a href=\""+ hit.source + "\" target=\"_blank\" rel=\"noopener noreferrer\">" + hit.title + "</a></h5>"
                searchResult += "      </div>"
                searchResult += "      <div class=\"search-result-highlight\">"
                searchResult += "        <p>" + hit.highlight + "...</p>"
                searchResult += "      </div>"
                searchResult += "    </div>"
                searchResult += thumbnailHtml
                searchResult += "  </div>"
                searchResult += "</div>"
             });
             searchResult += "</div>"
             $(".search-results-container").append(searchResult)
         });

         if (json.totalHits > perPageResult && json.hits.length > 0) {
             renderPagination(json, page, query)
         }
     });
}

function renderPagination(json, page, query) {
    var paginationData = getPaginationDisplayData(Math.min(json.totalHits, maxPaginatedResults), page, perPageResult)
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

/**
  Search Animations
*/

// Add something to given element placeholder
function addToPlaceholder(toAdd, el) {
    el.attr('placeholder', el.attr('placeholder') + toAdd);
    // Delay between symbols "typing"
    return new Promise(resolve => setTimeout(resolve, 75));
}

function deleteFromPlaceHolder(el) {
    var placeHolder = el.attr('placeholder')
    el.attr('placeholder', placeHolder.substring(0, placeHolder.length - 1));
    return new Promise(resolve => setTimeout(resolve, 30));
}

// Clear placeholder attribute in given element
function clearPlaceholder(el) {
    el.attr("placeholder", "");
}

// Print one phrase
function printPhrase(phrase, el) {
    return new Promise(resolve => {
        // Clear placeholder before typing next phrase
        clearPlaceholder(el);
        let letters = phrase.split('');
        // For each letter in phrase
        letters.reduce(
            (promise, letter, index) => promise.then(_ => {
                // Resolve promise when all letters are typed
                if (index === letters.length - 1) {
                    resolve()
                }
                return addToPlaceholder(letter, el);
            }),
            Promise.resolve()
        );
    });
}

function clearPhrase(phrase, el) {
    return new Promise(resolve => {
        let letters = phrase.split('');
        letters.reduce(
            (promise, letter, index) => promise.then(_ => {
                // Resolve promise when all letters are typed
                if (index === letters.length - 1) {
                    resolve()
                }
                return deleteFromPlaceHolder(el);
            }),
            Promise.resolve()
        )
    });
}

function delay(timeout) {
    return new Promise(resolve => setTimeout(resolve, timeout));
}

// Print given phrases to element
function printPhrases(phrases, el) {
    clearPlaceholder(el);

    // check for promise cancellation
    // do not clear last search value
    phrases.reduce(
        (promise, phrase, index) => {
            return promise
                 .then(_ => delay(1000))
                .then(_ => printPhrase(phrase, el))
                .then(_ => delay(2000))
                .then(_ => {
                    if (index !== phrases.length - 1) {
                        return clearPhrase(phrase, el)
                    }
                })
        },
        Promise.resolve()
    );
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
        "Sinister Peak"
    ]);

    var animatedPhrases = [firstPhrase]
    animatedPhrases.push(...phrases.slice(0,3));
    animatedPhrases.push(finalPhrase);

    printPhrases(animatedPhrases, $('#search-bar'));
}


function shuffle(array) {
  let currentIndex = array.length,  randomIndex;

  while (currentIndex != 0) {
    randomIndex = Math.floor(Math.random() * currentIndex);
    currentIndex--;

    [array[currentIndex], array[randomIndex]] = [
      array[randomIndex], array[currentIndex]];
  }

  return array;
}