
MainController =

  start: () ->
    $.ajax
      url: '/api/v1/sources/all'
      type: "GET"
      success: (arr) ->
        # TODO sorting by count
        arr.forEach((x) => Sources.add(new Source(x)))

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
            Sources.add(new Source(json))
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


