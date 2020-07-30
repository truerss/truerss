
WSController =

  logger: Sirius.Application.get_logger("WSController")

  new_sources: (e, source) ->
    @logger.info("New source: #{JSON.stringify(source)}")
    Sources.add(new Source(source))

  # new feeds
  fresh: (e, xs) ->
    feeds = xs.map (x) -> Feed.create(x)
    if !feeds.is_empty()
      source_id = feeds[0].source_id()
      source = Sources.find('id', source_id)

      if source
        source.count(source.count() + feeds.length)
        if source.count() > 0
          # push to the top
          current_source = jQuery("#source-#{source_id}")
          first_element = jQuery("#{Templates.source_list_view.get_element()} .source-element").first()
          if current_source.attr('id') != first_element.attr('id')
            current_source
              .remove().clone(true)
              .insertBefore(first_element)

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
