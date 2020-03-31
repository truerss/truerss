
SettingsController =

  _modal: null

  load: () ->
    ajax.get_settings (arr) ->
      arr.forEach (x) -> Settings.add(x)
      # TODO bind Settings.Collection plz
      result = Templates.settings_template.render({settings: arr})
      Templates.settings_view.render(result).html()

  on_add: (event, setting) ->
    key = setting.key()
    view = new Sirius.View("#settings-#{key}")
    html_element = if setting.is_radio()
      "input"
    else
      "select"
    transformer = Sirius.Transformer.draw({
      "#{html_element}[name='#{key}']":
        to: 'value'
        from: 'selected'
        via: (new_value, old_value, selector, event_target) ->
          c(new_value)
          c(old_value)
          c(selector)
          c(event_target)
    })

    view.bind(setting, transformer)


  show: () ->
    if @_modal == null
      @_modal = UIkit.modal(document.querySelector("#settings-modal"))

    @_modal.show()

  save: () ->
    c(123)

    @_modal.hide()