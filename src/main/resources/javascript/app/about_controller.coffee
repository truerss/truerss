
AboutController =

  _modal: null

  load: () ->
    ajax.about (info) ->
      result = Templates.about_template.render({text: info})
      Templates.about_view.render(result).html()

  show: () ->
    if @_modal == null
      @_modal = UIkit.modal(document.querySelector("#about-modal"))

    @_modal.show()

