
SourcesController =

  logger: Sirius.Application.get_logger("SourcesController")

  show_all: (page) ->
    page = parseInt(page || 1, 10)
    offset = get_offset(page)
    limit = get_limit()
    href = "/show/all"

    if Sources.size() == 0
      @logger.debug("No sources, nothing to show")
      render_feeds([], page, 0, href)
    else
      ajax.latest(offset, limit, (feeds, total) =>
        render_feeds(feeds, page, total, href)
      )

  show_all_in_source: (normalized, page) ->
    @_show(normalized, page, true)

  show: (normalized, page) ->
    @_show(normalized, page, false)

  _show: (normalized, page, is_loaded_all) ->
    page = parseInt(page || 1, 10)
    offset = get_offset(page)
    limit = get_limit()
    normalized = decodeURIComponent(normalized)
    source = Sources.find('normalized', normalized)

    unread_only = !is_loaded_all

    if source?
      ajax.get_page_of_feeds(source.id(), offset, limit, unread_only,
        (feeds, total) =>
          @logger.info("Load from source: #{source.id()}, #{feeds.length} feeds")
          @_process_feeds(source, feeds, page, total, is_loaded_all)
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
        source = Sources.find('id', id)
        if source
          source.count(0)
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

