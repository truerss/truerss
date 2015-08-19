`var c = function(m){console.log(m);};`

$ ->
  routes =
    "application:run" : controller: MainController, action: "start"
    "/sources" : controller: SourcesController, action: "all"
    "/" : controller: MainController, action: "view"

  app = Sirius.Application.run
    route: routes
    adapter: new JQueryAdapter()
    mix_logger_into_controller: true
    controller_wrapper: ControllerStatesExt
    log: false