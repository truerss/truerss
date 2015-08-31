
WSController =
  create: (e, body) ->
    source = new Source(JSON.parse(body))
    Sources.add(source)
