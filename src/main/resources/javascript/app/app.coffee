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
    "ws:new": controller: WSController, action: "fresh"
    "ws:create": controller: WSController, action: "create"
    "ws:deleted": controller: WSController, action: "deleted"
    "ws:updated": controller: WSController, action: "updated"
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
    "click a.feed-link": controller: FeedsController, action: "view0", data: "data-feed-id"
    "click a[href='#refresh']" : controller: SourcesController, action: "refresh_all"
    "click i.favorite": controller: FeedsController, action: "favorite", data: "data-favorite"
    "click i.unfavorite": controller: FeedsController, action: "unfavorite", data: "data-favorite"
    "click a[href='#update-source']" : controller: SourcesController, action: "refresh_one", data: "data-source-id"
    "click a[href='#delete-source']": controller: SourcesController, action: "remove", data: "data-source-id"
    "click a[href='#edit-source']": controller: SourcesController, action: "edit", data: "data-source-id"
    "click a[href='#mark-source-as-read']": controller: SourcesController, action: "mark"
    "click a[href='#restart-system']": controller: SystemController, action: "restart"
    "click a[href='#stop-system']": controller: SystemController, action: "stop"
    "click a[href='#exit-app']": controller: SystemController, action: "exit"
    "click #truerss-next": controller: FeedsController, action: "next", data: "data-feed-id", guard: "prev_next_guard"
    "click #truerss-prev": controller: FeedsController, action: "prev", data: "data-feed-id", guard: "prev_next_guard"
    "click #truerss-markall": controller: SourcesController, action: "mark_all"

  app = Sirius.Application.run
    route: routes
    adapter: new JQueryAdapter()
    mix_logger_into_controller: true
    controller_wrapper: ControllerExt
    log: false
    log_filters: ['Redirect']