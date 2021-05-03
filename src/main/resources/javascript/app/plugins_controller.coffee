
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

        sources = sources.flat()
        available = available.map (obj) ->
          obj.plugins = obj.plugins.map (url) ->
            tmp = url.split("/")
            last = tmp.last()
            installed = false
            if sources.contains(last)
              installed = true
            {
              url: url,
              name: last.replace(".jar", ""),
              installed: installed
            }
          obj

        result = Templates.plugins_template.render({plugins: list, available: available})
        Templates.plugins_view.render(result).html()

  show: () ->
    if @_modal == null
      @_modal = UIkit.modal(document.querySelector("#plugins-modal"))

    @_modal.show()

  install: (e, url) ->
    c(e)
    ajax.install_plugin(url)

  remove: (e, url) ->
    c(e)
    ajax.remove_plugin(url)

