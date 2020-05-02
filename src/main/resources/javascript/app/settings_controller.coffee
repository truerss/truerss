
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

#    Sirius.Materializer.build(view, setting)
#      .field((v) -> v.zoom("#{html_element}[name='#{key}']"))
#      .from_attribute("selected")
#      .to((x) -> x.value)
#      .transform((changes) -> changes.text) # or state
#      .run()


  show: () ->
    if @_modal == null
      @_modal = UIkit.modal(document.querySelector("#settings-modal"))

    @_modal.show()

  save: () ->
    c(123)

    @_modal.hide()