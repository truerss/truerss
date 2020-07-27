
MainController =

  logger: Sirius.Application.get_logger("MainController")

  error_message: """
    <h1>Error! Websockets and FormData not supported</h1>
    <p>
      Seems you have old browser.
      TrueRSS work with IE 10+, FF 33+,
      Chrome 31+ and Opera 26+.
      Update browser please.
    </p>
  """

  _init_ws: (adapter, port) ->
    logger = @logger
    protocol = if location.protocol is "https"
      "wss"
    else
      "ws"

    ws = new WebSocket("#{protocol}://#{location.hostname}:#{port}/")
    ws.onopen = () ->
      logger.info("ws open on port: #{port}")

    ws.onmessage = (e) ->
      message = JSON.parse(e.data)
      c(message)
      logger.info("ws given message: #{message.messageType}")
      adapter.fire(document, "ws:#{message.messageType}", message.body)
    ws.onclose = () ->
      c("===================close")
      logger.info("ws close")

  _load_js_and_css: (ajax) ->
    ajax.js_all (resp) ->
      script = document.createElement('script')
      jQuery(script).text(resp)
      jQuery("head").append(script)

    ajax.css_all (resp) ->
      style = document.createElement('style')
      style.setAttribute("type", "text/css")
      jQuery(style).text(resp)
      jQuery("head").append(style)


  start: () ->
    jQuery.when(
      ajax.load_ejs("sources")
      ajax.load_ejs("feeds_list")
      ajax.load_ejs("favorites")
      ajax.load_ejs("plugins")
      ajax.load_ejs("tippy_tooltip")
      ajax.load_ejs("settings")
      ajax.load_ejs("source_overview")
      ajax.load_ejs("about")
      ajax.load_ejs("short_view_feeds_list")
    ).done (sources, feeds_list, favorites, plugins, tippy_tooltip, settings, source_description, about, short_view) =>
      # TODO use async loading in controllers
      Templates.source_list = new EJS(sources[0])
      Templates.feeds_list = new EJS(feeds_list[0])
      Templates.favorites_template = new EJS(favorites[0])
      Templates.plugins_template = new EJS(plugins[0])
      Templates.tippy_template = new EJS(tippy_tooltip[0])
      Templates.settings_template = new EJS(settings[0])
      Templates.source_overview_template = new EJS(source_description[0])
      Templates.about_template = new EJS(about[0])
      Templates.short_view_feeds_list_template = new EJS(short_view[0])

      @_load_js_and_css(ajax)
      port = read_cookie("port")
      mb_redirect = read_cookie("redirect")
      default_count = 100

      if !(!!window.WebSocket && !!window.FormData && !!history.pushState)
        UIkit.notify
          message : @error_message,
          status  : 'danger',
          timeout : 30000,
          pos     : 'top-center'

      else
        Sirius.Application.get_adapter().and_then (adapter) =>
          @logger.info("initialize ws on #{port}")
          adapter.fire(document, "plugins:load")
          adapter.fire(document, "settings:load")
          adapter.fire(document, "sources:load")
          adapter.fire(document, "about:load")
          @_init_ws(adapter, port)

      delete_cookie("redirect")
    return

