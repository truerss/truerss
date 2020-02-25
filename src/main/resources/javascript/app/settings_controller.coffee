
SettingsController =

  all: () ->
    ajax.get_settings (response) ->
      response.map (f) ->
        f['is_radio'] = !Sirius.Utils.is_array(f.options)
      c(response)
      result = Templates.settings_template.render({settings: response})
      Templates.article_view.render(result).html()
      state.to(States.Settings)
