
class AjaxService

  constructor: () ->
    @sources_api = "/api/v1/sources"
    @feeds_api = "/api/v1/feeds"
    @plugin_api = "/api/v1/plugins"
    @settings_api = "/api/v1/settings"
    @k = () ->

  plugins_all: (success, error) ->
    @_get("#{@plugin_api}/all", success, error)

  js_all: (success) ->
    @_get("#{@plugin_api}/js", success, @k)

  css_all: (success) ->
    @_get("#{@plugin_api}/css", success, @k)

  sources_all: (success, error) ->
    @_get("#{@sources_api}/all", success, error)

  get_unread: (sourceId, success) ->
    @_get("#{@sources_api}/unread/#{sourceId}", success, @k)

  latest: (count, success, error) ->
    @_get("#{@sources_api}/latest/#{count}", success, error)

  source_create: (params, success, error) ->
    @_post("#{@sources_api}", params, success, error)

  refresh_all: () ->
    @_put("#{@sources_api}/refresh", @k , @k)

  get_feeds: (source_id, success, error) ->
    @_get("#{@sources_api}/feeds/#{source_id}", success, error)

  refresh_one: (num, success, error) ->
    @_put("#{@sources_api}/refresh/#{num}", {}, @k, @k)

  remove_source: (num, success, error) ->
    @_delete("#{@sources_api}/#{num}", @k, @k)

  update_source: (id, params, success, error) ->
    @_put("#{@sources_api}/#{id}", params, success, error)

  favorites_feed: (success, error) ->
    @_get("#{@feeds_api}/favorites", success, error)

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
    @_put("#{@sources_api}/mark/#{source_id}", @k, @k)

  mark_all_as_read: () ->
    @_put("#{@sources_api}/markall", @k, @k)

  get_source_overview: (sourceId, success) ->
    @_get("#{@sources_api}/overview/#{sourceId}", success, @k)

  get_feed_content: (feedId, success) ->
    @_get("#{@feeds_api}/content/#{feedId}", success, @k)

  get_settings: (success) ->
    @_get(
      "#{@settings_api}/current",
      (response) -> success(response.map (x) -> new Setting(x))
      @k)

  update_settings: (success, error) ->
    # TODO update impl
    @_put("#{@settings_api}", success, error)

  update_settings: (params, success, error) ->
    @_put("#{@settings_api}", params, success, error)


  load_ejs: (url) ->
    jQuery.ajax
      type: "GET"
      url: "templates/#{url}.ejs"

  count_header: () -> "XCount"

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
      dataType: "json"
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
      type: "GET"
      url: url
      success: success
      error : error


States =
  Main: 0
  Sources: 1
  Plugins: 2
  Favorites: 3
  About: 4
  Plugins: 5
  List: 6
  Source: 7
  Feed: 8
  Settings: 9

class Steps
  constructor: (state = States.Main) ->
    @_supported = Object.keys(States).map (k) -> States[k]
    @_swap = {}
    for k, v of States
      @_swap[v] = k
    @_state = @_swap[state]

  set: (state) ->
    if @_supported.indexOf(state) == -1
      alert("Unexpected state: #{state}, supported: #{@_supported}")
    else
      @_state = @_swap[state]

  to: (state) -> @set(state)
  current: () -> @_state
  get: () -> @current()

  hasState: (state) ->
    @_swap[state] is @_state

  isState: (state) ->
    @_swap[state] is @_state


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

class GlobalStateService
  constructor: () ->
    @current = null
  set: (id) ->
    @current = id
    return
  is_empty: () -> @current is null
  get: () -> @current
  clear: () ->
    @current = null
    return


Array::each_cons = (num) ->
  Array.from(
    {length: @length - num + 1},
    (_, i) => @slice(i, i + num)
  )

Array::add_to = (el) ->
  if @length == 0 || @[@length-1] != el
    @push(el)
  @

ControllerExt =
  ajax: new AjaxService()
  state: new Steps()
  posts: new GlobalStateService()
  sources: new GlobalStateService()
  read_cookie: window.readCookie
  delete_cookie: (cn) ->
    document.cookie = cn + '=; Path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT;'

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

  _materializer: null

  render_source_feeds_and_redirect_to_first: (source, current_page, source_name_normalized, overview) ->
    # TODO from setup - feeds per page
    feeds = source.feeds()
    length = feeds.length

    settings = Settings.takeFirst((x) -> x.is_feeds_per_page())

    feeds_per_page = 30

    if settings?
      feeds_per_page = parseInt(settings.value(), 10)

    pagination = @_make_pagination(length, feeds_per_page, current_page)

    start = (current_page - 1) * feeds_per_page
    end = start + feeds_per_page

    needed_feeds = feeds.slice(start, end)

    options =
      feeds: needed_feeds
      current_page: current_page
      feeds_per_page: feeds_per_page
      pagination: pagination
      source_name_normalized: source_name_normalized

    result = Templates.feeds_list.render(options)
    Templates.article_view.render(result).html()
    if source.feeds().length > 0
      1

    # overview

    description = Templates.source_overview_template.render({source: source, overview: overview})
    Templates.source_overview_view.render(description).html()
    overview_view = new Sirius.View("#source-overview-#{source.id()}")

    if @_materializer?
      @_materializer.stop()

    @_materializer = Sirius.Materializer.build(source, overview_view)
      .field((x) -> x.count)
      .to((v) -> v.zoom('.source-overview-unread-count'))
      .transform((x) ->
        if x != 0
          "#{x}"
        else
          ""
      )
      .field((x) -> x.favorites_count)
      .to((v) -> v.zoom('.source-overview-favorites-count'))

    @_materializer.run()

      # TODO redirect(source.feeds()[0].href())


