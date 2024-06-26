import StarIO10

func getPrinterInternationalCharacterType(name: String) -> StarXpandCommand.Printer.InternationalCharacterType {
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
    case "ireland":
      return .ireland
    case "slovenia":
      return .slovenia
    case "croatia":
      return .croatia
    case "china":
      return .china
    case "vietnam":
      return .vietnam
    case "arabic":
      return .arabic
    case "legal":
      return .legal
    default:
      return .usa
  }
}
