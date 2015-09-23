
SourcesController =

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
      ajax.get_feeds source.id(), (feeds) ->
        source.reset('feed')
        feeds = feeds.map (f) ->
          feed = new Feed(f)
          source.add_feed(feed)
          feed

        result = Templates.feeds_template.render({feeds: feeds})
        Templates.feeds_view.render(result).html()
        if feeds.length > 0
          redirect(feeds[0].href())

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

      #err = $("#{el} td span.errors")

      #view. input[type='button']").on "click", (e) ->
      #  (s) ->
          #$("#{el} td > span").toggleClass("uk-hidden")
          #$("#{el} td input").toggleClass("uk-hidden")
          #unless err.hasClass("uk-hidden")
          #  err.addClass("uk-hidden")
          #$("#{el} span.source-name").text(s.name)
          #$("#{el} span.source-url").html("<a href='#{s.url}'>#{s.url}</a>")
          #$("#{el} span.source-interval").text(s.interval)
      #  (e) ->
          #err
          #.removeClass("uk-hidden")
          #.text(e.responseText)
          #.css({'color': 'red'})










