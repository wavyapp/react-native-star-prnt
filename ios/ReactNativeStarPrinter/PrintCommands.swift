enum Action: String {
  case cut = "cut"
  case partialCut = "partial-cut"
  case fullDirect = "full-direct"
  case partialDirect = "partial-direct"
  case paperFeed = "paper-feed"
  case feedLine = "feed-line"
  case printRuledLine = "print-line-separator"
  
  static func fromString(value: String) throws -> Action {
    return switch value {
      case "cut":
          .cut
      case "partial-cut":
          .partialCut
      case "full-direct":
          .fullDirect
      case "partial-direct":
          .partialDirect
      case "paper-feed":
          .paperFeed
      case "feed-line":
          .feedLine
      case "print-line-separator":
          .printRuledLine
      default:
        throw ReactNativeStarPrinterError.invalidArgument(message: "\(value) is not a valid printer action")
    }
  }
}

enum PrintDataType: String {
  case text = "text"
  case image = "image"
  case barcode = "barcode"
  
  static func fromString(value: String) throws -> PrintDataType {
    return switch value {
      case "text":
          .text
      case "image":
          .image
      case "barcode":
          .barcode
      default:
        throw ReactNativeStarPrinterError.invalidArgument(message: "\(value) is not a valid print data type")
    }
  }
}

enum Align: String {
  case center = "center"
  case left = "left"
  case right = "right"
  
  static func fromString(value: String) throws -> Align {
    return switch value {
      case "center":
          .center
      case "left":
          .left
      case "right":
          .right
      default:
        throw ReactNativeStarPrinterError.invalidArgument(message: "Invalid align value \(value)")
    }
  }
}

struct Style {
  let align: Align?
  let barWidth, height, heightExpansion, threshold, width, widthExpansion: Int?
  let bold, diffusion, underlined: Bool?
  
  init(
    align: Align?,
    barWidth: Int?,
    bold: Bool?,
    diffusion: Bool?,
    height: Int?,
    heightExpansion: Int?,
    threshold: Int?,
    underlined: Bool?,
    width: Int?,
    widthExpansion: Int?
  ) {
    self.align = align
    self.barWidth = barWidth
    self.bold = bold
    self.diffusion = diffusion
    self.height = height
    self.heightExpansion = heightExpansion
    self.threshold = threshold
    self.underlined = underlined
    self.width = width
    self.widthExpansion = widthExpansion
  }
  
  static func fromDict(style: [String: Any?]) throws -> Style {
    if (style["align"] as? String == nil && style["align"] != nil) {
      throw ReactNativeStarPrinterError.invalidArgument(message: "Invalid align value type \(style["align"])")
    }
    if (style["barWidth"] as? Int == nil && style["barWidth"] != nil) {
      throw ReactNativeStarPrinterError.invalidArgument(message: "Invalid barWidth value type \(style["barWidth"])")
    }
    if (style["bold"] as? Bool == nil && style["bold"] != nil) {
      throw ReactNativeStarPrinterError.invalidArgument(message: "Invalid bold value type \(style["bold"])")
    }
    if (style["heightExpansion"] as? Int == nil && style["heightExpansion"] != nil) {
      throw ReactNativeStarPrinterError.invalidArgument(message: "Invalid heightExpansion value type \(style["heightExpansion"])")
    }
    if (style["underlined"] as? Bool == nil && style["underlined"] != nil) {
      throw ReactNativeStarPrinterError.invalidArgument(message: "Invalid underlined value type \(style["underlined"])")
    }
    if (style["widthExpansion"] as? Int == nil && style["widthExpansion"] != nil) {
      throw ReactNativeStarPrinterError.invalidArgument(message: "Invalid widthExpansion value type \(style["widthExpansion"])")
    }
    if (style["width"] as? Int == nil && style["width"] != nil) {
      throw ReactNativeStarPrinterError.invalidArgument(message: "Invalid width value type \(style["width"])")
    }
    if (style["height"] as? Int == nil && style["height"] != nil) {
      throw ReactNativeStarPrinterError.invalidArgument(message: "Invalid height value type \(style["height"])")
    }
    if (style["threshold"] as? Int == nil && style["threshold"] != nil) {
      throw ReactNativeStarPrinterError.invalidArgument(message: "Invalid threshold value type \(style["threshold"])")
    }
    if (style["diffusion"] as? Bool == nil && style["diffusion"] != nil) {
      throw ReactNativeStarPrinterError.invalidArgument(message: "Invalid diffusion value type \(style["diffusion"])")
    }
    
    return Style(
      align: try style["align"].map { try Align.fromString(value: $0 as? String ?? "center") },
      barWidth: style["barWidth"] as? Int,
      bold: style["bold"] as? Bool,
      diffusion: style["diffusion"] as? Bool,
      height: style["height"] as? Int,
      heightExpansion: style["heightExpansion"] as? Int,
      threshold: style["threshold"] as? Int,
      underlined: style["underlined"] as? Bool,
      width: style["width"] as? Int,
      widthExpansion: style["widthExpansion"] as? Int
    )
  }
}

protocol PrinterCommand {}

struct Print: PrinterCommand {
  let data: String
  let style: Style?
  let type: PrintDataType
  
  init(data: String, type: PrintDataType, style: Style?) {
    self.data = data
    self.style = style
    self.type = type
  }
}

struct PrinterAction: PrinterCommand {
  let action: Action
  let args: [String: Any?]?
  
  init(_ action: Action, _ args: [String: Any?]?) {
    self.action = action
    self.args = args
  }
}

func printerCommandFactory(_ command: [String: Any?]) throws -> PrinterCommand {
  if let data = command["data"] as? String {
    let type = command["type"] as? String ?? "text"
    let style = command["style"] as? [String: Any?]
    return Print(
      data: data,
      type: try PrintDataType.fromString(value: type),
      style: try style.map { try Style.fromDict(style: $0) }
    )
  }
  if let action = command["action"] as? String {
    return PrinterAction(try Action.fromString(value: action), command["actionArguments"] as? [String: Any?])
  }
  throw ReactNativeStarPrinterError.invalidArgument(message: "The command $command has no data nor action")
}

