
FeedsController =
  favorites: () ->
    ajax.favorites_feed (response) ->
      feeds = response.map (f) -> new Feed(f)
      html = Templates.favorites_template.render({feeds: feeds})
      Templates.article_view.render(html).html()
      state.to(States.Favorites)

  favorite: (e, f) ->
    ajax.set_favorite f, (response) ->
      logger.info("#{f} mark as favorite feed")

  unfavorite: (e, f) ->
    ajax.unset_favorite f, (response) ->
      logger.info("#{f} remove from favorite list")

  show: (source_name, feed_name) ->
    source = Sources.takeFirst (s) -> s.normalized() == source_name
    if source
      feeds = source.feed().filter (feed) -> feed.normalized() == feed_name
      if feeds.length > 0
        ajax.show_feed feeds[0].id(), (feed) ->
          feed = new Feed(feed)
          result = Templates.feed_template.render({feed: feed})
          Templates.article_view.render(result).html()

          unless feed.read()
            ajax.set_read feed.id(), (x) ->
              feed.read(true)
              source.count(source.count() - 1)


