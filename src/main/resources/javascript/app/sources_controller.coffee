
SourcesController =

  all: () ->
    html = Templates.all_sources_template.render({sources: Sources.all()})
    Templates.article_view.render(html).html()
    state.to(States.Sources)

  refresh_all: () ->
    $.ajax
      url: "/api/v1/sources/refresh"
      method: "PUT"

  show: (normalized) ->
    normalized = decodeURIComponent(normalized)
    source = Sources.takeFirst (s) -> s.normalized() == normalized
    if source
      $.ajax
        url: "/api/v1/sources/feeds/#{source.id()}"
        method: "GET"
        dataType: "json"
        success: (feeds) ->
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
    $.ajax
      url: "/api/v1/sources/refresh/#{id}"
      method: "PUT"
      success: () ->

  remove: (e, id) ->
    $.ajax
      url: "/api/v1/sources/#{id}"
      method: "DELETE"

  edit: (e, id) ->
    # TODO use sirius binding
    source = Sources.find('id', id)
    if source
      el = "table tr.source-#{source.id()}"
      $("#{el} td > span").addClass("uk-hidden")
      $("#{el} td input").removeClass("uk-hidden")
      $("#{el} td input[type='button']").on "click", (e) ->
        name = $(#{el} td input[name="name"]).val()
        url = $(#{el} td input[name="url"]).val()
        interval = $(#{el} td input[name="interval"]).val()








