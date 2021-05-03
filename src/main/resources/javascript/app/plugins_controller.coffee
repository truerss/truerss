
PluginsController =

  _modal: null

  load: () ->
    ajax.plugins_all (list) ->
      ajax.available_plugins (available) ->
        # available = [{id:1, url: asd, plugins: [http://asd.jar]
        # list = {content: [author, about, jarSourcePath]
        sources = for _, v of list
          for e in v
            arr = e.jarSourcePath.split("/")
            arr.last() # foo.jar
            e.jarSourcePath

        sources = sources.flat()

        available = available.map (obj) ->
          obj.plugins = obj.plugins.map (url) ->
            tmp = url.split("/")
            last = tmp.last()
            installed = false
            jarSourcePath = ""
            need = sources.find (jar) -> jar.split("/").last() == last
            if need
              installed = true
              jarSourcePath = need
            {
              url: url,
              name: last.replace(".jar", ""),
              installed: installed,
              jarSourcePath: jarSourcePath
            }
          obj

        is_empty = true
        for _, v of list
          is_empty = false unless v.is_empty()
        result = Templates.plugins_template.render({plugins: list, is_empty: is_empty, available: available})
        Templates.plugins_view.render(result).html()

  show: () ->
    if @_modal == null
      @_modal = UIkit.modal(document.querySelector("#plugins-modal"))

    @_modal.show()

  install: (e, url) ->
    $("#cover").fadeIn(100)
    ajax.install_plugin url, () ->
      Sirius.Application.get_adapter().and_then (adapter) =>
        $("#cover").fadeOut(100)
        adapter.fire(document, "plugins:load")


  uninstall: (e, url) ->
    ajax.uninstall_plugin url, () ->
      Sirius.Application.get_adapter().and_then (adapter) =>
         adapter.fire(document, "plugins:load")

