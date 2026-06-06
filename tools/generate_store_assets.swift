import AppKit

struct Canvas {
    let size: CGSize
    let bitmap: NSBitmapImageRep

    init(width: CGFloat, height: CGFloat) {
        self.size = CGSize(width: width, height: height)
        self.bitmap = NSBitmapImageRep(
            bitmapDataPlanes: nil,
            pixelsWide: Int(width),
            pixelsHigh: Int(height),
            bitsPerSample: 8,
            samplesPerPixel: 4,
            hasAlpha: true,
            isPlanar: false,
            colorSpaceName: .deviceRGB,
            bytesPerRow: 0,
            bitsPerPixel: 0
        )!
        self.bitmap.size = size
    }

    func draw(_ work: () -> Void) {
        let previousContext = NSGraphicsContext.current
        NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: bitmap)
        NSGraphicsContext.current?.imageInterpolation = .high
        work()
        NSGraphicsContext.current = previousContext
    }

    func writePNG(to path: String) throws {
        guard let png = bitmap.representation(using: .png, properties: [:]) else {
            throw NSError(domain: "StoreAssetGenerator", code: 1)
        }
        try png.write(to: URL(fileURLWithPath: path))
    }
}

extension NSColor {
    convenience init(hex: Int, alpha: CGFloat = 1) {
        self.init(
            calibratedRed: CGFloat((hex >> 16) & 0xff) / 255,
            green: CGFloat((hex >> 8) & 0xff) / 255,
            blue: CGFloat(hex & 0xff) / 255,
            alpha: alpha
        )
    }
}

func fillRoundedRect(_ rect: CGRect, radius: CGFloat, color: NSColor) {
    color.setFill()
    NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius).fill()
}

func fillPath(points: [CGPoint], color: NSColor) {
    guard let first = points.first else { return }
    let path = NSBezierPath()
    path.move(to: first)
    for point in points.dropFirst() {
        path.line(to: point)
    }
    path.close()
    color.setFill()
    path.fill()
}

func drawLine(_ from: CGPoint, _ to: CGPoint, color: NSColor, width: CGFloat) {
    let path = NSBezierPath()
    path.move(to: from)
    path.line(to: to)
    path.lineWidth = width
    path.lineCapStyle = .round
    color.setStroke()
    path.stroke()
}

func drawText(_ text: String, rect: CGRect, size: CGFloat, weight: NSFont.Weight, color: NSColor, alignment: NSTextAlignment = .left) {
    let paragraph = NSMutableParagraphStyle()
    paragraph.alignment = alignment
    paragraph.lineBreakMode = .byWordWrapping
    let attributes: [NSAttributedString.Key: Any] = [
        .font: NSFont.systemFont(ofSize: size, weight: weight),
        .foregroundColor: color,
        .paragraphStyle: paragraph
    ]
    text.draw(in: rect, withAttributes: attributes)
}

func drawBookMarkIcon(in rect: CGRect, scale: CGFloat = 1) {
    let shadow = NSShadow()
    shadow.shadowColor = NSColor.black.withAlphaComponent(0.22)
    shadow.shadowOffset = CGSize(width: 0, height: -8 * scale)
    shadow.shadowBlurRadius = 18 * scale
    NSGraphicsContext.saveGraphicsState()
    shadow.set()
    fillRoundedRect(
        CGRect(x: rect.minX + 4 * scale, y: rect.minY - 4 * scale, width: rect.width - 8 * scale, height: rect.height - 6 * scale),
        radius: 34 * scale,
        color: NSColor(hex: 0x10242B, alpha: 0.22)
    )
    NSGraphicsContext.restoreGraphicsState()

    let centerX = rect.midX
    let top = rect.maxY - 56 * scale
    let bottom = rect.minY + 72 * scale
    let left = rect.minX + 46 * scale
    let right = rect.maxX - 46 * scale

    fillPath(
        points: [
            CGPoint(x: left, y: top),
            CGPoint(x: centerX, y: top - 24 * scale),
            CGPoint(x: centerX, y: bottom),
            CGPoint(x: left, y: bottom + 26 * scale)
        ],
        color: NSColor(hex: 0xFFF4D8)
    )
    fillPath(
        points: [
            CGPoint(x: right, y: top),
            CGPoint(x: centerX, y: top - 24 * scale),
            CGPoint(x: centerX, y: bottom),
            CGPoint(x: right, y: bottom + 26 * scale)
        ],
        color: NSColor(hex: 0xF2DEB7)
    )
    fillRoundedRect(CGRect(x: centerX - 3 * scale, y: bottom - 6 * scale, width: 6 * scale, height: top - bottom + 10 * scale), radius: 3 * scale, color: NSColor.white.withAlphaComponent(0.55))

    for index in 0..<4 {
        let y = top - 60 * scale - CGFloat(index) * 34 * scale
        drawLine(CGPoint(x: left + 34 * scale, y: y), CGPoint(x: centerX - 28 * scale, y: y + 10 * scale), color: NSColor.white.withAlphaComponent(0.46), width: 5 * scale)
        drawLine(CGPoint(x: centerX + 28 * scale, y: y + 10 * scale), CGPoint(x: right - 34 * scale, y: y), color: NSColor(hex: 0xCFB783, alpha: 0.52), width: 5 * scale)
    }

    fillPath(
        points: [
            CGPoint(x: centerX + 46 * scale, y: top - 4 * scale),
            CGPoint(x: centerX + 96 * scale, y: top + 4 * scale),
            CGPoint(x: centerX + 96 * scale, y: top - 156 * scale),
            CGPoint(x: centerX + 72 * scale, y: top - 134 * scale),
            CGPoint(x: centerX + 46 * scale, y: top - 156 * scale)
        ],
        color: NSColor(hex: 0xD65F45)
    )

    let starCenter = CGPoint(x: left + 74 * scale, y: top + 28 * scale)
    let star = NSBezierPath()
    for i in 0..<10 {
        let radius = (i % 2 == 0 ? 27 : 12) * scale
        let angle = CGFloat(i) * .pi / 5 + .pi / 2
        let point = CGPoint(x: starCenter.x + cos(angle) * radius, y: starCenter.y + sin(angle) * radius)
        i == 0 ? star.move(to: point) : star.line(to: point)
    }
    star.close()
    NSColor(hex: 0xEFAE69).setFill()
    star.fill()
}

func drawAppIcon(output: String) throws {
    let canvas = Canvas(width: 512, height: 512)
    canvas.draw {
        let bounds = CGRect(x: 0, y: 0, width: 512, height: 512)
        NSGradient(colors: [NSColor(hex: 0x203A43), NSColor(hex: 0x2E5B60), NSColor(hex: 0x76B796)])!.draw(in: bounds, angle: 235)
        NSColor(hex: 0xF7E7C6, alpha: 0.13).setFill()
        NSBezierPath(ovalIn: CGRect(x: -80, y: -90, width: 430, height: 260)).fill()
        NSColor(hex: 0x7EC8A7, alpha: 0.24).setFill()
        NSBezierPath(ovalIn: CGRect(x: 248, y: 212, width: 360, height: 360)).fill()
        drawBookMarkIcon(in: CGRect(x: 68, y: 54, width: 376, height: 388), scale: 1)
    }
    try canvas.writePNG(to: output)
}

func drawFeatureGraphic(output: String) throws {
    let canvas = Canvas(width: 1024, height: 500)
    canvas.draw {
        let bounds = CGRect(x: 0, y: 0, width: 1024, height: 500)
        NSGradient(colors: [NSColor(hex: 0x173139), NSColor(hex: 0x2F6A65), NSColor(hex: 0xF2D7A4)])!.draw(in: bounds, angle: 210)

        NSColor(hex: 0xFFF4D8, alpha: 0.18).setFill()
        NSBezierPath(ovalIn: CGRect(x: 620, y: -170, width: 530, height: 420)).fill()
        NSColor(hex: 0x7EC8A7, alpha: 0.18).setFill()
        NSBezierPath(ovalIn: CGRect(x: -160, y: 260, width: 460, height: 320)).fill()

        drawBookMarkIcon(in: CGRect(x: 620, y: 58, width: 300, height: 330), scale: 0.78)

        fillRoundedRect(CGRect(x: 88, y: 315, width: 88, height: 34), radius: 17, color: NSColor(hex: 0xFFF4D8, alpha: 0.22))
        drawText("Reading My Bible", rect: CGRect(x: 88, y: 318, width: 260, height: 28), size: 18, weight: .semibold, color: NSColor(hex: 0xFFF4D8))
        drawText("내 성경 읽기", rect: CGRect(x: 86, y: 214, width: 470, height: 90), size: 58, weight: .bold, color: NSColor.white)
        drawText("내가 가진 성경 파일로 읽고 검색하는 조용한 성경 리더", rect: CGRect(x: 90, y: 154, width: 560, height: 46), size: 25, weight: .regular, color: NSColor(hex: 0xFFF4D8))

        fillRoundedRect(CGRect(x: 92, y: 84, width: 120, height: 34), radius: 17, color: NSColor(hex: 0xFFF4D8, alpha: 0.18))
        drawText("BDF", rect: CGRect(x: 92, y: 88, width: 120, height: 24), size: 16, weight: .semibold, color: NSColor(hex: 0xFFF4D8), alignment: .center)
        fillRoundedRect(CGRect(x: 226, y: 84, width: 120, height: 34), radius: 17, color: NSColor(hex: 0xFFF4D8, alpha: 0.18))
        drawText("LFA", rect: CGRect(x: 226, y: 88, width: 120, height: 24), size: 16, weight: .semibold, color: NSColor(hex: 0xFFF4D8), alignment: .center)
        fillRoundedRect(CGRect(x: 360, y: 84, width: 142, height: 34), radius: 17, color: NSColor(hex: 0xFFF4D8, alpha: 0.18))
        drawText("검색·메모", rect: CGRect(x: 360, y: 88, width: 142, height: 24), size: 16, weight: .semibold, color: NSColor(hex: 0xFFF4D8), alignment: .center)
    }
    try canvas.writePNG(to: output)
}

let root = CommandLine.arguments.dropFirst().first ?? "store-assets"
try FileManager.default.createDirectory(atPath: root, withIntermediateDirectories: true)
try drawAppIcon(output: "\(root)/play-store-icon-512.png")
try drawFeatureGraphic(output: "\(root)/play-store-feature-graphic-1024x500.png")
print("Generated store assets in \(root)")
