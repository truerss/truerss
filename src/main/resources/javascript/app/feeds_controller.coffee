
FeedsController =

  logger: Sirius.Application.get_logger("FeedsController")

  favorites: (page) ->
    page = parseInt(page || 1)
    ajax.favorites_feed (response) ->
      # extract sources
      feeds = response.map (f) -> Feed.create(f)

      render_feeds(feeds, page, "/favorites")

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
    e.preventDefault()

  unread: (e, id, source_id) ->
    ajax.set_unread id, (response) =>
      @logger.info("Feed #{id} mark as unread")
      @_read_helper(id, source_id, false)
    e.preventDefault()

  favorite: (e, id, source_id) ->
    ajax.set_favorite id, (response) =>
      @logger.info("Feed #{id} mark as favorite")
      @_favorite_helper(id, source_id, true)
    e.preventDefault()

  unfavorite: (e, id, source_id) ->
    ajax.unset_favorite id, (response) =>
      @logger.info("Feed #{id} remove from favorite list")
      @_favorite_helper(id, source_id, false)

      if is_favorite()
        el = $("div.feeds[data-feed-id='#{id}']")
        prev = el.prev()
        if prev? && prev.hasClass("mt3")
          prev.removeClass("mt3")
        if prev.length == 0
          next = el.next("hr").next("div.feeds")
          c next
          if next.length != 0
            next.removeClass("mt3")
        el.next("hr").remove()
        el.remove()


    e.preventDefault()

  view_content: (feed_id) ->
    ajax.get_feed_content feed_id, (response) ->
      Templates.article_view.render(response.content).html()

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



