
SourcesController =

  logger: Sirius.Application.get_logger("SourcesController")

  show: (normalized, page) ->
    page ||= 1
    page = parseInt(page, 10)
    normalized = decodeURIComponent(normalized)
    source = Sources.takeFirst (s) -> s.normalized() == normalized
    @logger.info("Show: #{normalized} source is exist? #{source != null}")
    # TODO do not fetch from server render feeds if already present
    if source?
      ajax.get_unread source.id(), (feeds) =>
        @logger.info("Load from source: #{source.id()}, #{feeds.length} feeds")
        source.reset('feeds')
        feeds = feeds.map((x) -> Feed.create(x))
        source.add_feeds(feeds)

        ajax.get_source_overview source.id(), (overview) =>

          source.favorites_count(overview.favoritesCount)
          source.count(overview.unreadCount)

          if feeds.length > 0
            render_source_feeds_and_redirect_to_first(source, page, normalized, overview)
          else
            # if not unread
            ajax.get_feeds source.id(), (feeds) ->
              feeds = feeds.map ((x) -> Feed.create(x))
              source.add_feeds(feeds)

              render_source_feeds_and_redirect_to_first(source, page, normalized, overview)

      state.to(States.Source)
      posts.clear()
      sources.set(source.id())

    else
      @logger.warn "source: #{normalized} does not exist"

  refresh_all: () ->
    ajax.refresh_all()

  refresh_one: (e, id) ->
    ajax.refresh_one id

  remove: (e, id) ->
    ajax.remove_source id
    source = Sources.find("id", id)
    if source
      Sources.remove(source)
      # TODO current table. does sirius do it?
      jQuery("#all-sources tr.source-#{id}").remove()


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

  mark_by_click_on_count_button: (_, id) ->
    id = parseInt(id, 10)
    source = Sources.takeFirst (s) -> s.id() == id
    if source
      ajax.mark_as_read(id)
      source.count(0)
    else
      @logger.warn("source with id=#{id} not found")

  mark: (event, id) ->
    unless !(state.hasState(States.Source) || state.hasState(States.Feed))
      url = location.pathname
      if url.startsWith("/show/")
        normalized = decodeURIComponent(url.split("/")[2])
        source = Sources.takeFirst (s) -> s.normalized() == normalized
        if source
          ajax.mark_as_read(source.id())
          source.count(0)
        else
          @logger.warn("source not found with normalized: '#{normalized}' from '#{url}'")

    event.preventDefault()

  mark_all: (event) ->
    ajax.mark_all_as_read()
    Sources.all().forEach (s) -> s.count(0)
    return

  filter: (event) ->
    hc = "uk-hidden"
    q = jQuery(event.target).val()
    if q.trim().length == 0
      jQuery("li.source-element").removeClass(hc)

    hide = Sources.filter (s) -> s.name().indexOf(q) == -1
    show = Sources.filter (s) -> s.name().indexOf(q) != -1

    if show.length > 0
      show_ids = show.map (s) -> "\#source-#{s.id()}"
      jQuery(show_ids.join(",")).removeClass(hc)

    if hide.length > 0
      hide_ids = hide.map (s) -> "\#source-#{s.id()}"
      jQuery(hide_ids.join(",")).addClass(hc)

  download: (e) ->
    window.open("/api/v1/sources/opml")

  fetch_unread: (e, source) ->
    ajax.get_unread source.id(), (feeds) ->
      feeds = feeds.map (x) -> Feed.create(x)
      source.add_feeds(feeds)
