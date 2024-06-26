import StarIO10

func getDisplayInternationalCharacterType(name: String) -> StarXpandCommand.Display.InternationalCharacterType {
  switch name {
    case "usa":
      return .usa
    case "france":
      return .france
    case "germany":
      return .germany
    case "uk":
      return .uk
    case "denmark":
      return .denmark
    case "sweden":
      return .sweden
    case "italy":
      return .italy
    case "spain":
      return .spain
    case "japan":
      return .japan
    case "norway":
      return .norway
    case "denmark2":
      return .denmark2
    case "spain2":
      return .spain2
    case "latinAmerica":
      return .latinAmerica
    case "korea":
      return .korea
    default:
      return .usa
  }
}

func getDisplayCursorState(name: String) -> StarXpandCommand.Display.CursorState {
  switch name {
    case "on":
      return .on
    case "off":
      return .off
    case "blink":
      return .blink
    default:
      return .off
  }
  }
