
MainController =

  start: () ->
    $.ajax
      url: '/api/v1/sources/all'
      type: "GET"
      success: (arr) ->
        c(state.current())
        c(state.to(States.Sources))
        c(state.to("das"))
        # TODO sorting by count
        arr.forEach((x) => Sources.add(new Source(x)))


  about: () ->


