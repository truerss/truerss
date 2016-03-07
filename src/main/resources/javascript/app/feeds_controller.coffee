
FeedsController =

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
      # extract sources
      feeds = response.map (f) ->
        feed = new Feed(f)
        new FavoriteFeed(feed)

      html = Templates.favorites_template.render({feeds: feeds.group_by('source_name')})
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
    posts.set(id)

  show: (source_name, feed_name) ->
    source_name = decodeURIComponent(source_name)
    feed_name = decodeURIComponent(feed_name)

    source = Sources.takeFirst (s) -> s.normalized() == source_name
    if source
      name = decodeURI(feed_name)
      finder = (source, name) ->
        source.feed().filter (feed) -> feed.normalized() == name
      feeds = if !posts.is_empty()
        cf = posts.get()
        xs = source.feed().filter (feed) -> feed.id() == parseInt(cf)
        if xs.length == 0
          finder(source, name)
        else
          xs
      else
        finder(source, name)
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
            state.to(States.Feed)
            posts.set(original_feed.id())
          (error) ->
            UIkit.notify
              message : error.responseText,
              status  : 'danger',
              timeout : 4000,
              pos     : 'top-center'
    else
      logger.warn("Source not found #{source_name}")

  prev_next_guard: () -> state.hasState(States.Feed)

  prev: (e, feed_id) ->
    source_id = sources.get()
    source = Sources.takeFirst (s) -> s.id() == source_id
    if source
      arr = source.feed()
      if arr.length > 0
        index = 0
        for f in arr
          if f.id() == parseInt(feed_id)
            index = f.id()
        ch = jQuery("a[data-feed-id='#{index}']").parent().prev().children()
        if ch.length > 0
          new_feed_id = ch.attr('data-feed-id')
          posts.set(new_feed_id)
          redirect(ch.attr('href'))

    else
      logger.warn("Source not found #{source_id}")

  next: (e, feed_id) ->
    source_id = sources.get()
    source = Sources.takeFirst (s) -> s.id() == source_id
    if source
      arr = source.feed()
      if arr.length > 0
        index = 0
        for f in arr
          if f.id() == parseInt(feed_id)
            index = f.id()
        ch = jQuery("a[data-feed-id='#{index}']").parent().next().children()
        if ch.length > 0
          new_feed_id = ch.attr('data-feed-id')
          posts.set(new_feed_id)
          redirect(ch.attr('href'))

    else
      logger.warn("Source not found #{source_id}")

  check_key: (e) ->
    code = e.keyCode
    (code == 39 || code == 37) && state.hasState(States.Feed)

  move: (e) ->
    code = e.keyCode
    post = posts.get()
    if post
      if e.keyCode == 37 # prev
        @prev(e, post)
      else if e.keyCode == 39 # next
        @next(e, post)
      else
        # skip

