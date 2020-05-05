
UploadController =

  logger: Sirius.Application.get_logger("UploadController")

  _modal: null

  to_model_materializer: null

  to_view_materializer: null

  upload: (e) ->
    logger = @logger

    if @_modal == null
      @_modal = UIkit.modal(document.querySelector("#upload-modal"))

    @_modal.show()


    clear_input = () ->
      jQuery("#{Templates.modal_view.get_element()} input").val('')


    if @to_model_materializer?
      @to_model_materializer.stop()

    if to_view_materializer?
      @to_model_materializer.stop()

    source = new Source()

    @to_model_materializer = Sirius.Materializer.build(Templates.modal_view, source)
      .field("input[name='title']")
      .to((x) -> x.name)
      .transform((x) -> x.text)
      .field("input[name='url']")
      .to((x) -> x.url)
      .transform((x) -> x.text)
      .field("select")
      .to((x) -> x.interval)
      .transform((x) -> x.text)

    @to_model_materializer.run()

    @to_view_materializer = Sirius.Materializer.build(source, Templates.modal_view)
      .field((x) -> x.errors.all)
      .to((v) -> v.zoom("span.errors"))

    @to_view_materializer.run()


    Templates.modal_view.on "#save-source", "click", (e) =>
      if source.is_valid()
        json = source.ajaxify()
        @logger.debug "Send a new source: #{json}"

        ajax.source_create source.ajaxify(),
          (json) =>
            Sources.add(new Source(json))
            # todo hide modal
            @_modal.hide()
            clear_input()

          (err) =>
            @logger.warn("Failed to create a new source: #{JSON.stringify(err)}")
            source.set_error("url.url_validator", err.responseJSON['error'])


    bar = document.getElementById("progressbar")


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