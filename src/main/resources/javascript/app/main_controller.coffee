
MainController =

  error_message: """
    <h1>Error! Websockets and FormData not supported</h1>
    <p>
      Seems you have old browser.
      TrueRSS work with IE 10+, FF 33+,
      Chrome 31+ and Opera 26+.
      Update browser please.
    </p>
  """

  _init_ws: (logger, adapter, port) ->
    protocol = if location.protocol is "https"
      "wss"
    else
      "ws"

    ws = new WebSocket("#{protocol}://#{location.hostname}:#{port}/")
    ws.onopen = () ->
      logger.info("ws open on port: #{port}")

    ws.onmessage = (e) ->
      message = JSON.parse(e.data)
      logger.info("ws given message: #{message.messageType}")
      adapter.fire(document, "ws:#{message.messageType}", message.body)
    ws.onclose = () ->
      logger.info("ws close")

  _bind_modal: () ->
    source = new Source()

    to_model_transformer = Sirius.Transformer.draw({
      "input[name='title']" : to: "name"
      "input[name='url']" : to: "url"
      "input[name='interval']" : to : "interval"
    })

    Templates.modal_view.bind(source, to_model_transformer)

    to_view_transformer = Sirius.Transformer.draw({
      "errors.url.url_validator": {
        to: 'span.source-url-error'
      }
    })

    source.bind(Templates.modal_view, to_view_transformer)

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
        ajax.sources_all (arr) ->
          logger.info("load #{arr.length} sources")
          
          Sirius.Application.get_adapter().and_then (adapter) ->
            logger.info("initialize ws on #{port}")
            #TODO self._init_ws(logger, adapter, port)

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

        ajax.get_settings (arr) ->
          arr.forEach (x) -> Settings.add(x)
          # TODO bind Settings.Collection plz
          result = Templates.settings_template.render({settings: arr})
          Templates.settings_view.render(result).html()

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

  plugin_list: () ->
    ajax.plugins_all (list) ->
      result = Templates.plugins_template.render({plugins: list})
      Templates.article_view.render(result).html()
      state.to(States.Plugins)



