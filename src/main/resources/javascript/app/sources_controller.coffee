
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
          c(feeds)

  refresh_one: (e, id) ->
    $.ajax
      url: "/api/v1/sources/refresh/#{id}"
      method: "PUT"
      success: () ->






