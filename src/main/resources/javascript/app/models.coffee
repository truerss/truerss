
class Source extends Sirius.BaseModel
  @attrs: ["id", "url", "name", {"interval" : 1}, "state", "normalized",
    "lastUpdate", "count", {"feeds": []}]

  @skip : true
  @validate:
    url: url_validator : true

  is_disable: () ->
    parseInt(@state()) is 2

  is_empty: () ->
    @count() == 0

  withPlugin: () ->
    (parseInt(@state()) is 1) || (parseInt(@state()) is 2)

  href: () ->
    "/show/#{@normalized()}"

  href0: () ->
    "/by/#{@normalized()}"

  ajaxify: () ->
    JSON.stringify({url: @url(), interval: parseInt(@interval()), name: @name(), id: @id()})

  compare: (other) -> @id() == other.id()

  add_feed: (feed) ->
    tmp = @feeds()
    tmp.push(feed)
    @feeds(tmp)

  unread_feeds: () ->
    @feeds().filter (f) -> !f.read()



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
    @source_name = feed.source().name()


Sources = new Sirius.Collection(Source, {index: ['id', 'name', 'normalized']})
Sources.subscribe "add", (source) ->
  html = Templates.source_list.render({source: source})
  Templates.source_list_view.render(html).prepend()
  source_view = new Sirius.View("#source-#{source.id()}")

  transformer = Sirius.Transformer.draw({
    "normalized": {
      to: 'a.source-url'
      attr: 'href'
      via: (new_value, selector, view, attribute) ->
        view.zoom(selector).render("/show/#{new_value}").swap(attribute)
    },
    "name": {
      to: 'a.source-url'
    },
    "count": {
      to: 'a.source-count'
      via: (new_value, selector, view, attribute) ->
        x = parseInt(new_value, 10)
        count = if isNaN(x) or x <= 0
            "0"
          else
            "#{x}"
        view.zoom(selector).render(count).toggle()
    }
  })

  source.bind(source_view, transformer)


  return

Sources.subscribe "add", (source) ->
  Sirius.Application.get_adapter().and_then (adapter) ->
    if source.count() > 0
      adapter.fire(document, "sources:fetch", source)

Sources.subscribe "remove", (source) ->
  # TODO unbind
  jQuery("#source-#{source.id()}").remove()


