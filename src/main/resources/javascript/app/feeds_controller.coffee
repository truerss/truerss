
FeedsController =
  _current_feed: null

  view: (normalized) ->
    normalized = decodeURIComponent(normalized)
    source = Sources.takeFirst (s) -> s.normalized() == normalized
    if source
      # FIXME is it necessary?
      ajax.get_feeds source.id(), (feeds) ->
        source.reset('feed')
        feeds = feeds.map (f) ->
          feed = new Feed(f)
          source.add_feed(feed)
          feed

        result = Templates.feeds_list.render({feeds: feeds})
        Templates.article_view.render(result).html()


  favorites: () ->
    ajax.favorites_feed (response) ->
      feeds = response.map (f) -> new Feed(f)
      html = Templates.favorites_template.render({feeds: feeds})
      Templates.article_view.render(html).html()
      state.to(States.Favorites)

  _favorite_helper: (id, favorite) =>
    # TODO binding
    v = new Sirius.View("i[data-favorite='#{id}']")
    klasses = [["uk-icon-star-o", "favorite"], ["uk-icon-star", "unfavorite"]]
    [remove, add] = if favorite
      klasses
    else
      klasses.reverse()

    remove.forEach (klass) -> v.render(klass).remove_class()
    add.forEach (klass) -> v.render(klass).add_class()

  favorite: (e, f) ->
    ajax.set_favorite f, (response) =>
      logger.info("#{f} mark as favorite feed")
      @_favorite_helper(f, true)

  unfavorite: (e, f) ->
    ajax.unset_favorite f, (response) =>
      logger.info("#{f} remove from favorite list")
      @_favorite_helper(f, false)

  view0: (e, id) -> # helper, if feeds have not uniq name need check it
    @_current_feed = id

  show: (source_name, feed_name) ->
    source = Sources.takeFirst (s) -> s.normalized() == source_name
    if source
      feeds = if @_current_feed
        cf = @_current_feed
        source.feed().filter (feed) -> feed.id() == parseInt(cf)
      else
        source.feed().filter (feed) -> feed.normalized() == decodeURI(feed_name)

      if feeds.length > 0
        original_feed = feeds[0]
        ajax.show_feed feeds[0].id(),
          (feed) ->
            feed = new Feed(feed)
            try
              result = Templates.feed_template.render({feed: feed})
              Templates.article_view.render(result).html()
            catch error
              logger.error("error when insert feed #{error}")

            original_feed.merge(feed)
            unless feed.read()
              ajax.set_read feed.id(), (x) ->
                original_feed.read(true)
                feed.read(true)
                source.count(source.count() - 1)
          (error) ->
            UIkit.notify
              message : error.responseText,
              status  : 'danger',
              timeout : 4000,
              pos     : 'top-center'



