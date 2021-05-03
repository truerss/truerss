
class AjaxService

  constructor: () ->
    logger = Sirius.Application.get_logger("AjaxService")
    @counter = 0
    @sources_api = "/api/v1/sources"
    @feeds_api = "/api/v1/feeds"
    @plugin_api = "/api/v1/plugins"
    @settings_api = "/api/v1/settings"
    @search_api = "/api/v1/search"
    @mark_api = "/api/v1/mark"
    @refresh_api = "/api/v1/refresh"
    @overview_api = "/api/v1/overview"
    @plugin_sources_api = "/api/v1/plugin-sources"
    @k = (err) ->
      if err # otherwise: 204 no content
        c(JSON.stringify(err))
        logger.warn(JSON.stringify(err))

  is_ready: () ->
    @counter == 0

  available_plugins: (success) ->
    @_get("#{@plugin_sources_api}", success, @k)

  remove_plugin: (url) ->
    params = JSON.stringify({
      url: url
    })
    @_post("#{@plugin_sources_api}/uninstall", params, @k, @k)

  install_plugin: (url) ->
    params = JSON.stringify({
      url: url
    })
    @_post("#{@plugin_sources_api}/install", params, @k, @k)

  plugins_all: (success, error) ->
    @_get("#{@plugin_api}/all", success, error)

  js_all: (success) ->
    @_get("#{@plugin_api}/js", success, @k)

  css_all: (success) ->
    @_get("#{@plugin_api}/css", success, @k)

  sources_all: (success, error) ->
    @_get(
      "#{@sources_api}/all",
      (response) -> success(response.map((x) -> new Source(x))),
      error
    )

  latest: (offset, limit, success) ->
    @_get("#{@sources_api}/latest?offset=#{offset}&limit=#{limit}",
      (r) => @_feeds_transform(r, success)
    )

  source_create: (params, success, error) ->
    @_post("#{@sources_api}", params, success, error)

  refresh_all: () ->
    @_put("#{@refresh_api}/all", @k , @k)

  get_page_of_feeds: (source_id, offset, limit, unread_only, success, error) ->
    @_get(
      "#{@sources_api}/#{source_id}/feeds?offset=#{offset}&limit=#{limit}&unreadOnly=#{unread_only}",
      (r) => @_feeds_transform(r, success),
      error
    )

  refresh_one: (num, success, error) ->
    @_put("#{@refresh_api}/#{num}", {}, @k, @k)

  remove_source: (num, success, error) ->
    @_delete("#{@sources_api}/#{num}", @k, @k)

  update_source: (id, params, success, error) ->
    @_put("#{@sources_api}/#{id}", params, success, error)

  favorites_feed: (offset, limit, success, error) ->
    @_get("#{@feeds_api}/favorites?offset=#{offset}&limit=#{limit}",
      (r) => @_feeds_transform(r, success),
      error)

  set_favorite: (num, success, error) ->
    @_put("#{@feeds_api}/mark/#{num}", {}, success, error)

  unset_favorite: (num, success, error) ->
    @_put("#{@feeds_api}/unmark/#{num}", {}, success, error)

  show_feed: (num, success, error) ->
    @_get("#{@feeds_api}/#{num}", success, error)

  set_read: (num, success) ->
    @_put("#{@feeds_api}/read/#{num}", {}, success, @k)

  set_unread: (num, success) ->
    @_put("#{@feeds_api}/unread/#{num}", {}, success, @k)

  about: (success) ->
    @_get("/about", success, @k)

  mark_as_read: (source_id) ->
    @_put("#{@mark_api}/#{source_id}", @k, @k)

  mark_all_as_read: (success) ->
    @_put("#{@mark_api}", {}, success, @k)

  get_source_overview: (sourceId, success) ->
    @_get(
      "#{@overview_api}/#{sourceId}",
      (response) -> success(SourceOverview.create(response)),
      @k)

  get_feed_content: (feedId, success) ->
    @_get("#{@feeds_api}/content/#{feedId}", success, @k)

  get_settings: (success) ->
    @_get(
      "#{@settings_api}/current",
      (response) -> success(response.map (x) -> new Setting(x))
      @k)

  search: (request, success) ->
    @_post(
      @search_api,
      request,
      (r) => @_feeds_transform(r, success),
      @k)

  update_settings: (params, success) ->
    @_put(
      "#{@settings_api}",
      params,
      success,
      @k)

  load_ejs: (url) ->
    jQuery.ajax
      type: "GET"
      url: "/templates/#{url}.ejs"

  _feeds_transform: (response, success_f) ->
    total = response["total"]
    xs = response["resources"]
    success_f(xs.map((x) -> Feed.create(x)), total)

  _delete: (url, success, error = @k ) ->
    $.ajax
      type: "DELETE"
      url: url
      success: success
      error : error

  _put: (url, params, success, error = @k) ->
    $.ajax
      url: url
      type: "PUT"
      dataType: "json" # json
      data: params
      success: success
      error: error

  _post: (url, params, success, error = @k) ->
    $.ajax
      url: url
      type: "POST"
      dataType: "json"
      data: params
      success: success
      error: error

  _get: (url, success, error = @k ) ->
    $.ajax
      beforeSend: () =>
        @counter = @counter + 1
      type: "GET"
      url: url
      success: success
      error : error
      complete: () =>
        @counter = @counter - 1


`
// source http://stackoverflow.com/a/5639455/1581531
(function(){
    var cookies;

    function readCookie(name,c,C,i){
        if(cookies){ return cookies[name]; }

        c = document.cookie.split('; ');
        cookies = {};

        for(i=c.length-1; i>=0; i--){
           C = c[i].split('=');
           cookies[C[0]] = C[1];
        }

        return cookies[name];
    }

    window.readCookie = readCookie; // or expose it however you want
})();
`



ControllerExt =

  _materializer: null

  ajax: new AjaxService()

  get_source_overview: (source_id) ->
    new Promise((resolve, reject) ->
      result = SourceOverviews.find('id', source_id)
      if result == null
        @ajax.get_source_overview(source_id,
          (result) ->
            SourceOverviews.add(result)
            resolve(result)
        )
      else
        resolve(result)
    )

  change_count: (count) ->
    Templates.sources_all_view
      .zoom("#source-count-all")
      .render(count)
      .sum()

  scroll_to_top: () ->
    $('html,body').animate({scrollTop: 0}, 1000);

  clean_route: () ->
    history.pushState("", document.title, window.location.pathname
      + window.location.search)

  read_cookie: window.readCookie

  delete_cookie: (cn) ->
    document.cookie = cn + '=; Path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT;'

  is_favorite: () ->
    window.location.href.endsWith("/favorites")

  _make_pagination: (length, per_page, current) ->
    tmp = parseInt(length / per_page, 10)
    additional = if per_page * tmp == length
      0
    else
      1
    page_count = additional + tmp

    next_page = current + 1
    prev_page = current - 1

    arr = [prev_page, current, next_page]
    arr.unshift(1)
    arr.push(page_count)
    arr = Array.from(new Set(arr))
    arr = arr.filter (x) -> x > 0 && x <= page_count

    results = []
    splitted = arr.each_cons(2)
    for [a, b] in splitted
      if b - a > 1
        results.add_to(a).add_to(-1).add_to(b)
      else
        results.add_to(a).add_to(b)
    results

  _is_short_view_enabled: () ->
    settings = Settings.takeFirst((x) -> x.is_short_view())
    if settings?
      false

    settings.value()

  _get_per_page: () ->
    settings = Settings.takeFirst((x) -> x.is_feeds_per_page())

    feeds_per_page = 30

    if settings?
      feeds_per_page = parseInt(settings.value(), 10)
    feeds_per_page

  get_limit: () ->
    @_get_per_page()

  get_offset: (page_num) ->
    (page_num - 1) * @get_limit()

  render_favorites: (feeds, current_page) ->
    start_page_name = "/favorites"
    options = @_make_options(feeds, current_page, start_page_name)

    html = Templates.favorites_template.render(options)
    Templates.article_view.render(html).html()

  _make_options: (feeds, current_page, total_feeds, start_page_name) ->
    length = total_feeds
    feeds_per_page = @_get_per_page()

    pagination = @_make_pagination(length, feeds_per_page, current_page)

    needed_feeds = feeds

    options =
      feeds: needed_feeds
      current_page: current_page
      feeds_per_page: feeds_per_page
      pagination: pagination
      page_url: start_page_name
      is_favorites_page: start_page_name.contains("/favorites")
    options

  clean_main_page: () ->
    Templates.article_view.render("").swap()

  render_feeds: (feeds, current_page, total_feeds, start_page_name) ->
    is_short_view = @_is_short_view_enabled()

    options = @_make_options(feeds, current_page, total_feeds, start_page_name)

    result = ""
    if is_short_view
      result = Templates.short_view_feeds_list_template.render(options)
    else
      result = Templates.feeds_list.render(options)

    Templates.article_view.render(result).html()


  render_feeds_and_source_overview: (source, overview, feeds, current_page, total_feeds) ->
    href = if overview.is_loaded_all()
      source.href_all()
    else
      source.href()
    @render_feeds(feeds, current_page, total_feeds, href)

    description = Templates.source_overview_template.render({source: source, overview: overview})
    Templates.source_overview_view.render(description).html()
    overview_view = new Sirius.View("#source-overview-#{source.id()}")

    if @_materializer?
      @_materializer.stop()

    @_materializer = Sirius.Materializer.build(overview, overview_view)
      .field((x) -> x.unread_count)
      .to((v) -> v.zoom('.source-overview-unread-count'))
      .transform((x) ->
        if x != 0
          "#{x} feeds"
        else
          "no feeds"
      )
      .field((x) -> x.favorites_count)
      .to((v) -> v.zoom('.source-overview-favorites-count'))
      .transform((x) ->
        if x != 0
          "#{x} feeds"
        else
          "no feeds"
      )

    @_materializer.run()

