
FeedsController =

  favorites: () ->
    ajax.favorites_feed (response) ->
      # extract sources
      feeds = response.map (f) ->
        feed = new Feed(f)
        new FavoriteFeed(feed)

      html = Templates.favorites_template.render({feeds: feeds.group_by('source_name')})
      Templates.article_view.render(html).html()
      state.to(States.Favorites)

  _favorite_helper: (id, is_favorite) =>
    v = new Sirius.View("a[data-feed-id='#{id}']")
    klasses = ["favorite", "unfavorite"]
    [remove, add] = if is_favorite
      klasses
    else
      klasses.reverse()

    v.render(remove).remove_class()
    v.render(add).add_class()

    # mark feed in collection as favorite or unmark
    if !sources.is_empty()
      source = Sources.takeFirst (s) -> s.id() == sources.get()
      if source && !posts.is_empty()
        feed = source.feeds().filter (f) -> f.id() == posts.get()
        if feed && feed.length > 0
          feed[0].favorite(is_favorite)

  favorite: (e, id) ->
    ajax.set_favorite id, (response) =>
      logger.info("Feed #{id} mark as favorite")
      @_favorite_helper(id, true)

  unfavorite: (e, id) ->
    ajax.unset_favorite id, (response) =>
      logger.info("Feed #{id} remove from favorite list")
      @_favorite_helper(id, false)

  view0: (e, id) -> # helper, if feeds have not uniq name need check it
    posts.set(id)


  _display_feed: (source, original_feed, feed) ->

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
      logger.warn("Source not found #{source_name}")

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



