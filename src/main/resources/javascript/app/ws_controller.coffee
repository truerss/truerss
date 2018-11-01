
WSController =
  # new feeds
  fresh: (e, xs) ->
    feeds = JSON.parse(xs).map (x) -> new Feed(x)
    if feeds.length > 0
      source = Sources.takeFirst (s) -> s.id() == feeds[0].source_id()
      feeds.forEach (f) -> source.add_feed(f)
      source.count(source.count() + feeds.length)

      if source
        if location.pathname == "" || location.pathname == "/"
          redirect(source.href())
        else
          if state.isState(States.Source) || state.isState(States.Feed)
            if sources && sources.get() && sources.get() == source.id()
              render_source_feeds_and_redirect_to_first(source)


        UIkit.notify
          message : "Given #{feeds.length} feeds, from #{source.name()}"
          status  : 'success',
          timeout : 3000,
          pos     : 'top-right'

  notify: (e, msg) ->
    obj = JSON.parse(msg) # level: lvl, message: msg
    UIkit.notify
      message : obj.message
      status  : obj.level,
      timeout : 3000,
      pos     : 'top-right'
