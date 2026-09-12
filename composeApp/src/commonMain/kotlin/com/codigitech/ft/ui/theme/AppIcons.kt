package com.codigitech.ft.ui.theme

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The handful of Material Symbols the app needs beyond `material-icons-core`, drawn from their
 * 24dp path data. Keeps the ~10MB extended icon pack out of the build.
 */
object AppIcons {
    val SwapHoriz: ImageVector by lazy {
        materialIcon("SwapHoriz") { materialPath { addPath("M6.99 11L3 15l3.99 4v-3H14v-2H6.99v-3zM21 9l-3.99-4v3H10v2h7.01v3L21 9z") } }
    }
    val Bank: ImageVector by lazy {
        materialIcon("Bank") { materialPath { addPath("M4 10v7h3v-7H4zm6 0v7h3v-7h-3zM2 22h19v-3H2v3zm14-12v7h3v-7h-3zm-4.5-9L2 6v2h19V6l-9.5-5z") } }
    }
    val Wallet: ImageVector by lazy {
        materialIcon("Wallet") {
            materialPath {
                addPath("M21 18v1c0 1.1-.9 2-2 2H5c-1.11 0-2-.9-2-2V5c0-1.1.89-2 2-2h14c1.1 0 2 .9 2 2v1h-9c-1.11 0-2 .9-2 2v8c0 1.1.89 2 2 2h9zm-9-2h10V8H12v8zm4-2.5c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5z")
            }
        }
    }
    val CreditCard: ImageVector by lazy {
        materialIcon("CreditCard") {
            materialPath { addPath("M20 4H4c-1.11 0-1.99.89-1.99 2L2 18c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V6c0-1.11-.89-2-2-2zm0 14H4v-6h16v6zm0-10H4V6h16v2z") }
        }
    }
    val Layers: ImageVector by lazy {
        materialIcon("Layers") {
            materialPath { addPath("M11.99 18.54l-7.37-5.73L3 14.07l9 7 9-7-1.63-1.27-7.38 5.74zM12 16l7.36-5.73L21 9l-9-7-9 7 1.63 1.27L12 16z") }
        }
    }
    val BarChart: ImageVector by lazy {
        materialIcon("BarChart") { materialPath { addPath("M5 9.2h3V19H5zM10.6 5h2.8v14h-2.8zm5.6 8H19v6h-2.8z") } }
    }
    val PieChart: ImageVector by lazy {
        materialIcon("PieChart") {
            materialPath { addPath("M11 2v20c-5.07-.5-9-4.79-9-10s3.93-9.5 9-10zm2.03 0v8.99H22c-.47-4.74-4.24-8.52-8.97-8.99zm0 11.01V22c4.74-.47 8.5-4.25 8.97-8.99h-8.97z") }
        }
    }
    val Repeat: ImageVector by lazy {
        materialIcon("Repeat") { materialPath { addPath("M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4z") } }
    }
    val Category: ImageVector by lazy {
        materialIcon("Category") {
            materialPath {
                addPath("M12 2l-5.5 9h11L12 2zm0 3.84L13.93 9h-3.87L12 5.84zM17.5 13c-2.49 0-4.5 2.01-4.5 4.5s2.01 4.5 4.5 4.5 4.5-2.01 4.5-4.5-2.01-4.5-4.5-4.5zm0 7c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5zM3 21.5h8v-8H3v8zm2-6h4v4H5v-4z")
            }
        }
    }
    val Receipt: ImageVector by lazy {
        materialIcon("Receipt") {
            materialPath {
                addPath("M18 17H6v-2h12v2zm0-4H6v-2h12v2zm0-4H6V7h12v2zM3 22l1.5-1.5L6 22l1.5-1.5L9 22l1.5-1.5L12 22l1.5-1.5L15 22l1.5-1.5L18 22l1.5-1.5L21 22V2l-1.5 1.5L18 2l-1.5 1.5L15 2l-1.5 1.5L12 2l-1.5 1.5L9 2 7.5 3.5 6 2 4.5 3.5 3 2v20z")
            }
        }
    }
    val Backspace: ImageVector by lazy {
        materialIcon("Backspace") {
            materialPath {
                addPath("M22 3H7c-.69 0-1.23.35-1.59.88L0 12l5.41 8.11c.36.53.9.89 1.59.89h15c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-3 12.59L17.59 17 14 13.41 10.41 17 9 15.59 12.59 12 9 8.41 10.41 7 14 10.59 17.59 7 19 8.41 15.41 12 19 15.59z")
            }
        }
    }
    val TrendingUp: ImageVector by lazy {
        materialIcon("TrendingUp") { materialPath { addPath("M16 6l2.29 2.29-4.88 4.88-4-4L2 16.59 3.41 18l6-6 4 4 6.3-6.29L22 12V6z") } }
    }
    val TrendingDown: ImageVector by lazy {
        materialIcon("TrendingDown") { materialPath { addPath("M16 18l2.29-2.29-4.88-4.88-4 4L2 7.41 3.41 6l6 6 4-4 6.3 6.29L22 12v6z") } }
    }
    val Tune: ImageVector by lazy {
        materialIcon("Tune") {
            materialPath { addPath("M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z") }
        }
    }
    val Palette: ImageVector by lazy {
        materialIcon("Palette") {
            materialPath {
                addPath("M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9c.83 0 1.5-.67 1.5-1.5 0-.39-.15-.74-.39-1.01-.23-.26-.38-.61-.38-.99 0-.83.67-1.5 1.5-1.5H16c2.76 0 5-2.24 5-5 0-4.42-4.03-8-9-8zm-5.5 9c-.83 0-1.5-.67-1.5-1.5S5.67 9 6.5 9 8 9.67 8 10.5 7.33 12 6.5 12zm3-4C8.67 8 8 7.33 8 6.5S8.67 5 9.5 5s1.5.67 1.5 1.5S10.33 8 9.5 8zm5 0c-.83 0-1.5-.67-1.5-1.5S13.67 5 14.5 5s1.5.67 1.5 1.5S15.33 8 14.5 8zm3 4c-.83 0-1.5-.67-1.5-1.5S16.67 9 17.5 9s1.5.67 1.5 1.5-.67 1.5-1.5 1.5z")
            }
        }
    }
    val ChevronRight: ImageVector by lazy {
        materialIcon("ChevronRight") { materialPath { addPath("M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z") } }
    }
    val Share: ImageVector by lazy {
        materialIcon("Share") {
            materialPath {
                addPath("M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92 1.61 0 2.92-1.31 2.92-2.92s-1.31-2.92-2.92-2.92z")
            }
        }
    }
    val Face: ImageVector by lazy {
        materialIcon("Face") {
            materialPath {
                addPath("M9 11.75c-.69 0-1.25.56-1.25 1.25s.56 1.25 1.25 1.25 1.25-.56 1.25-1.25-.56-1.25-1.25-1.25zm6 0c-.69 0-1.25.56-1.25 1.25s.56 1.25 1.25 1.25 1.25-.56 1.25-1.25-.56-1.25-1.25-1.25zM12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8 0-.29.02-.58.05-.86 2.36-1.05 4.23-2.98 5.21-5.37C11.07 8.33 14.05 10 17.42 10c.78 0 1.53-.09 2.25-.26.21.71.33 1.47.33 2.26 0 4.41-3.59 8-8 8z")
            }
        }
    }
}

/** Parses SVG path data into the receiver path builder. */
private fun androidx.compose.ui.graphics.vector.PathBuilder.addPath(data: String) {
    androidx.compose.ui.graphics.vector.addPathNodes(data).forEach { node -> node.applyTo(this) }
}

private fun androidx.compose.ui.graphics.vector.PathNode.applyTo(b: androidx.compose.ui.graphics.vector.PathBuilder) {
    when (this) {
        is androidx.compose.ui.graphics.vector.PathNode.Close -> b.close()
        is androidx.compose.ui.graphics.vector.PathNode.MoveTo -> b.moveTo(x, y)
        is androidx.compose.ui.graphics.vector.PathNode.RelativeMoveTo -> b.moveToRelative(dx, dy)
        is androidx.compose.ui.graphics.vector.PathNode.LineTo -> b.lineTo(x, y)
        is androidx.compose.ui.graphics.vector.PathNode.RelativeLineTo -> b.lineToRelative(dx, dy)
        is androidx.compose.ui.graphics.vector.PathNode.HorizontalTo -> b.horizontalLineTo(x)
        is androidx.compose.ui.graphics.vector.PathNode.RelativeHorizontalTo -> b.horizontalLineToRelative(dx)
        is androidx.compose.ui.graphics.vector.PathNode.VerticalTo -> b.verticalLineTo(y)
        is androidx.compose.ui.graphics.vector.PathNode.RelativeVerticalTo -> b.verticalLineToRelative(dy)
        is androidx.compose.ui.graphics.vector.PathNode.CurveTo -> b.curveTo(x1, y1, x2, y2, x3, y3)
        is androidx.compose.ui.graphics.vector.PathNode.RelativeCurveTo -> b.curveToRelative(dx1, dy1, dx2, dy2, dx3, dy3)
        is androidx.compose.ui.graphics.vector.PathNode.ReflectiveCurveTo -> b.reflectiveCurveTo(x1, y1, x2, y2)
        is androidx.compose.ui.graphics.vector.PathNode.RelativeReflectiveCurveTo -> b.reflectiveCurveToRelative(dx1, dy1, dx2, dy2)
        is androidx.compose.ui.graphics.vector.PathNode.QuadTo -> b.quadTo(x1, y1, x2, y2)
        is androidx.compose.ui.graphics.vector.PathNode.RelativeQuadTo -> b.quadToRelative(dx1, dy1, dx2, dy2)
        is androidx.compose.ui.graphics.vector.PathNode.ReflectiveQuadTo -> b.reflectiveQuadTo(x, y)
        is androidx.compose.ui.graphics.vector.PathNode.RelativeReflectiveQuadTo -> b.reflectiveQuadToRelative(dx, dy)
        is androidx.compose.ui.graphics.vector.PathNode.ArcTo -> b.arcTo(horizontalEllipseRadius, verticalEllipseRadius, theta, isMoreThanHalf, isPositiveArc, arcStartX, arcStartY)
        is androidx.compose.ui.graphics.vector.PathNode.RelativeArcTo -> b.arcToRelative(horizontalEllipseRadius, verticalEllipseRadius, theta, isMoreThanHalf, isPositiveArc, arcStartDx, arcStartDy)
    }
}
