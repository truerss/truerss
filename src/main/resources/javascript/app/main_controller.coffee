
MainController =

  error_message: """
    <h1>Error! Websocket and FormData not supported</h1>
    <p>
      Seems you have old browser.
      TrueRSS work with IE 10+, FF 33+,
      Chrome 31+ and Opera 26+.
      Update browser please.
    </p>
  """

  start: () ->
    if !(!!window.WebSocket && !!window.FormData && !!history.pushState)
      UIkit.notify
          message : @error_message,
          status  : 'danger',
          timeout : 30000,
          pos     : 'top-center'

    else

      $.ajax
        url: '/api/v1/sources/all'
        type: "GET"
        success: (arr) ->
          Sirius.Application.get_adapter().and_then (adapter) ->
            # open socket TODO change port with cookie ?
            # TODO c -> logger.info
            sock = new WebSocket("ws://#{location.hostname}:8080/")
            sock.onopen = () ->
              c("ws open")

            sock.onmessage = (e) ->
              message = JSON.parse(e.data)
              c("ws given message: #{message.messageType}")
              adapter.fire(document, "ws:#{message.messageType}", message.body)

            sock.onclose = () ->
              c("ws close")

          # TODO sorting by count
          arr.forEach((x) => Sources.add(new Source(x)))

          $.ajax
            url: '/api/v1/sources/latest/100'
            type: "GET"
            success: (arr) ->
              feeds = arr.map (x) ->
                pd = moment(x['publishedDate'])
                x['publishedDate'] = pd
                f = new Feed(x)
                source = Sources.find("id", f.source_id())
                source.add_feed(f)
                f
              feeds = _.sortBy(feeds, '_published_date')
              result = Templates.feeds_template.render({feeds: feeds})
              Templates.feeds_view.render(result).html()

              if feeds.length > 0
                redirect(feeds[0].href())

      source = new Source()
      Templates.modal_view.bind source,
        "fieldset input[name='title']" : to: "name"
        "fieldset input[name='url']" : to: "url"
        "fieldset input[name='interval']" : to : "interval"

      source.bind Templates.modal_view,
        "fieldset span.source-url-error" : from: "errors.url.url_validator"

      modal = UIkit.modal("#add-modal")

      Templates.modal_view.on 'button.uk-button-primary', 'click', (e) ->
        if source.is_valid()
          $.ajax
            url: '/api/v1/sources/create'
            type: "POST"
            dataType: "json"
            data: source.ajaxify()
            success: (json) ->
              # see WSController
              #Sources.add(new Source(json))
              modal.hide()

      Templates.modal_view.on 'button.close-modal', 'click', (e) ->
        modal.hide()



  view: () ->
    unless state.hasState(States.Main)
      result = Templates.feed_template.render({
        feed: {
          title: () -> "Easily create nicely looking buttons, which come in different styles.",
          description: () -> "A button can be used to trigger a dropdown menu from the Dropdown component. Just add the .uk-button-dropdown class and the data-uk-dropdown attribute to a
            <div> element that contains the button and the dropdown itself."
        }
      })

      Templates.article_view.render(result).html()
      state.to(States.Main)


  about: () ->
    Templates.article_view.render("<h1>about truerss</h1>").html()
    state.to(States.About)

  plugin_list: () ->
    $.ajax
      url: "/api/v1/plugins/all"
      method: "GET"
      success: (list) ->
        result = Templates.plugins_template.render({plugins: list})
        Templates.article_view.render(result).html()
        state.to(States.Plugins)



