`var c = function(m){console.log(m);};`

$ ->
  routes =
    "application:run" : controller: MainController, action: "start"
    "/sources" : controller: SourcesController, action: "all"
    "/" : controller: MainController, action: "view"
    "/show/:source-name" : controller: SourcesController, action: "show"
    "click a[href='#refresh']" : controller: SourcesController, action: "refresh_all"

  app = Sirius.Application.run
    route: routes
    adapter: new JQueryAdapter()
    mix_logger_into_controller: true
    controller_wrapper: ControllerStatesExt
    log: false