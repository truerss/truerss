
SettingsController =

  _modal: UIkit.modal(document.querySelector("#settings-modal"))

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
    UIkit.modal(document.querySelector("#settings-modal")).show()

  save: () ->
    c(123)

    @_modal.hide()