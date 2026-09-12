package com.codigitech.ft.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.popoverPresentationController

/** Writes the file to the temp directory and presents the system share sheet from the top view controller. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosFileSharer : FileSharer {
    override fun shareText(fileName: String, mimeType: String, content: String) {
        val path = NSTemporaryDirectory() + fileName
        val written = NSString.create(string = content).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        if (!written) return
        val presenter = topViewController() ?: return
        val activity = UIActivityViewController(activityItems = listOf(NSURL.fileURLWithPath(path)), applicationActivities = null)
        // iPad shows the sheet as a popover and needs an anchor; centring on the presenter is enough.
        activity.popoverPresentationController?.let { popover ->
            popover.sourceView = presenter.view
            val bounds = presenter.view.bounds
            popover.sourceRect = bounds.useContents { CGRectMake(size.width / 2, size.height / 2, 0.0, 0.0) }
        }
        presenter.presentViewController(activity, animated = true, completion = null)
    }

    private fun topViewController(): UIViewController? {
        val window = UIApplication.sharedApplication.windows.filterIsInstance<UIWindow>().firstOrNull { it.isKeyWindow() }
            ?: UIApplication.sharedApplication.keyWindow
        var top = window?.rootViewController ?: return null
        while (top.presentedViewController != null) top = top.presentedViewController!!
        return top
    }
}
