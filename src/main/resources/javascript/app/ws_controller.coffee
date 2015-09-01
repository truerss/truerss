
WSController =
  create: (e, source) ->
    Sources.add(new Source(JSON.parse(source)))

  fresh: (e, xs) ->
    feeds = JSON.parse(xs).map (x) -> new Feed(x)
    if feeds.length > 0
      source = Sources.takeFirst (s) -> s.id() == feeds[0].source_id()
      feeds.forEach (f) -> source.add_feed(f)
      source.count(source.count() + feeds.length)
      # TODO redirect ?
      if source
        UIkit.notify
          message : "Given #{feeds.length} feeds, from #{source.name()}"
          status  : 'success',
          timeout : 3000,
          pos     : 'top-right'
