
class Source extends Sirius.BaseModel
  @attrs: ["id", "url", "name", {"interval" : 1}, "state", "normalized",
    "lastUpdate", "count", {"active": true}, {"favorites_count": 0}, "errorsCount"]

  @skip : true
  @validate:
    url: url_validator : true

  last_update_in_tz: () ->
    moment.utc(@last_update()).local()

  is_empty: () ->
    @count() == 0

  is_active: () ->
    active() is true

  is_plugin_disabled: () ->
    parseInt(@state()) is 2

  has_plugin: () ->
    (parseInt(@state()) is 1) || (parseInt(@state()) is 2)

  href: () ->
    "/show/sources/#{@normalized()}"

  href_all: () ->
    "#{@href()}/all"

  has_errors: () ->
    parseInt(@errors_count()) > 0

  errors_message: () ->
    if parseInt(@errors_count()) == 1
      "Has 1 Error"
    else
      "Has #{@errors_count()} Errors"

  ajaxify: () ->
    JSON.stringify({url: @url(), interval: parseInt(@interval()), name: @name(), id: @id()})

  compare: (other) -> @id() == other.id()

class SourceOverview extends Sirius.BaseModel
  @attrs: ["id",
    {"unread_count": 0}, {"favorites_count": 0},
    {"feeds_count": 0}, "frequency", {"is_loaded_all": false}]

  @create: (json) ->
    new SourceOverview({
      id: json["sourceId"],
      unread_count: json["unreadCount"],
      favorites_count: json["favoritesCount"],
      feeds_count: json["feedsCount"],
      frequency: json["frequency"]
    })

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

  is_short_view: () ->
    @key() == "short_view"

  to_object: () ->
    {key: @key(), value: @value()}


Settings = new Sirius.Collection(Setting, {index: ["key"]})

Sources = new Sirius.Collection(Source, {index: ['id', 'name', 'normalized']})

SourceOverviews = new Sirius.Collection(SourceOverview, {index: ["id"]})

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
  ControllerExt.change_count(source.count())

Sources.subscribe "remove", (source) ->
  jQuery("#source-#{source.id()}").remove()


