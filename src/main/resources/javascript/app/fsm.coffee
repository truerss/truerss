
States =
  Main: 0
  Sources: 1
  Plugins: 2
  Favorites: 3
  About: 4

# TODO rename
class FSM
  constructor: (state = States.Main) ->
    @_supported = Object.keys(States).map (k) -> States[k]
    @_swap = {}
    for k, v of States
      @_swap[v] = k
    @_state = @_swap[state]

  set: (state) ->
    if @_supported.indexOf(state) == -1
      alert("Unexpected state: #{state}, supported: #{@_supported}")
    else
      @_state = @_swap[state]

  to: (state) -> @set(state)
  current: () -> @_state
  get: () -> @current()

  hasState: (state) ->
    @_swap[state] is @_state

# from http://stackoverflow.com/a/5639455/1581531
`
(function(){
    var cookies;

    function readCookie(name,c,C,i){
        if(cookies){ return cookies[name]; }

        c = document.cookie.split('; ');
        cookies = {};

        for(i=c.length-1; i>=0; i--){
           C = c[i].split('=');
           cookies[C[0]] = C[1];
        }

        return cookies[name];
    }

    window.readCookie = readCookie; // or expose it however you want
})();
`


ControllerStatesExt =
  state: new FSM()
  read_cookie: window.readCookie


