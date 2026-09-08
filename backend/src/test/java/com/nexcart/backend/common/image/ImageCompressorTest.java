package com.nexcart.backend.common.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexcart.backend.common.exception.BadRequestException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImageCompressorTest {

	private final ImageCompressor compressor = new ImageCompressor();

	@Test
	void resizesLargeImageAndMeaningfullyReducesFileSize() throws Exception {
		// Solid-color images compress trivially either way — use noise so the size reduction
		// actually demonstrates the resize+recompress working, not just JPEG's baseline entropy coding.
		byte[] originalBytes = noisyPng(3000, 2000);

		ImageCompressor.CompressedImage result = compressor.compress(
			new MockMultipartFile("file", "big-photo.png", "image/png", originalBytes));

		BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.bytes()));
		assertThat(Math.max(decoded.getWidth(), decoded.getHeight())).isLessThanOrEqualTo(1600);
		assertThat(result.contentType()).isEqualTo("image/jpeg");
		assertThat(result.extension()).isEqualTo("jpg");
		// A noisy 3000x2000 PNG is large; resized+recompressed JPEG should be a fraction of it.
		assertThat(result.bytes().length).isLessThan(originalBytes.length / 3);
	}

	@Test
	void doesNotUpscaleAnImageSmallerThanTheCap() throws Exception {
		byte[] originalBytes = noisyPng(200, 150);

		ImageCompressor.CompressedImage result = compressor.compress(
			new MockMultipartFile("file", "small-photo.png", "image/png", originalBytes));

		BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.bytes()));
		assertThat(decoded.getWidth()).isEqualTo(200);
		assertThat(decoded.getHeight()).isEqualTo(150);
	}

	@Test
	void rejectsBytesThatArentARealImage() {
		MockMultipartFile file = new MockMultipartFile("file", "fake.jpg", "image/jpeg", "not-an-image".getBytes());

		assertThatThrownBy(() -> compressor.compress(file))
			.isInstanceOf(BadRequestException.class)
			.satisfies(exception -> assertThat(((BadRequestException) exception).getErrorCode().name())
				.isEqualTo("INVALID_IMAGE_FILE"));
	}

	@Test
	void passesWebpThroughUnchanged() throws Exception {
		byte[] originalBytes = "not-a-real-webp-but-doesnt-matter".getBytes();
		MockMultipartFile file = new MockMultipartFile("file", "photo.webp", "image/webp", originalBytes);

		ImageCompressor.CompressedImage result = compressor.compress(file);

		assertThat(result.bytes()).isEqualTo(originalBytes);
		assertThat(result.contentType()).isEqualTo("image/webp");
		assertThat(result.extension()).isEqualTo("webp");
	}

	@Test
	void flattensTransparentPngWithoutThrowing() throws Exception {
		BufferedImage source = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < source.getWidth(); x++) {
			for (int y = 0; y < source.getHeight(); y++) {
				source.setRGB(x, y, 0x00000000); // fully transparent
			}
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(source, "png", out);

		ImageCompressor.CompressedImage result = compressor.compress(
			new MockMultipartFile("file", "transparent.png", "image/png", out.toByteArray()));

		BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.bytes()));
		// JPEG has no alpha; a fully-transparent source should flatten to (near-)white, not black.
		int corner = decoded.getRGB(0, 0);
		int red = (corner >> 16) & 0xFF;
		assertThat(red).isGreaterThan(200);
	}

	private byte[] noisyPng(int width, int height) throws Exception {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Random random = new Random(42);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				image.setRGB(x, y, random.nextInt(0xFFFFFF));
			}
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "png", out);
		return out.toByteArray();
	}
}
