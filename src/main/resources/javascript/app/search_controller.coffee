
SearchController =
  logger: Sirius.Application.get_logger("SearchController")

  search_results: []

  _current_request: null

  search_page: "/search"
  favorites_search_page: "/favorites/search"

  show: (page) ->
    page = parseInt(page || 1, 10)
    @_current_request.offset = get_offset(page)
    @_current_request.limit = get_limit()

    @_process_request(@_current_request, page)

  # if now is favorites - search only in favorites
  # otherwise sources+ feeds+sources
  filter: (event) ->
    query = jQuery(event.target).val()

    fav = is_favorite()

    request =
      inFavorites: fav
      query: query
      offset: get_offset(1) # as first page
      limit: get_limit()

    @_current_request = request

    if query.is_empty()
      # reload all
      Sirius.Application.get_adapter().and_then (adapter) ->
        adapter.fire(document, "sources:reload")
      @_current_request = null

    else
      @_process_request(request, 1)


  _process_request: (request, page) ->
    query = request.query
    ajax.search JSON.stringify(request),
      (feeds, total) =>
        if request.inFavorites
          @_process(feeds, page, total, @favorites_search_page)

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
              if obj[x.id()]?
                x.count(obj[x.id()].length)
              else
                x.count(0)
          )

          @_process(feeds, page, total, @search_page)

  _process: (feeds, current_page, total, href) ->
    render_feeds(feeds, current_page, total, href)



