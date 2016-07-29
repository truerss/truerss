
SourcesController =

  by_source: () ->
    html = Templates.list.render({sources: Sources.all()})
    Templates.article_view.render(html).html()
    state.to(States.List)

  all: () ->
    html = Templates.all_sources_template.render({sources: Sources.all()})
    Templates.article_view.render(html).html()
    state.to(States.Sources)

  refresh_all: () ->
    ajax.refresh_all()

  show: (normalized) ->
    normalized = decodeURIComponent(normalized)
    source = Sources.takeFirst (s) -> s.normalized() == normalized
    if source
      ajax.get_unread source.id(), (feeds) ->
        source.reset('feed')
        feeds = feeds.map (x) ->
          pd = moment(x['publishedDate'])
          x['publishedDate'] = pd
          f = new Feed(x)
          source.add_feed(f)
          f

        if feeds.length > 0
          render_source_feeds_and_redirect_to_first(source)
        else
          ajax.get_feeds source.id(), (feeds) ->
            feeds = feeds.map (x) ->
              pd = moment(x['publishedDate'])
              x['publishedDate'] = pd
              f = new Feed(x)
              source.add_feed(f)
              f

            render_source_feeds_and_redirect_to_first(source)

      state.to(States.Source)
      posts.clear()
      sources.set(source.id())

  refresh_one: (e, id) ->
    ajax.refresh_one id

  remove: (e, id) ->
    ajax.remove_source id

  edit: (e, id) ->
    source = Sources.find('id', id)
    if source
      el = "table tr.source-#{source.id()}"

      view = new Sirius.View(el)
      view.render("uk-hidden").zoom("td > span").add_class()
      view.render("uk-hidden").zoom("td input").remove_class()
      view.render("uk-hidden").zoom("span.errors").remove_class()

      source.bind view,
        "span.source-name": {from: "name"}
        "span.source-url": {from: "url"}
        "span.source-interval": {from: "interval"}
        "span.errors": {from: "errors.url.url_validator"}

      view.bind source,
        "input[name='name']" : {to: "name"}
        "input[name='url']" : {to: "url"}
        "input[name='interval']": {to: "interval"}

      view.on "input[type='button']", "click", (e) ->
        ajax.update_source source.id(), source.ajaxify(),
          (s) ->
            logger.info("update source #{source.id()}")
            view.render("uk-hidden").zoom("td > span").remove_class()
            view.render("uk-hidden").zoom("td input").add_class()
          (e) ->
            logger.error("error on update source")
            source.set_error("url.url_validator", e.responseText)

  mark_by_click_on_count_button: (_, id) ->
    id = parseInt(id, 10)
    source = Sources.takeFirst (s) -> s.id() == id
    if source
      ajax.mark_as_read(id)
      source.count(0)
    else
      logger.warn("source with id=#{id} not found")

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
          logger.warn("source not found with normalized: '#{normalized}' from '#{url}'")

    event.preventDefault()

  mark_all: (event) ->
    ajax.mark_all_as_read()
    Sources.all().forEach (s) -> s.count(0)
    return
