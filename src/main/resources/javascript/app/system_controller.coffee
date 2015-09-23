
SystemController =

  restart: () ->
    ajax.restart_system (msg) ->
      UIkit.notify
        message : "Start restarting system",
        status  : 'success',
        timeout : 1000,
        pos     : 'top-center'

  stop: () ->
    ajax.stop_system (msg) ->
      UIkit.notify
        message : "Stop system",
        status  : 'success',
        timeout : 1000,
        pos     : 'top-center'

  exit: () ->
    ajax.exit_app (msg) ->
      UIkit.notify
        message : "Stop app and exit",
        status  : 'success',
        timeout : 1000,
        pos     : 'top-center'

