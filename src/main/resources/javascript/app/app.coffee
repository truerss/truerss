`var c = function(m){console.log(m);};`

$ ->
  # TODO move to controller plz
  `
  // source http://getuikit.com/docs/upload.html
  var progressbar = $("#progressbar"),
    bar         = progressbar.find('.uk-progress-bar'),
    settings    = {

    action: '/api/v1/sources/import',

    allow : '*.opml',

    filelimit: 700,

    loadstart: function() {
        bar.css("width", "0%").text("0%");
        progressbar.removeClass("uk-hidden");
    },

    progress: function(percent) {
        percent = Math.ceil(percent);
        bar.css("width", percent+"%").text(percent+"%");
    },

    complete: function(json) {
      // TODO add source response
      c(json)
    },

    allcomplete: function(response) {

        bar.css("width", "100%").text("100%");

        setTimeout(function(){
            progressbar.addClass("uk-hidden");
        }, 250);

        // hide modal
        UIkit.modal("#add-modal").hide();
    }
  };

  var select = UIkit.uploadSelect($("#upload-select"), settings),
    drop   = UIkit.uploadDrop($("#upload-drop"), settings);
  `


  routes =
    "application:run" : controller: MainController, action: "start"
    "sources:fetch": controller: SourcesController, action: "fetch_unread"
    "ws:new": controller: WSController, action: "fresh"
    "ws:notify": controller: WSController, action: "notify"
    "/sources" : controller: SourcesController, action: "all"
    "/" : controller: MainController, action: "view"
    "/favorites" : controller: FeedsController, action: "favorites"
    "/plugins" : controller: MainController, action: "plugin_list"
    "/about" : controller: MainController, action: "about"
    "/show/:source-name/:feed-name": controller: FeedsController, action: "show"
    "/show/:source-name" : controller: SourcesController, action: "show"
    "/by-source" : controller: SourcesController, action: "by_source"
    "/by/:source-name": controller: FeedsController, action: "view"
    "/opml": controller: SourcesController, action: "download"
    "click a.feed-link": controller: FeedsController, action: "view0", data: "data-feed-id"
    "click a[href='#refresh']" : controller: SourcesController, action: "refresh_all"
    "click i.favorite": controller: FeedsController, action: "favorite", data: "data-favorite"
    "click i.unfavorite": controller: FeedsController, action: "unfavorite", data: "data-favorite"
    "click a[href='#update-source']" : controller: SourcesController, action: "refresh_one", data: "data-source-id"
    "click a[href='#delete-source']": controller: SourcesController, action: "remove", data: "data-source-id"
    "click a[href='#edit-source']": controller: SourcesController, action: "edit", data: "data-source-id"
    "click a[href='#mark-source-as-read']": controller: SourcesController, action: "mark"
    "click #truerss-next": controller: FeedsController, action: "next", data: "data-feed-id", guard: "prev_next_guard"
    "click #truerss-prev": controller: FeedsController, action: "prev", data: "data-feed-id", guard: "prev_next_guard"
    "keyup body": controller: FeedsController, action: "move", guard: "check_key"
    "keydown body": controller: FeedsController, action: "key_action", guard: "check_shift"
    "click #truerss-markall": controller: SourcesController, action: "mark_all"
    "click span.source-count": controller: SourcesController, action: "mark_by_click_on_count_button", data: "data-source-id"
    "input #search": controller: SourcesController, action: "filter"
    "mouseenter .tippy-count": controller: FeedsController, action: 'draw_tooltip', data: "data-source-id"

  app = Sirius.Application.run
    route: routes
    adapter: new JQueryAdapter()
    mix_logger_into_controller: true
    controller_wrapper: ControllerExt
    log: false
    log_filters: 'all'