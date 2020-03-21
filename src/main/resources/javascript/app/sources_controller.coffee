
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
    logger.info("Show: #{normalized} source is exist? #{source != null}")

    if source
      ajax.get_unread source.id(), (feeds) ->
        logger.info("Load from source: #{source.id()}, #{feeds.length} feeds")
        source.reset('feeds')
        feeds = feeds.map (x) ->
          x['publishedDate'] = moment(x['publishedDate'])
          f = new Feed(x)
          source.add_feed(f)
          f

        if feeds.length > 0
          render_source_feeds_and_redirect_to_first(source)
        else
          ajax.get_feeds source.id(), (feeds) ->
            feeds = feeds.map (x) ->
              x['publishedDate'] = moment(x['publishedDate'])
              f = new Feed(x)
              source.add_feed(f)
              f

            render_source_feeds_and_redirect_to_first(source)

      state.to(States.Source)
      posts.clear()
      sources.set(source.id())

    else
      logger.warn "source: #{normalized} does not exist"

  refresh_one: (e, id) ->
    ajax.refresh_one id

  remove: (e, id) ->
    ajax.remove_source id
    source = Sources.find("id", id)
    if source
      Sources.remove(source)
      jQuery("#all-sources tr.source-#{id}").remove()


  edit: (e, id) ->
    logger.info("update #{id} source")
    source = Sources.find('id', id)
    if source
      el = "table tr.source-#{source.id()}"

      view = new Sirius.View(el)
      view.render("uk-hidden").zoom("td > span").add_class()
      view.render("uk-hidden").zoom("td input").remove_class()
      view.render("uk-hidden").zoom("span.errors").remove_class()

      to_view_transformer = Sirius.Transformer.draw({
        name: {
          to: 'span.source-name'
        },
        'url': {
          to: 'span.source-url'
        },
        'interval': {
          to: 'span.source-interval'
        },
        'errors.url.url_validator': {
          to: 'span.errors'
        }
      })

      source.bind(view, to_view_transformer)

      to_model_transformer = Sirius.Transformer.draw({
        "input[name='name']": {
          to: 'name'
        },
        "input[name='url']": {
          to: 'url'
        },
        "input[name='interval']": {
          to: 'interval'
        }
      })

      view.bind(source, to_model_transformer)
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
    logger.debug(123)
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
      feeds = feeds.map (x) ->
        pd = moment(x['publishedDate'])
        x['publishedDate'] = pd
        new Feed(x)

      source.reset('feeds')
      source.count(feeds.length)
      feeds.forEach (f) -> source.add_feed(f)
