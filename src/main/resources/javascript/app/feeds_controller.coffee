
FeedsController =

  logger: Sirius.Application.get_logger("FeedsController")

  favorites: () ->
    ajax.favorites_feed (response) ->
      # extract sources
      feeds = response.map (f) ->
        feed = new Feed(f)
        new FavoriteFeed(feed)

      html = Templates.favorites_template.render({feeds: feeds})
      Templates.article_view.render(html).html()
      state.to(States.Favorites)

  _change: (view, has_flag, available_classes, feed_id, source_id, f) =>
    feed_id = parseInt(feed_id, 10)
    source_id = parseInt(source_id, 10)
    [remove, add] = if has_flag
      available_classes
    else
      available_classes.reverse()

    view.render(remove).remove_class()
    view.render(add).add_class()

    source = Sources.takeFirst (s) -> s.id() == source_id
    if source?
      feed = source.feeds().filter (f) -> f.id() == feed_id
      if feed? && feed.length > 0
        f(feed[0], source)

  _favorite_helper: (id, source_id, is_favorite) ->
    @_change(new Sirius.View("a[data-feed-id='#{id}'].in-favorites"),
      is_favorite, ["favorite", "unfavorite"], id, source_id,
        (feed, source) ->
            current = source.favorites_count()
            count = if is_favorite
              current + 1
            else
              current - 1
            source.favorites_count(count)
            feed.favorite(is_favorite))


  _read_helper: (id, source_id, is_read) ->
    @_change(new Sirius.View("a[data-feed-id='#{id}'].in-read"), is_read,
      ["read", "unread"], id, source_id,
      (feed, source) ->
          current = source.count()
          count = if is_read
            current - 1
          else
            current + 1
          source.count(count)
          feed.read(is_read)
    )


  read: (e, id, source_id) ->
    ajax.set_read id, (response) =>
      @logger.info("Feed #{id} mark as read")
      @_read_helper(id, source_id, true)

  unread: (e, id, source_id) ->
    ajax.set_unread id, (response) =>
      @logger.info("Feed #{id} mark as unread")
      @_read_helper(id, source_id, false)

  favorite: (e, id, source_id) ->
    ajax.set_favorite id, (response) =>
      @logger.info("Feed #{id} mark as favorite")
      @_favorite_helper(id, source_id, true)

  unfavorite: (e, id, source_id) ->
    ajax.unset_favorite id, (response) =>
      @logger.info("Feed #{id} remove from favorite list")
      @_favorite_helper(id, source_id, false)

  view_content: (feed_id) ->
    ajax.get_feed_content feed_id, (response) ->
      console.log(response)

  view0: (e, id) -> # helper, if feeds have not uniq name need check it
    posts.set(id)


  _display_feed: (source, original_feed, feed) ->

    feed = new Feed(feed)

    try
      result = Templates.feed_template.render({feed: feed})
      Templates.article_view.render(result).html()
    catch error
      @logger.error("error when insert feed #{error}")

    original_feed.merge(feed)
    unless feed.read()
      ajax.set_read feed.id(), (x) ->
        original_feed.read(true)
        feed.read(true)
        source.count(source.count() - 1)
    state.to(States.Feed)
    posts.set(original_feed.id())

  show: (source_name, feed_name) ->
    source_name = decodeURIComponent(source_name)
    feed_name = decodeURIComponent(feed_name)

    source = Sources.takeFirst (s) -> s.normalized() == source_name
    if source
      name = decodeURI(feed_name)
      finder = (source, name) ->
        source.feeds().filter (feed) -> feed.normalized() == name
      feeds = if !posts.is_empty()
        cf = posts.get()
        xs = source.feeds().filter (feed) -> feed.id() == parseInt(cf)
        if xs.length == 0
          finder(source, name)
        else
          xs
      else
        finder(source, name)
      if feeds.length > 0
        self = @
        original_feed = feeds[0]
        ajax.show_feed feeds[0].id(),
          (feed) ->
            self._display_feed(source, original_feed, feed)

          (error) ->
            self._display_feed(source, original_feed, JSON.parse(original_feed.to_json()))
            UIkit.notify
              message : error.responseText,
              status  : 'danger',
              timeout : 4000,
              pos     : 'top-right'
    else
      @logger.warn("Source not found #{source_name}")

  draw_tooltip: (e, source_id) ->
    # seems tippy does not work with uikit
    ew = $('.uk-offcanvas-bar-show').width()
    id = e.target.id

    if id

      span = document.getElementById(id)
      source_id = parseInt(source_id)
      source = Sources.takeFirst (s) -> s.id() == source_id
      # because tippy does not have properly flow
      tippy = new Tippy(".tippy-count", {
        position: 'right'
        distance: -1 * ew + 10
        arrow: true
        hideDelay: 2500
        theme: 'light'
        interactive: false
      })

      result = Templates.tippy_template
        .render({feeds: source.unread_feeds().slice(0, 25)})
      span.title = result

      popper = tippy.getPopperElement(span)

      tippy.update(popper)



