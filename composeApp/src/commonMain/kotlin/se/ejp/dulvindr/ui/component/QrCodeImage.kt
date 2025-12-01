package se.ejp.dulvindr.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.qrose.options.*
import io.github.alexzhirkevich.qrose.rememberQrCodePainter

/**
 * Composable function to display a QR code using qrose library.
 *
 * @param data The data to encode in the QR code
 * @param size The size of the QR code display
 * @param modifier Optional modifier for the composable
 */
@Composable
fun QrCodeImage(
    data: String,
    size: Dp = 250.dp,
    modifier: Modifier = Modifier
) {
    val qrCodePainter = rememberQrCodePainter(data) {
        shapes {
            ball = QrBallShape.square()
            darkPixel = QrPixelShape.square()
            frame = QrFrameShape.square()
        }
        colors {
            dark = QrBrush.solid(Color.Black)
            light = QrBrush.solid(Color.White)
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .background(Color.White)
    ) {
        Image(
            painter = qrCodePainter,
            contentDescription = "QR Code for identity",
            modifier = Modifier.size(size)
        )
    }
}

