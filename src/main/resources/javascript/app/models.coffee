
class Source extends Sirius.BaseModel
  @attrs: ["id", "url", "name", "interval", "normalized", "lastUpdate"]
  @skip : true
  count: 0
  href: () ->
    "/show/#{@normalized()}"


Sources = new Sirius.Collection(Source)
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



