
class Source extends Sirius.BaseModel
  @attrs: ["id", "url", "name", {"interval" : 1}, "state", "normalized",
    "lastUpdate", "count", {"feeds": []}, {"favorites_count": 0}]


  @skip : true
  @validate:
    url: url_validator : true

  is_disable: () ->
    parseInt(@state()) is 2

  is_empty: () ->
    @count() == 0

  has_plugin: () ->
    (parseInt(@state()) is 1) || (parseInt(@state()) is 2)

  href: () ->
    "/show/#{@normalized()}"

  ajaxify: () ->
    JSON.stringify({url: @url(), interval: parseInt(@interval()), name: @name(), id: @id()})

  compare: (other) -> @id() == other.id()

  add_feeds: (feeds) ->
    @feeds((@feeds() || []).concat(feeds))
    @favorites_count(@feeds().filter((x) -> x.favorite()).length)

  add_feed: (feed) ->
    @add_feeds([feed])

  unread_feeds: () ->
    @feeds().filter (f) -> !f.read()

  feeds_count: () ->
    @feeds().length


class Feed extends Sirius.BaseModel
  @attrs: ["id", "sourceId", "url",
           "title", "author", "publishedDate",
           "description", "content", "normalized",
           "favorite", "read", "delete"]

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

  @create: (json) ->
    json['publishedDate'] = moment(json['publishedDate'])
    new Feed(json)

  anything: () ->
    if @content()
      @content()
    else if @description()
      @description()
    else
      "<div>impossible extract content <a href='#{@url()}'>#{@title()}</a></div>"

# just a wrapper on Feed + Source Name
class FavoriteFeed
  constructor: (feed) ->
    @id = feed.id()
    @url = feed.url()
    @title = feed.title()
    @description = feed.description()
    @href = feed.href()
    @favorite = feed.favorite()
    @source_name = feed.source().name()

class Setting extends Sirius.BaseModel
  @attrs: ["key", "description", "value", "options"]

  @skip: true

  is_radio: () ->
    @options()['type'] == 'radio'

  is_eq: (x) ->
    x == @value()

  is_feeds_per_page: () ->
    @key() == "feeds_per_page"

Settings = new Sirius.Collection(Setting, {index: ["key"]})
Settings.subscribe("add", "settings:add")

Sources = new Sirius.Collection(Source, {index: ['id', 'name', 'normalized']})
Sources.subscribe "add", (source) ->
  html = Templates.source_list.render({source: source})
  Templates.source_list_view.render(html).prepend()
  source_view = new Sirius.View("#source-#{source.id()}")

  Sirius.Materializer.build(source, source_view)
    .field((x) -> x.normalized)
    .to((v) -> v.zoom("a.source-url"))
    .transform((x) -> "/show/#{x}")
    .handle((view, value) -> view.render(value).swap("href"))
    .field((x) -> x.count)
    .to((v) -> v.zoom("a.source-count"))
    .transform((x) ->
      x = parseInt(x, 10)
      if (isNaN(x)) or x <= 0
        "0"
      else
        "#{x}"
    )
    .handle((view, value) -> view.render(value).toggle())
    .run()

  return

Sources.subscribe "add", (source) ->
  Sirius.Application.get_adapter().and_then (adapter) ->
    if source.count() > 0
      adapter.fire(document, "sources:fetch", source)

Sources.subscribe "remove", (source) ->
  # TODO unbind
  jQuery("#source-#{source.id()}").remove()


