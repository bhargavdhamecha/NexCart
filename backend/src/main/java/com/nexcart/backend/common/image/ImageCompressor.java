package com.nexcart.backend.common.image;

import com.nexcart.backend.common.exception.BadRequestException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Resizes + re-encodes uploaded product photos before they're stored: caps the longest edge at
 * {@link #MAX_DIMENSION}px and re-encodes as JPEG at {@link #OUTPUT_QUALITY} quality. This is
 * "visually lossless" (imperceptible for a product photo), not bit-for-bit lossless — that's the
 * deliberate trade-off, since true lossless re-optimization only saves ~10-30% while this saves
 * ~50-80%, which is what actually matters for a free-tier (10GB) B2 bucket. Never upscales an
 * image already smaller than the cap (Thumbnailator's default behavior).
 *
 * <p>WEBP uploads are stored unchanged: the JDK's built-in ImageIO has no WEBP reader, and adding
 * one means pulling in a native-backed codec library — not worth it for what's a rare *upload*
 * format in practice (WEBP is far more common as a serving format than a source photo format).
 */
@Component
public class ImageCompressor {

	private static final int MAX_DIMENSION = 1600;
	private static final float OUTPUT_QUALITY = 0.85f;
	private static final String WEBP_CONTENT_TYPE = "image/webp";

	public CompressedImage compress(MultipartFile file) {
		String contentType = file.getContentType();
		if (WEBP_CONTENT_TYPE.equals(contentType)) {
			return passThrough(file);
		}
		return resizeAndRecompress(file);
	}

	private CompressedImage resizeAndRecompress(MultipartFile file) {
		BufferedImage source = readImage(file);
		if (source == null) {
			throw BadRequestException.invalidImageFile("Uploaded file is not a valid image");
		}

		// Thumbnailator's .size(w, h) scales UP to fit the box by default — there's no "never
		// enlarge" flag on the builder, so the target size is computed explicitly here with the
		// scale factor capped at 1.0, to guarantee small images are never upscaled.
		double scale = Math.min(1.0, (double) MAX_DIMENSION / Math.max(source.getWidth(), source.getHeight()));
		int targetWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
		int targetHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			Thumbnails.of(flattenTransparency(source))
				.size(targetWidth, targetHeight)
				.outputQuality(OUTPUT_QUALITY)
				.outputFormat("jpg")
				.toOutputStream(output);
		}
		catch (IOException exception) {
			throw new UncheckedIOException("Failed to compress image", exception);
		}

		return new CompressedImage(output.toByteArray(), "image/jpeg", "jpg");
	}

	private CompressedImage passThrough(MultipartFile file) {
		try {
			return new CompressedImage(file.getBytes(), WEBP_CONTENT_TYPE, "webp");
		}
		catch (IOException exception) {
			throw new UncheckedIOException("Failed to read uploaded file", exception);
		}
	}

	private BufferedImage readImage(MultipartFile file) {
		try {
			return ImageIO.read(file.getInputStream());
		}
		catch (IOException exception) {
			throw new UncheckedIOException("Failed to read uploaded file", exception);
		}
	}

	// JPEG has no alpha channel — a transparent PNG encoded straight to JPEG can render its
	// transparent pixels as black on some codecs. Flatten onto a white background first.
	private BufferedImage flattenTransparency(BufferedImage source) {
		if (!source.getColorModel().hasAlpha()) {
			return source;
		}
		BufferedImage flattened = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = flattened.createGraphics();
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
		graphics.drawImage(source, 0, 0, null);
		graphics.dispose();
		return flattened;
	}

	public record CompressedImage(byte[] bytes, String contentType, String extension) {
	}
}
