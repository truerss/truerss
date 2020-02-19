
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
      ajax.load_ejs("list")
      ajax.load_ejs("feeds_list")
      ajax.load_ejs("all_sources")
      ajax.load_ejs("feeds")
      ajax.load_ejs("favorites")
      ajax.load_ejs("plugins")
      ajax.load_ejs("main")
      ajax.load_ejs("tippy_tooltip")
    ).done (sources, list, feeds_list, all_sources, feeds, favorites, plugins, main, tippy_tooltip) =>
      # TODO use async loading in controllers
      Templates.source_list = new EJS(sources[0])
      Templates.list = new EJS(list[0])
      Templates.feeds_list = new EJS(feeds_list[0])
      Templates.all_sources_template = new EJS(all_sources[0])
      Templates.feeds_template = new EJS(feeds[0])
      Templates.favorites_template = new EJS(favorites[0])
      Templates.plugins_template = new EJS(plugins[0])
      Templates.feed_template = new EJS(main[0])
      Templates.tippy_template = new EJS(tippy_tooltip[0])

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
          Sirius.Application.get_adapter().and_then (adapter) ->
            self._init_ws(logger, adapter, port)

          arr = arr.sort (a, b) -> parseInt(a.count) - parseInt(b.count)

          arr.forEach((x) => Sources.add(new Source(x)))

          if arr.length > 0
            sxs = Sources.all().filter (s) -> s.count() > 0
            if sxs[0]
              source = sxs[0]
              redirect(source.href())
            else
              redirect(Sources.first().href())


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



