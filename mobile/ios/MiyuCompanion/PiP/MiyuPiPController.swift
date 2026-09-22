import AVFoundation
import AVKit
import SwiftUI
import UIKit

/// Real Picture-in-Picture for Miyu.
///
/// iOS does not allow a free floating overlay the way Android does, so PiP is the only supported way
/// for a companion to stay visible above other apps. This controller renders the Miyu character into
/// pixel buffers (no camera, no file, no network) and feeds them to an
/// `AVSampleBufferDisplayLayer`, which `AVPictureInPictureController` can present.
@available(iOS 15.0, *)
final class MiyuPiPController: NSObject, AVPictureInPictureControllerDelegate,
                               AVPictureInPictureSampleBufferPlaybackDelegate {

    private let displayLayer = AVSampleBufferDisplayLayer()
    private var pipController: AVPictureInPictureController?
    private var frameTimer: Timer?
    private var frameIndex = 0
    private var mood: String = "cozy"

    var isSupported: Bool { AVPictureInPictureController.isPictureInPictureSupported() }
    var isActive: Bool { pipController?.isPictureInPictureActive ?? false }

    // MARK: - Lifecycle

    func attachDisplayLayer(to hostView: UIView) {
        displayLayer.frame = hostView.bounds
        displayLayer.videoGravity = .resizeAspect
        displayLayer.backgroundColor = UIColor(red: 1.0, green: 0.96, blue: 0.96, alpha: 1).cgColor
        if displayLayer.superlayer == nil {
            hostView.layer.addSublayer(displayLayer)
        }
    }

    func setMood(_ newMood: String) {
        mood = newMood
    }

    func start() {
        guard isSupported else {
            NSLog("Miyu PiP is not supported on this device")
            return
        }
        if pipController == nil {
            let source = AVPictureInPictureController.ContentSource(
                sampleBufferDisplayLayer: displayLayer,
                playbackDelegate: self
            )
            pipController = AVPictureInPictureController(contentSource: source)
            pipController?.delegate = self
        }
        renderFrame()
        startFrameTimer()
        pipController?.startPictureInPicture()
    }

    func stop() {
        pipController?.stopPictureInPicture()
        frameTimer?.invalidate()
        frameTimer = nil
    }

    private func startFrameTimer() {
        frameTimer?.invalidate()
        // ~8 fps of gentle breathing/blinking is plenty for a companion and stays battery friendly.
        frameTimer = Timer.scheduledTimer(withTimeInterval: 0.125, repeats: true) { [weak self] _ in
            self?.renderFrame()
        }
    }

    // MARK: - Frame rendering (local only)

    private func renderFrame() {
        guard displayLayer.isReadyForMoreMediaData else { return }
        let size = CGSize(width: 480, height: 480)
        guard let pixelBuffer = makePixelBuffer(size: size) else { return }
        draw(into: pixelBuffer, size: size)
        guard let sample = makeSampleBuffer(from: pixelBuffer) else { return }
        displayLayer.enqueue(sample)
        frameIndex &+= 1
    }

    private func makePixelBuffer(size: CGSize) -> CVPixelBuffer? {
        var pixelBuffer: CVPixelBuffer?
        let attributes: [String: Any] = [
            kCVPixelBufferCGImageCompatibilityKey as String: true,
            kCVPixelBufferCGBitmapContextCompatibilityKey as String: true,
            kCVPixelBufferIOSurfacePropertiesKey as String: [:]
        ]
        let status = CVPixelBufferCreate(kCFAllocatorDefault,
                                        Int(size.width),
                                        Int(size.height),
                                        kCVPixelFormatType_32BGRA,
                                        attributes as CFDictionary,
                                        &pixelBuffer)
        return status == kCVReturnSuccess ? pixelBuffer : nil
    }

    private func draw(into pixelBuffer: CVPixelBuffer, size: CGSize) {
        CVPixelBufferLockBaseAddress(pixelBuffer, [])
        defer { CVPixelBufferUnlockBaseAddress(pixelBuffer, []) }
        guard let base = CVPixelBufferGetBaseAddress(pixelBuffer) else { return }

        let colorSpace = CGColorSpaceCreateDeviceRGB()
        let bitmapInfo = CGImageAlphaInfo.premultipliedFirst.rawValue | CGBitmapInfo.byteOrder32Little.rawValue
        guard let context = CGContext(data: base,
                                      width: Int(size.width),
                                      height: Int(size.height),
                                      bitsPerComponent: 8,
                                      bytesPerRow: CVPixelBufferGetBytesPerRow(pixelBuffer),
                                      space: colorSpace,
                                      bitmapInfo: bitmapInfo) else { return }

        let width = size.width
        let height = size.height
        context.setFillColor(UIColor(red: 1.0, green: 0.96, blue: 0.96, alpha: 1).cgColor)
        context.fill(CGRect(x: 0, y: 0, width: width, height: height))

        // breathing scale
        let phase = Double(frameIndex) * 0.125
        let breathing = 1.0 + 0.04 * sin(phase)
        let radius = width * 0.34 * breathing

        context.setFillColor(UIColor(red: 0.54, green: 0.37, blue: 0.38, alpha: 1).cgColor)
        context.fillEllipse(in: CGRect(x: width / 2 - radius, y: height / 2 - radius,
                                       width: radius * 2, height: radius * 2))

        // blink every ~4 seconds
        let blinking = Int(phase) % 4 == 0 && phase.truncatingRemainder(dividingBy: 1.0) < 0.2
        context.setFillColor(UIColor(red: 1.0, green: 0.96, blue: 0.96, alpha: 1).cgColor)
        let eyeWidth = width * 0.09
        let eyeHeight = blinking ? height * 0.012 : height * 0.075
        for offset in [-width * 0.12, width * 0.12] {
            context.fillEllipse(in: CGRect(x: width / 2 + offset - eyeWidth / 2,
                                           y: height / 2 - eyeHeight * 0.2,
                                           width: eyeWidth,
                                           height: eyeHeight))
        }

        // smile
        context.setStrokeColor(UIColor(red: 1.0, green: 0.96, blue: 0.96, alpha: 1).cgColor)
        context.setLineWidth(max(3, width * 0.012))
        context.setLineCap(.round)
        context.beginPath()
        context.addArc(center: CGPoint(x: width / 2, y: height / 2 + height * 0.06),
                       radius: width * 0.14,
                       startAngle: .pi * 0.15,
                       endAngle: .pi * 0.85,
                       clockwise: false)
        context.strokePath()

        // mood tint dot
        context.setFillColor(mood == "playful"
                             ? UIColor(red: 1.0, green: 0.72, blue: 0.78, alpha: 1).cgColor
                             : UIColor(red: 0.77, green: 0.91, blue: 0.85, alpha: 1).cgColor)
        context.fillEllipse(in: CGRect(x: width * 0.78, y: height * 0.12,
                                       width: width * 0.09, height: width * 0.09))
    }

    private func makeSampleBuffer(from pixelBuffer: CVPixelBuffer) -> CMSampleBuffer? {
        var formatDescription: CMFormatDescription?
        CMVideoFormatDescriptionCreateForImageBuffer(allocator: kCFAllocatorDefault,
                                                     imageBuffer: pixelBuffer,
                                                     formatDescriptionOut: &formatDescription)
        guard let format = formatDescription else { return nil }
        var timing = CMSampleTimingInfo(
            duration: CMTime(value: 1, timescale: 8),
            presentationTimeStamp: CMClockGetTime(CMClockGetHostTimeClock()),
            decodeTimeStamp: .invalid
        )
        var sampleBuffer: CMSampleBuffer?
        CMSampleBufferCreateReadyWithImageBuffer(allocator: kCFAllocatorDefault,
                                                 imageBuffer: pixelBuffer,
                                                 formatDescription: format,
                                                 sampleTiming: &timing,
                                                 sampleBufferOut: &sampleBuffer)
        return sampleBuffer
    }

    // MARK: - AVPictureInPictureControllerDelegate

    func pictureInPictureControllerDidStartPictureInPicture(_ controller: AVPictureInPictureController) {
        NSLog("Miyu PiP started")
    }

    func pictureInPictureControllerDidStopPictureInPicture(_ controller: AVPictureInPictureController) {
        frameTimer?.invalidate()
        frameTimer = nil
        NSLog("Miyu PiP stopped")
    }

    // MARK: - AVPictureInPictureSampleBufferPlaybackDelegate

    func pictureInPictureController(_ controller: AVPictureInPictureController, setPlaying playing: Bool) {}

    func pictureInPictureController(_ controller: AVPictureInPictureController,
                                    didTransitionToRenderSize newRenderSize: CMVideoDimensions) {}

    func pictureInPictureController(_ controller: AVPictureInPictureController,
                                    skipByInterval skipInterval: CMTime,
                                    completion completionHandler: @escaping () -> Void) {
        completionHandler()
    }

    func pictureInPictureControllerTimeRangeForPlayback(_ controller: AVPictureInPictureController) -> CMTimeRange {
        CMTimeRange(start: .negativeInfinity, duration: .positiveInfinity)
    }

    func pictureInPictureControllerIsPlaybackPaused(_ controller: AVPictureInPictureController) -> Bool {
        false
    }
}

/// SwiftUI bridge that hosts the sample buffer layer used by PiP.
@available(iOS 15.0, *)
struct MiyuPiPHostView: UIViewRepresentable {
    let controller: MiyuPiPController

    func makeUIView(context: Context) -> UIView {
        let view = UIView()
        view.backgroundColor = UIColor(red: 1.0, green: 0.96, blue: 0.96, alpha: 1)
        controller.attachDisplayLayer(to: view)
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        controller.attachDisplayLayer(to: uiView)
    }
}
