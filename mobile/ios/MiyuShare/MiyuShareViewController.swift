import UIKit
import UniformTypeIdentifiers

/// Share Sheet extension: send text to Miyu from Safari, Notes, Messages and any other app.
///
/// Privacy rules kept deliberately strict:
///  - only `public.text` / `public.plain-text` items are read;
///  - the shared text is handed to the Miyu app through a custom URL scheme;
///  - nothing is uploaded anywhere and no other item type is inspected.
final class MiyuShareViewController: UIViewController {

    private let statusLabel = UILabel()
    private let shareButton = UIButton(type: .system)

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.systemBackground

        statusLabel.text = "هيتم إرسال النص إلى Miyu · Text will be sent to Miyu"
        statusLabel.textAlignment = .center
        statusLabel.numberOfLines = 0
        statusLabel.font = .preferredFont(forTextStyle: .body)

        shareButton.setTitle("افتحي Miyu · Open Miyu", for: .normal)
        shareButton.titleLabel?.font = .preferredFont(forTextStyle: .headline)
        shareButton.addTarget(self, action: #selector(sendToMiyu), for: .touchUpInside)

        let stack = UIStackView(arrangedSubviews: [statusLabel, shareButton])
        stack.axis = .vertical
        stack.spacing = 16
        stack.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            stack.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            stack.leadingAnchor.constraint(greaterThanOrEqualTo: view.leadingAnchor, constant: 24),
            stack.trailingAnchor.constraint(lessThanOrEqualTo: view.trailingAnchor, constant: -24)
        ])
    }

    @objc private func sendToMiyu() {
        loadSharedText { [weak self] text in
            guard let self else { return }
            guard let text, !text.isEmpty else {
                self.statusLabel.text = "لا يوجد نص لمشاركته · Nothing to share"
                return
            }
            var components = URLComponents()
            components.scheme = "miyu"
            components.host = "share"
            components.queryItems = [URLQueryItem(name: "text", value: String(text.prefix(2000)))]
            if let url = components.url {
                self.openURL(url)
            } else {
                self.statusLabel.text = "تعذر فتح Miyu · Could not open Miyu"
            }
        }
    }

    private func loadSharedText(completion: @escaping (String?) -> Void) {
        let providers = (extensionContext?.inputItems as? [NSExtensionItem])?
            .flatMap { $0.attachments ?? [] } ?? []

        for provider in providers where provider.hasItemConformingToTypeIdentifier(UTType.plainText.identifier) {
            provider.loadItem(forTypeIdentifier: UTType.plainText.identifier, options: nil) { item, _ in
                var text: String?
                if let string = item as? String {
                    text = string
                } else if let url = item as? URL {
                    text = url.absoluteString
                } else if let data = item as? Data {
                    text = String(data: data, encoding: .utf8)
                }
                DispatchQueue.main.async { completion(text) }
            }
            return
        }
        completion(nil)
    }

    @objc private func openURL(_ url: URL) {
        var responder: UIResponder? = self
        while let current = responder {
            if let application = current as? UIApplication {
                application.open(url, options: [:], completionHandler: nil)
                extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
                return
            }
            responder = current.next
        }
        extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
    }
}
