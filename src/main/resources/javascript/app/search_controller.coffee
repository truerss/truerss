
SearchController =
  logger: Sirius.Application.get_logger("SearchController")

  filter: (event) ->
    hc = "uk-hidden"
    query = jQuery(event.target).val()

    # if favorites ...

    # find by source

    request =
      inFavorites: false
      query: query

    # next find by feeds
    ajax.search JSON.stringify(request),
      (feeds) ->
        sources = (feeds.map (x) -> x.source_id()).uniq()
        full = Sources.filter (x) -> sources.contains(x.id()) ||  x.name().contains(query) || x.url().contains(query)
        console.log(full)


      (err) =>
        @logger.warn("Failed to process search: #{JSON.stringify(err)}")


    # if now is favorites - search only in favorites
    # otherwise sources+ feeds+sources

