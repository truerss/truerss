

FeedsController =
  favorites: () ->
    $.ajax
      url: "/api/v1/feeds/favorites"
      type: "GET"
      success: (response) ->
        feeds = response.map (f) -> new Feed(f)
        html = Templates.favorites_template.render({feeds: feeds})
        Templates.article_view.render(html).html()
        state.to(States.Favorites)


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

            unless feed.read()
              $.ajax
                url: "/api/v1/feeds/read/#{feed.id()}"
                type: "PUT"
                success: (x) ->
                  feed.read(true)
                  source.count(source.count() - 1)


