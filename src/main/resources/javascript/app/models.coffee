
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


