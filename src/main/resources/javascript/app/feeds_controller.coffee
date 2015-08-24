

FeedsController =
  favorite: (e, f) ->
    $.ajax
      url: "/api/v1/feeds/mark/#{f}"
      type: "PUT"
      success: (response) ->
        c(response)

  unfavorite: (e, f) ->
    $.ajax
      url: "/api/v1/feeds/unmark/#{f}"
      type: "PUT"
      success: (response) ->
        c(response)

  show: (source_name, feed_name) ->
    source = Sources.takeFirst (s) -> s.normalized() == source_name
    if source
      feeds = source.feed().filter (feed) -> feed.normalized() == feed_name
      if feeds.length > 0
        $.ajax
          url: "/api/v1/feeds/#{feeds[0].id()}"
          type: "GET"
          success: (feed) ->
            feed = new Feed(feed)
            result = Templates.feed_template.render({feed: feed})
            Templates.article_view.render(result).html()

