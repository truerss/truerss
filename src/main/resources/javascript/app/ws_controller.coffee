
WSController =
  create: (e, source) ->
    Sources.add(new Source(JSON.parse(source)))

  fresh: (e, xs) ->
    feeds = JSON.parse(xs).map (x) -> new Feed(x)
    if feeds.length > 0
      source = Sources.takeFirst (s) -> s.id() == feeds[0].source_id()
      feeds.forEach (f) -> source.add_feed(f)
      source.count(source.count() + feeds.length)

      if source
        if location.pathname == "" || location.pathname == "/"
          redirect(source.href())


        UIkit.notify
          message : "Given #{feeds.length} feeds, from #{source.name()}"
          status  : 'success',
          timeout : 3000,
          pos     : 'top-right'

  deleted: (e, source) ->
    source = new Source(JSON.parse(source))
    Sources.remove(source)
    jQuery("#all-sources tr.source-#{source.id()}").remove()

  updated: (e, source) ->
    logger.info("ws update #{source}")
    obj = JSON.parse(source)
    need = Sources.find('id', obj.id)
    if need
      need.url(obj.url)
      need.name(obj.name)
      need.interval(obj.interval)
      need.normalized(obj.normalized)
      need.last_update(need.lastUpdate)

  notify: (e, msg) ->
    obj = JSON.parse(msg) # level: lvl, message: msg
    position = if obj.level == 'success'
      'pos-right'
    else
      'pos-center'
    UIkit.notify
      message : obj.message
      status  : obj.level,
      timeout : 3000,
      pos     : 'top-right'
