
SettingsController =

  _modal: null

  _materializers: []

  logger: Sirius.Application.get_logger("SettingsController")

  load: () ->
    ajax.get_settings (arr) =>
      arr.forEach (x) -> Settings.add(x)
      result = Templates.settings_template.render({settings: arr})
      Templates.settings_view.render(result).html()

      @_materializers.forEach (x) -> x.stop()

      Settings.map (settings) =>
        key = settings.key()
        @logger.debug("bind #{key}")
        if settings.is_radio()
          Templates.settings_view.on "#settings-#{key} a", "click", (e) =>
            Sirius.Application.get_adapter().and_then (adapter) =>
              value = parseInt(adapter.get_properties(e, ["data-value"])[0], 10)
              settings.value(value == 1)
              new Sirius.View("ul.settings-#{key}").zoom("li").render("uk-active").remove_class()
              $(e.target).parents("li").addClass("uk-active")

        else
          materializer = Sirius.Materializer.build(Templates.settings_view, settings)

          materializer
            .field((v) -> v.zoom("#settings-#{key} select"))
            .to((m) -> m.value)
            .transform((x) -> parseInt(x.text))

          materializer.run()
          @_materializers.push(materializer)


  show: () ->
    if @_modal == null
      @_modal = UIkit.modal(document.querySelector("#settings-modal"))

    @_modal.show()

    Templates.settings_view.on "#settings-save", "click", (e) =>
      Settings.map (x) =>
        json = x.ajaxify()
        ajax.update_settings json,
          (ok) =>
            @logger.debug("setting: #{x.key()} updated to #{ok.value}")
            x.value(ok.value)
            @_modal.hide()

          (err) =>
            c("fail: #{JSON.stringify(err)}")


  save: () ->
    @_modal.hide()