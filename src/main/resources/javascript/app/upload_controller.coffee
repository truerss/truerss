
UploadController =

  logger: Sirius.Application.get_logger("UploadController")

  _modal: null

  upload: (e) ->
    if @_modal == null
      @_modal = UIkit.modal(document.querySelector("#upload-modal"))

    @_modal.show()


    bar = document.getElementById("progressbar")

    console.log(bar)

    @logger.debug "Start upload"

    UIkit.upload("#upload", {
      url: '/api/v1/sources/import',
      allow: '*.opml',

      multiple: true,

      loadStart: (e) ->
        bar.removeAttribute('hidden')
        bar.max = e.total
        bar.value = e.loaded

      error: (e) ->
        console.log("boom #{e}")

      progress: (e) ->
        bar.max = e.total
        bar.value = e.loaded

      loadEnd: (e) ->
        bar.max = e.total
        bar.value = e.loaded

      complete: (json) ->
        console.log(json)

      completeAll: () ->
        setTimeout(
          () ->
            bar.setAttribute('hidden', 'hidden')
            # TODO close modal
          1000
        )
    })