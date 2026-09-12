package com.codigitech.ft

/**
 * Brand constants. The launcher label lives in platform resources (strings.xml / Config.xcconfig);
 * everything the shared code shows to the user comes from here so a rename is a one-line change.
 * Palette and logo rules: docs/branding/brand.md.
 */
object Brand {
    const val APP_NAME = "Pocketsum"
    const val TAGLINE = "Simple, offline money tracking."
    /** Keep in sync with versionName in composeApp/build.gradle.kts and CFBundleShortVersionString. */
    const val VERSION = "1.0.0"
    /**
     * Public URL of docs/branding/store/privacy-policy.md once it is hosted. The Settings > About
     * link only appears when this is non-blank; Play requires the same URL in the store listing.
     */
    const val PRIVACY_POLICY_URL = ""
}
