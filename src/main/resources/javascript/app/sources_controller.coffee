
SourcesController =

  logger: Sirius.Application.get_logger("SourcesController")

  show_all: (page) ->
    page = parseInt(page || 1, 10)
    sources = Sources.all().sort (a, b) -> parseInt(a.count()) - parseInt(b.count())

    Sirius.redirect(sources[0].href())

  show_all_in_source: (normalized, page) ->
    page = parseInt(page || 1, 10)
    normalized = decodeURIComponent(normalized)
    source = Sources.find('normalized', normalized)

    if source?
      ajax.get_feeds source.id(),
        (feeds) =>
          source.feeds([])
          source.add_feeds(feeds)
          @_process_feeds(source, source.feeds(), page, true, true)

        (err) =>
          @logger.warn "Unexpected error: #{JSON.stringify(err)}"

    else
      @logger.warn "Source #{normalized} was not found"



  show: (normalized, page) ->
    page = parseInt(page || 1, 10)
    normalized = decodeURIComponent(normalized)
    source = Sources.find('normalized', normalized)
    @logger.info("Show: #{normalized} source is exist? #{source != null}")
    # TODO do not fetch from server render feeds if already present
    if source?
      if source.has_feeds()
        @_process_feeds(source, source.feeds(), page, false)

      else
        ajax.get_unread source.id(), (feeds) =>
          @logger.info("Load from source: #{source.id()}, #{feeds.length} feeds")
          source.reset('feeds')
          source.add_feeds(feeds)

          @_process_feeds(source, feeds, page, true)

    else
      @logger.warn "source: #{normalized} does not exist"

  _process_feeds: (source, feeds, page, reset_count, is_loaded_all = false) ->
    ajax.get_source_overview source.id(), (overview) =>
      source.favorites_count(overview.favoritesCount)

      if reset_count
        current = source.count()
        diff = overview.unreadCount - current
        @change_count(diff)
        source.count(overview.unreadCount)

      overview.is_loaded_all = is_loaded_all

      if feeds.length > 0
        render_feeds_and_source_overview(source, page, overview)
      else
        # if not unread
        ajax.get_feeds source.id(), (feeds) ->
          source.add_feeds(feeds)

          render_feeds_and_source_overview(source, page, overview)

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
      @change_count(-source.count())
      Sources.remove(source)

  edit: (e, id) ->
    @logger.info("update #{id} source")
    source = Sources.find('id', id)
    if source
      hidden_class = "uk-hidden"
      el = "span#source-name-#{source.id()}"

      view = new Sirius.View(el)
      view.render(hidden_class).zoom("span").add_class()
      view.render(hidden_class).zoom("input").remove_class()
#      view.render(hidden_class).zoom("td button").remove_class()
#
#      to_view_transformer = Sirius.Transformer.draw({
#        'name': {
#          to: 'span.source-name'
#        },
#        'interval': {
#          to: 'span.source-interval'
#        }
#      })
#
#      source.bind(view, to_view_transformer)
#
#      to_model_transformer = Sirius.Transformer.draw({
#        "input[name='name']": {
#          to: 'name'
#        },
#        "input[name='interval']": {
#          to: 'interval'
#        }
#      })
#
#      view.bind(source, to_model_transformer)
#      view.on "button", "click", (e) ->
#        ajax.update_source source.id(), source.ajaxify(),
#          (s) ->
#            logger.info("update source #{source.id()}")
#            view.render(hidden_class).zoom("td input").add_class()
#            view.render(hidden_class).zoom("td button").add_class()
#            view.render(hidden_class).zoom("td > span").remove_class()
#            view.render(hidden_class).zoom("td a.source-link").remove_class()
#        (e) ->
#            logger.error("error on update source: #{e}")

  mark_by_click_on_count_button: (e, id) ->
    id = parseInt(id, 10)

    if isNaN(id)
      @mark_all()
    else
      source = Sources.takeFirst (s) -> s.id() == id
      if source
        current_count = source.count()
        ajax.mark_as_read(id)
        source.count(0)
        @change_count(-current_count)
      else
        @logger.warn("source with id=#{id} not found")

    e.preventDefault()

  mark_all: () ->
    ajax.mark_all_as_read()
    @logger.debug("Mark all sources as read")
    count = 0
    Sources.map (x) ->
      count = count + x.count()
      x.count(0)

    @change_count(-count)

  change_count: (count) ->
    Templates.sources_all_view
      .zoom("#source-count-all")
      .render(count)
      .sum()

  reload: () ->
    ajax.sources_all (sources) =>
      # just active. see @SearchController
      for source in sources
        x = Sources.find('id', source.id())
        if x?
          x.count(source.count())
          x.feeds([])
          x.active(true)
        else
          Sources.add(x)

  load: () ->
    ajax.sources_all (sources) =>
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
