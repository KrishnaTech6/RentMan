package com.rentman.sharedui.components

import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

/** The view controller on top, which is where a share sheet or a picker has to be presented from. */
internal fun topViewController(): UIViewController? {
    val scene = UIApplication.sharedApplication.connectedScenes.firstOrNull { it is UIWindowScene } as? UIWindowScene
    var top = scene?.windows?.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true }
        ?.let { (it as UIWindow).rootViewController }
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}

