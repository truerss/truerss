
Sirius.View.register_strategy('html',
  transform: (oldvalue, newvalue) -> newvalue
  render: (adapter, element, result, attribute) ->
    if attribute == 'text'
      $(element).html(result)
    else
      throw new Error("Html strategy work only for text, not for #{attribute}")
 )