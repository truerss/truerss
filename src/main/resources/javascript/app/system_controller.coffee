
SystemController =

  restart: () ->
    $.ajax
      url: "/api/v1/system/restart"
      method: "GET"
      success: (msg) ->
        UIkit.notify
          message : "Start restarting system",
          status  : 'success',
          timeout : 1000,
          pos     : 'top-center'

  stop: () ->
    $.ajax
      url: "/api/v1/system/stop"
      method: "GET"
      success: (msg) ->
        UIkit.notify
          message : "Stop system",
          status  : 'success',
          timeout : 1000,
          pos     : 'top-center'

  exit: () ->
    $.ajax
      url: "/api/v1/system/exit"
      method: "GET"
      success: (msg) ->
        UIkit.notify
          message : "Stop app and exit",
          status  : 'success',
          timeout : 1000,
          pos     : 'top-center'

