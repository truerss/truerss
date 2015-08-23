
class Source extends Sirius.BaseModel
  @attrs: ["id", "url", "name", {"interval" : 1}, "normalized", "lastUpdate"]
  @skip : true
  @validate:
    url: url_validator : true
  count: 0
  href: () ->
    "/show/#{@normalized()}"

  ajaxify: () ->
    JSON.stringify({url: @url(), interval: parseInt(@interval()), name: @name()})



Sources = new Sirius.Collection(Source, {index: ['id', 'name', 'normalized']})
Sources.subscribe "add", (source) ->
  html = Templates.source_list.render({source: source})
  Templates.source_list_view.render(html).prepend()
  source_view = new Sirius.View("#source-#{source.id()}")
  source_count_view = new Sirius.View("#source-#{source.id()} span.uk-badge")
  # TODO custom strategy : hide
  source_count_view.bind(source, 'count',
    transform: (x) ->
      if isNaN(parseInt(x, 10)) || x <= 0
        "0"
      else
        "#{x}"
  )



