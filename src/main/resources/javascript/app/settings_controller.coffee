
SettingsController =

  all: () ->
    ajax.get_settings (response) ->
      c(response)