
WSController =

  logger: Sirius.Application.get_logger("WSController")

  # new feeds
  fresh: (e, xs) ->
    feeds = xs.map (x) -> Feed.create(x)
    if !feeds.is_empty()
      source_id = feeds[0].source_id()
      source = Sources.find('id', source_id)

      if source
        source.count(source.count() + feeds.length)
        # push source to the top of list
        UIkit.notification
          message : "Received #{feeds.length} new feeds, from #{source.name()}"
          status  : 'success',
          timeout : 3000,
          pos     : 'top-right'

      else
        # todo load ?
        @logger.warn("Source #{source_id} was not found")

  notify: (e, obj) ->
    UIkit.notification
      message : obj.message
      status  : obj.level,
      timeout : 3000,
      pos     : 'top-right'
