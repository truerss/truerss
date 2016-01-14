
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
          result = Templates.feeds_template.render({feeds: source.feed()})
          Templates.feeds_view.render(result).html()
          redirect(source.feed()[0].href())
        else
          ajax.get_feeds source.id(), (feeds) ->
            feeds = feeds.map (x) ->
              pd = moment(x['publishedDate'])
              x['publishedDate'] = pd
              f = new Feed(x)
              source.add_feed(f)
              f

            result = Templates.feeds_template.render({feeds: source.feed()})
            Templates.feeds_view.render(result).html()
            if feeds.length > 0
              redirect(source.feed()[0].href())

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


  mark_all: (event) ->
    unless !state.hasState(States.Source)
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
