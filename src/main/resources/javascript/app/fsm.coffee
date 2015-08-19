
States =
  Main: 0
  Sources: 1
  Plugins: 2
  Favorites: 3

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


ControllerStatesExt =
  state: new FSM()



