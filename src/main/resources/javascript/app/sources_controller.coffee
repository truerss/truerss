
SourcesController =

  logger: Sirius.Application.get_logger("SourcesController")

  current_unread_feeds: []

  clear_current_feeds: () ->
    @current_unread_feeds = []

  show_all: (page) ->
    # TODO Call latest
    page = parseInt(page || 1, 10)
    sources = Sources.all().sort (a, b) -> parseInt(a.count()) - parseInt(b.count())

    # TODO call server here
    if @current_unread_feeds.is_empty()
      @current_unread_feeds =
        Sources.all().map((x) -> x.feeds()).flat().filter (z) -> !z.read()

    if @current_unread_feeds.is_empty()
      if Sources.all().is_empty()
        @logger.debug("No sources")
        clean_main_page()
      else
        Sirius.redirect(sources[0].href())
    else
      render_feeds(@current_unread_feeds, page, "/show/all")
      Sirius.redirect(sources[0].href())

  show_all_in_source: (normalized, page) ->
    page = parseInt(page || 1, 10)
    offset = get_offset(page)
    limit = get_limit()
    normalized = decodeURIComponent(normalized)
    @clear_current_feeds()
    source = Sources.find('normalized', normalized)

    if source?
      ajax.get_page_of_feeds(source.id(), offset, limit, false,
        (feeds, total) =>
          @_process_feeds(source, feeds, page, total, true)
      )

    else
      @logger.warn "Source #{normalized} was not found"

  show: (normalized, page) ->
    page = parseInt(page || 1, 10)
    normalized = decodeURIComponent(normalized)
    @clear_current_feeds()
    source = Sources.find('normalized', normalized)
    @logger.info("Show: #{normalized} source is exist? #{source != null}")
    limit = get_limit()
    offset = get_offset(page)
    # TODO do not fetch from server render feeds if already present
    if source?
      ajax.get_page_of_feeds(source.id(), offset, limit, true,
        (feeds, total) =>
          @logger.info("Load from source: #{source.id()}, #{feeds.length} feeds")
          @_process_feeds(source, feeds, page, total, false)
      )

    else
      @logger.warn "source: #{normalized} does not exist"

  _process_feeds: (source, feeds, page, total, is_load_all) ->
    get_source_overview(source.id()).then (overview) =>
      overview.is_loaded_all(is_load_all)
      render_feeds_and_source_overview(source, overview, feeds, page, total)

  refresh_all: (e) ->
    ajax.refresh_all()
    e.preventDefault()

  refresh_one: (e, id) ->
    ajax.refresh_one id
    e.preventDefault()

  remove: (e, id) ->
    ajax.remove_source id
    source = Sources.find("id", id)
    if source
      change_count(-source.count())
      Sources.remove(source)
      $("#source-overview-#{source.id()}").remove()
      Sirius.redirect("/show/all")

  edit: (e, id) ->
    @logger.info("update #{id} source")

  mark_by_click_on_count_button: (e, id) ->
    id = parseInt(id, 10)

    if isNaN(id)
      @mark_all()
    else
      get_source_overview(id).then (overview) =>
        current_count = overview.unread_count()
        ajax.mark_as_read(id)
        overview.unread_count(0)
        change_count(-current_count)

    e.preventDefault()

  mark_all: () ->
    ajax.mark_all_as_read()
    @logger.debug("Mark all sources as read")
    count = 0
    Sources.map (x) ->
      count = count + x.count()
      x.count(0)

    change_count(-count)

  reload: () ->
    ajax.sources_all (sources) =>
      # just active. see @SearchController
      for source in sources
        x = Sources.find('id', source.id())
        if x?
          x.active(true)
        else
          Sources.add(x)

  load: () ->
    ajax.sources_all (sources) =>
      # overview
      @logger.info("load #{sources.length} sources")
      sources = sources.sort (a, b) -> parseInt(a.count()) - parseInt(b.count())

      sources.forEach((x) -> Sources.add(x))

      unless sources.is_empty()
        sxs = Sources.all().filter (s) -> s.count() > 0
        @logger.info "redirect to"
        if sxs[0]
          source = sxs[0]
          #TODO redirect(source.href())
        else
          #TODO redirect(Sources.first().href())
