
SystemController =

  _notify: (msg) ->
    UIkit.notify
      message : msg,
      status  : 'success',
      timeout : 1000,
      pos     : 'top-center'

  restart: () ->
    ajax.restart_system (msg) =>
      self._notify("Start restarting system")

  stop: () ->
    ajax.stop_system (msg) =>
      @_notify("Stop system")

  exit: () ->
    ajax.exit_app (msg) =>
      @_notify("Stop app and exit")

