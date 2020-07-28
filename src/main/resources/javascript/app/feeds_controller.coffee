
FeedsController =

  logger: Sirius.Application.get_logger("FeedsController")

  favorites: (page) ->
    page = parseInt(page || 1)
    offset = get_offset(page)
    limit = get_limit()

    ajax.favorites_feed(offset, limit, (feeds, total) ->
      render_feeds(feeds, page, total, "/favorites")
    )

  _change: (view, has_flag, available_classes, feed_id, source_id, f) =>
    feed_id = parseInt(feed_id, 10)
    source_id = parseInt(source_id, 10)
    [remove, add] = if has_flag
      available_classes
    else
      available_classes.reverse()

    view.render(remove).remove_class()
    view.render(add).add_class()
    source = Sources.find('id', source_id)
    get_source_overview(source_id).then((overview) ->
      f(overview, source)
    )

  _favorite_helper: (id, source_id, is_favorite) ->
    @_change(new Sirius.View("a[data-feed-id='#{id}'].in-favorites"),
      is_favorite, ["favorite", "unfavorite"], id, source_id,
        (source_overview, _) ->
            current = source_overview.favorites_count()
            count = if is_favorite
              current + 1
            else
              current - 1
            source_overview.favorites_count(count)
    )

  _read_helper: (id, source_id, is_read) ->
    @_change(new Sirius.View("a[data-feed-id='#{id}'].in-read"), is_read,
      ["read", "unread"], id, source_id,
      (source_overview, source) ->
          current = source_overview.unread_count()
          count = if is_read
            current - 1
          else
            current + 1
          source.count(count)
          source_overview.unread_count(count)
    )


  read: (e, id, source_id) ->
    ajax.set_read id, (response) =>
      @logger.info("Feed #{id} mark as read")
      @_read_helper(id, source_id, true)
    change_count(-1)
    e.preventDefault()

  unread: (e, id, source_id) ->
    ajax.set_unread id, (response) =>
      @logger.info("Feed #{id} mark as unread")
      @_read_helper(id, source_id, false)
    change_count(1)
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
          if next.length != 0
            next.removeClass("mt3")
        el.next("hr").remove()
        el.remove()


    e.preventDefault()

  view_content: (feed_id) ->
    ajax.get_feed_content feed_id, (response) ->
      Templates.article_view.render(response.content).html()


