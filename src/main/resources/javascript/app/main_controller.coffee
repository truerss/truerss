
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
    protocol = if location.protocol is "https"
      "wss"
    else
      "ws"

    ws = new WebSocket("#{protocol}://#{location.hostname}:#{port}/")
    ws.onopen = () ->
      @logger.info("ws open on port: #{port}")

    ws.onmessage = (e) ->
      message = JSON.parse(e.data)
      @logger.info("ws given message: #{message.messageType}")
      adapter.fire(document, "ws:#{message.messageType}", message.body)
    ws.onclose = () ->
      @logger.info("ws close")

  _bind_modal: () ->
    source = new Source()

    Sirius.Materializer.build(Templates.modal_view, source)
      .field("input[name='title']")
      .to((x) -> x.name)
      .transform((x) -> x.text)
      .field("input[name='url']")
      .to((x) -> x.url)
      .transform((x) -> x.text)
      .field("input[name='interval']")
      .to((x) -> x.interval)
      .transform((x) -> x.text)
      .run()

    Sirius.Materializer.build(source, Templates.modal_view)
      .field((x) -> x.errors.url.url_validator)
      .to("span.source-url-error")

    modal = UIkit.modal("#add-modal")
    # TODO use binding
    clear_input = () ->
      jQuery("#{Templates.modal_element} .uk-form input.custom-input").val('')

    Templates.modal_view.on 'button.uk-button-primary', 'click', (e) ->
      if source.is_valid()
        ajax.source_create source.ajaxify(),
          (json) ->
            Sources.add(new Source(json))
            modal.hide()
            clear_input()
          (err) ->
            source.set_error("url.url_validator", err.responseJSON['error'])

    Templates.modal_view.on 'button.close-modal', 'click', (e) ->
      modal.hide()

      clear_input()

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
      ajax.load_ejs("main")
      ajax.load_ejs("tippy_tooltip")
      ajax.load_ejs("settings")
      ajax.load_ejs("source_overview")
    ).done (sources, feeds_list, favorites, plugins, main, tippy_tooltip, settings, source_description) =>
      # TODO use async loading in controllers
      Templates.source_list = new EJS(sources[0])
      Templates.feeds_list = new EJS(feeds_list[0])
      Templates.favorites_template = new EJS(favorites[0])
      Templates.plugins_template = new EJS(plugins[0])
      Templates.feed_template = new EJS(main[0])
      Templates.tippy_template = new EJS(tippy_tooltip[0])
      Templates.settings_template = new EJS(settings[0])
      Templates.source_overview_template = new EJS(source_description[0])

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
        self = @
        logger = @logger
        ajax.sources_all (arr) ->
          logger.info("load #{arr.length} sources")

          Sirius.Application.get_adapter().and_then (adapter) ->
            logger.info("initialize ws on #{port}")
            adapter.fire(document, "plugins:load")
            adapter.fire(document, "settings:load")
            #TODO self._init_ws(@logger, adapter, port)

          arr = arr.sort (a, b) -> parseInt(a.count) - parseInt(b.count)

          arr.forEach((x) => Sources.add(new Source(x)))

          if arr.length > 0
            sxs = Sources.all().filter (s) -> s.count() > 0
            logger.info "redirect to"
            if sxs[0]
              source = sxs[0]

              #TODO redirect(source.href())
            else
              #TODO redirect(Sources.first().href())


        @_bind_modal()
      delete_cookie("redirect")
    return

  view: () ->
    unless state.hasState(States.Main)
      source = Sources.first()
      if source
        redirect(source.href())

  about: () ->
    ajax.about (info) ->
      Templates.article_view.render(info).html()
      state.to(States.About)



