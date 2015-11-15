
class Source extends Sirius.BaseModel
  @attrs: ["id", "url", "name", {"interval" : 1}, "state", "normalized",
    "lastUpdate", "count"]

  @has_many: ["feed"]
  @skip : true
  @validate:
    url: url_validator : true

  is_disable: () ->
    parseInt(@state()) is 2

  withPlugin: () ->
    (parseInt(@state()) is 1) || (parseInt(@state()) is 2)

  href: () ->
    "/show/#{@normalized()}"

  href0: () ->
    "/by/#{@normalized()}"

  ajaxify: () ->
    JSON.stringify({url: @url(), interval: parseInt(@interval()), name: @name()})

  compare: (other) -> @id() == other.id()

  feed_sort: () ->
    @_feed = @_feed.sort (feed0, feed1) ->
      feed0.published_date().isAfter(feed1.published_date()) ? 1 : -1
    @_feed


class Feed extends Sirius.BaseModel
  @attrs: ["id", "sourceId", "url",
           "title", "author", "publishedDate",
           "description", "content", "normalized",
           "favorite", "read", "delete"]

  @belongs_to: [{model: "source",  back: "id", compose: (model, back) -> "#{model}Id"}]

  @skip: true

  href: () ->
    url = @source().href()
    "#{url}/#{@normalized()}"

  source: () ->
      source_id = @source_id()
      @_source ?= Sources.takeFirst((s) -> s.id() == source_id)
      @_source

  source_name: () -> @source().name()
  source_url : () -> @source().url()

  is_read: () ->
    @read()

  merge: (another_feed) ->
    @description(another_feed.description())
    @content(another_feed.content())
    @favorite(another_feed.favorite())

  anything: () ->
    if @content()
      @content()
    else if @description()
      @description()
    else
      "<div>impossible extract content <a href='#{@url()}'>#{@title()}</a></div>"


Sources = new Sirius.Collection(Source, {index: ['id', 'name', 'normalized']})
Sources.subscribe "add", (source) ->
  html = Templates.source_list.render({source: source})
  Templates.source_list_view.render(html).prepend()
  source_view = new Sirius.View("#source-#{source.id()}")

  source.bind(source_view,
    "a.source-url":
      [{
        from: "normalized"
        to: "href"
        transform: (x) ->
          "/show/#{x}"},
      {
        from: "name"
        to: "text"
      }]


    "span.source-count":
      from: "count"
      transform: (x) ->
        if isNaN(parseInt(x, 10)) || x <= 0
          "0"
        else
          "#{x}"
      strategy: "hide"
  )

  return

Sources.subscribe "remove", (source) ->
  # TODO unbind
  jQuery("#source-#{source.id()}").remove()



