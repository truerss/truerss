
MainController =

  start: () ->
    $.ajax
      url: '/api/v1/sources/all'
      type: "GET"
      success: (arr) ->
        # TODO sorting by count
        arr.forEach((x) => Sources.add(new Source(x)))


  about: () ->


