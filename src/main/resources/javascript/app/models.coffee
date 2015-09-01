
class Source extends Sirius.BaseModel
  @attrs: ["id", "url", "name", {"interval" : 1}, "normalized",
    "lastUpdate", "count"]

  @has_many: ["feed"]
  @skip : true
  @validate:
    url: url_validator : true

  href: () ->
    "/show/#{@normalized()}"

  ajaxify: () ->
    JSON.stringify({url: @url(), interval: parseInt(@interval()), name: @name()})


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
  # TODO custom strategy : hide
  source.bind(source_view,
    "span.source-count":
      from: "count"
      transform: (x) ->
        if isNaN(parseInt(x, 10)) || x <= 0
          "0"
        else
          "#{x}"
  )
  return



