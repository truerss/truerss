
MainController =

  start: () ->
    $.ajax
      url: '/api/v1/sources/all'
      type: "GET"
      success: (arr) ->
        # TODO sorting by count
        arr.forEach((x) => Sources.add(new Source(x)))

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


