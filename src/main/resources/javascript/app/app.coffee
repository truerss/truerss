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
      //{"0":{"id":2,"url":"test/rss","name":"123","interval":8,"state":0,"normalized":"123",
      // "lastUpdate":"2018-11-01 20:54:53","count":0}}
      var obj = JSON.parse(json);
      var keys = Object.keys(obj);
      for (var i = 0; i < keys.length; i++) {
        var key = keys[i];
        var value = obj[key]; // => object
        if (value["id"]) {
          Sources.add(new Source(value));
           // success flow
        } else {
          // notify
          var error = value["errors"].join(", ")
          UIkit.notify({
            message : error,
            status  : 'warning',
            timeout : 3000,
            pos     : 'top-right'
          });
        }
      }


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
    "plugins:load": controller: PluginsController, action: "load"
    "settings:load": controller: SettingsController, action: "load"
    "sources:fetch": controller: SourcesController, action: "fetch_unread"
    "ws:new": controller: WSController, action: "fresh"
    "ws:notify": controller: WSController, action: "notify"
    "/" : controller: MainController, action: "view"
    "/favorites" : controller: FeedsController, action: "favorites"
    "/plugins" : controller: PluginsController, action: "show"
    "/about" : controller: MainController, action: "about"
    "/show/:source-name/:feed-name": controller: FeedsController, action: "show"
    "/show/:source-name" : controller: SourcesController, action: "show"
    "/show/:source-name/page/:page" : controller: SourcesController, action: "show"
    "/opml": controller: SourcesController, action: "download"
    "/settings" : controller: SettingsController, action: "show"
    "settings:add": controller: SettingsController, action: "on_add"
    #"click a.feed-link": controller: FeedsController, action: "view0", data: "data-feed-id"
    "click a[href='#refresh']" : controller: SourcesController, action: "refresh_all"
    "click a.favorite": controller: FeedsController, action: "favorite", data: ["data-feed-id", "data-source-id"]
    "click a.unfavorite": controller: FeedsController, action: "unfavorite", data: ["data-feed-id", "data-source-id"]
    "click a.read": controller: FeedsController, action: "read", data: ["data-feed-id", "data-source-id"]
    "click a.unread": controller: FeedsController, action: "unread", data: ["data-feed-id", "data-source-id"]
    "click a[href='#refresh-source']" : controller: SourcesController, action: "refresh_one", data: "data-source-id"
    "click a[href='#remove-source']": controller: SourcesController, action: "remove", data: "data-source-id"
    "click a[href='#edit-source']": controller: SourcesController, action: "edit", data: "data-source-id"
    "click a.source-count": controller: SourcesController, action: "mark_by_click_on_count_button", data: "data-source-id"
    "input #search": controller: SourcesController, action: "filter"
#    "mouseenter .tippy-count": controller: FeedsController, action: 'draw_tooltip', data: "data-source-id"

  setup =
    enable_logging: true
    route: routes
    adapter: new JQueryAdapter()
    controller_wrapper: ControllerExt
    log_to: (log_level, log_source, message) ->
      msg = message.trim()
      is_define = msg.startsWith("define")
      is_set = msg.endsWith("to 'feeds'")
      is_render = msg.indexOf("Call render for") != -1

      if !is_define && !is_set && !is_render
        console.log("#{log_level} [#{log_source}] #{message}")


  Sirius.Application.run(setup)
    