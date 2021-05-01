
PluginsController =

  _modal: null

  load: () ->
    ajax.plugins_all (list) ->
      ajax.available_plugins (available) ->
        result = Templates.plugins_template.render({plugins: list, available: available})
        Templates.plugins_view.render(result).html()

  show: () ->
    if @_modal == null
      @_modal = UIkit.modal(document.querySelector("#plugins-modal"))

    @_modal.show()
