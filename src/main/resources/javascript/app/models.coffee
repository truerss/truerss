
class Source extends Sirius.BaseModel
  @attrs: ["id", "url", "name", {"interval" : 1}, "state", "normalized",
    "lastUpdate", "count", {"active": true}, {"feeds": []}, {"favorites_count": 0}]

  @skip : true
  @validate:
    url: url_validator : true

  last_update_in_tz: () ->
    moment.utc(@last_update()).local()

  is_active: () ->
    active() is true

  is_plugin_disabled: () ->
    parseInt(@state()) is 2

  is_empty: () ->
    @count() == 0

  has_plugin: () ->
    (parseInt(@state()) is 1) || (parseInt(@state()) is 2)

  href: () ->
    "/show/sources/#{@normalized()}"

  ajaxify: () ->
    JSON.stringify({url: @url(), interval: parseInt(@interval()), name: @name(), id: @id()})

  compare: (other) -> @id() == other.id()

  add_feeds: (feeds) ->
    @feeds((@feeds() || []).concat(feeds))
    unread = feeds.filter((x) -> !x.read()).length
    @count(unread)
    @favorites_count(@feeds().filter((x) -> x.favorite()).length)

  add_feed: (feed) ->
    @add_feeds([feed])

  has_feeds: () ->
    (@feeds() || []).length != 0

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

  is_read: () ->
    @read()

  merge: (another_feed) ->
    @description(another_feed.description())
    @content(another_feed.content())
    @favorite(another_feed.favorite())

  @create: (json) ->
    json['publishedDate'] = moment.utc(json['publishedDate']).local()
    new Feed(json)

class Setting extends Sirius.BaseModel
  @attrs: ["key", "description", "value", "options"]

  @skip: true

  is_radio: () ->
    @options()['type'] == 'radio'

  is_eq: (x) ->
    x == @value()

  is_feeds_per_page: () ->
    @key() == "feeds_per_page"

  ajaxify: () ->
    JSON.stringify({key: @key(), value: @value()})


Settings = new Sirius.Collection(Setting, {index: ["key"]})

Sources = new Sirius.Collection(Source, {index: ['id', 'name', 'normalized']})

Sources.subscribe "add", (source) ->
  id = "#source-#{source.id()}"
  html = Templates.source_list.render({source: source})
  Templates.source_list_view.render(html).prepend()
  source_view = new Sirius.View(id)

  Sirius.Materializer.build(source, source_view)
    .field((x) -> x.normalized)
    .to((v) -> v.zoom("a.source-url"))
    .transform((x) -> "/show/sources/#{x}")
    .handle((view, value) -> view.render(value).swap("href"))
    .field((x) -> x.active)
    .to((v) -> v)
    .handle((view, value) ->
      if value
        view.render("uk-hidden").remove_class()
      else
        view.render("uk-hidden").add_class()
    )
    .field((x) -> x.count)
    .to((v) -> v.zoom("a.source-count"))
    .transform((x) ->
      x = parseInt(x, 10)
      if isNaN(x) or x <= 0
        "-"
      else
        "#{x}"
    )
    .handle((view, value) -> view.render(value).toggle())
    .run()

  return

Sources.subscribe "add", (source) ->
  SourcesController.change_count(source.count())

Sources.subscribe "remove", (source) ->
  jQuery("#source-#{source.id()}").remove()


