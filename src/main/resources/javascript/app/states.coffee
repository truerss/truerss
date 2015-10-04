
class AjaxService
  constructor: () ->
    @sources_api = "/api/v1/sources"
    @feeds_api = "/api/v1/feeds"
    @plugin_api = "/api/v1/plugins"
    @system_api = "/api/v1/system"
    @k = () ->

  plugins_all: (success, error) ->
    @_get("#{@plugin_api}/all", success, error)

  js_all: (success) ->
    @_get("#{@plugin_api}/js", success, @k)

  css_all: (success) ->
    @_get("#{@plugin_api}/css", success, @k)

  sources_all: (success, error) ->
    @_get("#{@sources_api}/all", success, error)

  latest: (count, success, error) ->
    @_get("#{@sources_api}/latest/#{count}", success, error)

  source_create: (params, success, error) ->
    @_post("#{@sources_api}/create", params, success, error)

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

  restart_system: (success, error) ->
    @_get("#{@system_api}/restart", success, error)

  stop_system: (success, error) ->
    @_get("#{@system_api}/stop", success, error)

  exit_app: (success, error) ->
    @_get("#{@system_api}/exit", success, error)

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

  _delete: (url, success, error = () -> ) ->
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

  _get: (url, success, error = () -> ) ->
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

# from http://stackoverflow.com/a/5639455/1581531
`
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


ControllerStatesExt =
  ajax: new AjaxService()
  state: new Steps()
  read_cookie: window.readCookie
  delete_cookie: (cn) ->
    document.cookie = cn + '=; expires=Thu, 01 Jan 1970 00:00:01 GMT;'


