
SearchController =
  logger: Sirius.Application.get_logger("SearchController")

  filter: (event) ->
    hc = "uk-hidden"
    query = jQuery(event.target).val()

    # if favorites ...

    # find by source
    xs = Sources.filter (x) -> x.name().contains(query) || x.url().contains(query)

    request =
      inFavorites: false
      query: query

    # next find by feeds
    ajax.search request,
      (feeds) ->
        #

      (err) =>
        @logger.warn("Failed to process search: #{JSON.stringify(err)}")


    # if now is favorites - search only in favorites
    # otherwise sources+ feeds+sources

