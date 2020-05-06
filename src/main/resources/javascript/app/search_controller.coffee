
SearchController =
  logger: Sirius.Application.get_logger("SearchController")

  search_results: []

  search_page: "search"

  show: (page) ->
    page = parseInt(page || 1)
    @_process(@search_results, page)

  filter: (event) ->
    @search_results = []
    query = jQuery(event.target).val()

    fav = is_favorite()

    request =
      inFavorites: fav
      query: query

    if query.is_empty()
      # reload all
      Sirius.Application.get_adapter().and_then (adapter) ->
        adapter.fire(document, "sources:reload")

      if fav
        FeedsController.favorites(1)

    else
      # next find by feeds
      ajax.search JSON.stringify(request),
        (feeds) =>
          if fav
            render_feeds(feeds, 1, "/favorites")

          else

            obj = {}
            sources = new Set()
            for feed in feeds
              source_id = feed.source_id()
              sources.add(source_id)
              if obj[source_id]?
                obj[source_id].push(feed)
              else
                obj[source_id] = [feed]

            sources = Array.from(sources)

            full =
              Sources.filter (x) ->
                sources.contains(x.id()) ||  x.name().contains(query) || x.url().contains(query)
            ids = full.map((x) -> x.id())
            Sources.map((x) ->
              unless ids.contains(x.id())
                x.active(false)
              else
                x.feeds([])
                if obj[x.id()]?
                  x.feeds(obj[x.id()])
                  x.count(obj[x.id()].length)
                else
                  x.count(0)
            )
            @search_results = feeds
            @_process(feeds, 1)

        (err) =>
          @logger.warn("Failed to process search: #{JSON.stringify(err)}")

# if now is favorites - search only in favorites
# otherwise sources+ feeds+sources


  _process: (feeds, current_page) ->
    render_feeds(feeds, current_page, @search_page)



