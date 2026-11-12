package com.atelier.virtualstylist.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

object AtelierIcons {
    private fun icon(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = addPathNodes(pathData),
            fill = SolidColor(Color.Black)
        ).build()

    val Style: ImageVector by lazy {
        icon(
            "Style",
            "M2.53,19.65l1.34,0.56v-9.03l-2.43,5.86c-0.41,1.02 0.08,2.19 1.09,2.61zM22.03,15.95L17.07,3.98c-0.31,-0.75 -1.04,-1.21 -1.81,-1.23c-0.26,0 -0.53,0.04 -0.79,0.15L7.1,5.95c-0.75,0.31 -1.21,1.03 -1.23,1.8c-0.01,0.27 0.04,0.54 0.15,0.8l4.96,11.97c0.31,0.76 1.05,1.22 1.83,1.23c0.26,0 0.52,-0.05 0.77,-0.15l7.36,-3.05c1.02,-0.42 1.51,-1.59 1.09,-2.6zM12.83,19.75L7.87,7.79l7.35,-3.04h0.01l4.95,11.95l-7.35,3.05z"
        )
    }

    val Canvas: ImageVector by lazy {
        icon(
            "Canvas",
            "M3,5v14c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2L21,5c0,-1.1 -0.9,-2 -2,-2L5,3c-1.11,0 -2,0.9 -2,2zM19,19L5,19L5,5h14v14zM11,7h2v10h-2zM7,11h10v2L7,13z"
        )
    }

    val Wardrobe: ImageVector by lazy {
        icon(
            "Wardrobe",
            "M21.6,18.2L13,11.75v-0.91c1.65,-0.49 2.8,-2.17 2.43,-4.05c-0.26,-1.31 -1.3,-2.4 -2.61,-2.7C10.54,3.57 8.5,5.3 8.5,7.5h2c0,-0.83 0.67,-1.5 1.5,-1.5s1.5,0.67 1.5,1.5c0,0.84 -0.69,1.52 -1.53,1.5c-0.54,-0.01 -0.97,0.45 -0.97,0.99v1.76L2.4,18.2C1.63,18.78 2.04,20 3,20h18c0.96,0 1.37,-1.22 0.6,-1.8zM6,18l6,-4.5l6,4.5H6z"
        )
    }

    val AddPhoto: ImageVector by lazy {
        icon(
            "AddPhoto",
            "M19,7v2.99s-1.99,0.01 -2,0V7h-3s0.01,-1.99 0,-2h3V2h2v3h3v2h-3zM16,11L16,8h-3L13,5L5,5c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2v-8h-3zM5,19l3,-4 2,3 3,-4 4,5L5,19z"
        )
    }

    val Refresh: ImageVector by lazy {
        icon(
            "Refresh",
            "M17.65,6.35C16.2,4.9 14.21,4 12,4c-4.42,0 -7.99,3.58 -7.99,8s3.57,8 7.99,8c3.73,0 6.84,-2.55 7.73,-6h-2.08c-0.82,2.33 -3.04,4 -5.65,4 -3.31,0 -6,-2.69 -6,-6s2.69,-6 6,-6c1.66,0 3.14,0.69 4.22,1.78L13,11h7V4l-2.35,2.35z"
        )
    }

    val Download: ImageVector by lazy {
        icon(
            "Download",
            "M19,9h-4V3H9v6H5l7,7 7,-7zM5,18v2h14v-2H5z"
        )
    }

    val BookmarkBorder: ImageVector by lazy {
        icon(
            "BookmarkBorder",
            "M17,3H7c-1.1,0 -2,0.9 -2,2v16l7,-3 7,3V5c0,-1.1 -0.9,-2 -2,-2zM17,18l-5,-2.18L7,18V5h10v13z"
        )
    }

    val Delete: ImageVector by lazy {
        icon(
            "Delete",
            "M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM8,9h8v10H8V9zM15.5,4l-1,-1h-5l-1,1H5v2h14V4z"
        )
    }

    val OpenInNew: ImageVector by lazy {
        icon(
            "OpenInNew",
            "M19,19H5V5h7V3H5c-1.11,0 -2,0.9 -2,2v14c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2v-7h-2v7zM14,3v2h3.59l-9.83,9.83 1.41,1.41L19,6.41V10h2V3h-7z"
        )
    }

    val Lightbulb: ImageVector by lazy {
        icon(
            "Lightbulb",
            "M9,21c0,0.55 0.45,1 1,1h4c0.55,0 1,-0.45 1,-1v-1H9v1zM12,2C8.14,2 5,5.14 5,9c0,2.38 1.19,4.47 3,5.74V17c0,0.55 0.45,1 1,1h6c0.55,0 1,-0.45 1,-1v-2.26c1.81,-1.27 3,-3.36 3,-5.74 0,-3.86 -3.14,-7 -7,-7zM14.85,13.1l-0.85,0.6V16h-4v-2.3l-0.85,-0.6C7.8,12.16 7,10.63 7,9c0,-2.76 2.24,-5 5,-5s5,2.24 5,5c0,1.63 -0.8,3.16 -2.15,4.1z"
        )
    }

    val Copy: ImageVector by lazy {
        icon(
            "Copy",
            "M16,1H4c-1.1,0 -2,0.9 -2,2v14h2V3h12V1zM19,5H8c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h11c1.1,0 2,-0.9 2,-2V7c0,-1.1 -0.9,-2 -2,-2zM19,21H8V7h11v14z"
        )
    }

    val Close: ImageVector by lazy {
        icon(
            "Close",
            "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z"
        )
    }

    val Edit: ImageVector by lazy {
        icon(
            "Edit",
            "M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25zM20.71,7.04c0.39,-0.39 0.39,-1.02 0,-1.41l-2.34,-2.34c-0.39,-0.39 -1.02,-0.39 -1.41,0l-1.83,1.83 3.75,3.75 1.83,-1.83z"
        )
    }

    val Check: ImageVector by lazy {
        icon(
            "Check",
            "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z"
        )
    }

    val Compare: ImageVector by lazy {
        icon(
            "Compare",
            "M10,3H5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h5v-2H5V5h5V3zM19,3h-5v2h5v14h-5v2h5c1.1,0 2,-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2zM11,1h2v22h-2V1z"
        )
    }

    val Swap: ImageVector by lazy {
        icon(
            "Swap",
            "M6.99,11L3,15l3.99,4v-3H14v-2H6.99v-3zM21,9l-3.99,-4v3H10v2h7.01v3L21,9z"
        )
    }
}
