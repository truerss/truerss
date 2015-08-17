
class Source extends Sirius.BaseModel
  @attrs: ["id", "url", "name", "interval", "normalized", "lastUpdate"]
  @skip : true
  count: 0


Sources = new Sirius.Collection(Source)
Sources.subscribe "add", (source) ->
  template = Templates.source_list.render({source: source})
  c(template)


