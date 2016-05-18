package com.vaguehope.morrigan.server.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.util.ChecksumHelper;

public class ImageResizer {

	/**
	 * 1 = Maximum.
	 */
	private static final float QUALITY = 0.9f;

	private static final Object[] LOCK = new Object[0];

	public static File resizeFile (final File inF, final Integer size) throws IOException {
		if (!inF.exists()) throw new IllegalArgumentException("File does not exist: " + inF.getAbsolutePath());
		if (size < 16 || size > 1000) throw new IllegalArgumentException("Invalid size: " + size);

		final File outF = new File(Config.getResizedDir(), ChecksumHelper.md5String(inF.getAbsolutePath()) + "_" + size);
		if (outF.exists() && outF.lastModified() > inF.lastModified()) return outF;

		synchronized (LOCK) {
			return scaleImageToFile(inF, size, outF);
		}
	}

	private static File scaleImageToFile (final File inF, final Integer size, final File outF) throws IOException {
		final BufferedImage inImg = ImageIO.read(inF);

		if (inImg.getWidth() < 1 || inImg.getHeight() < 1) throw new IllegalArgumentException("Image too small: " + inF.getAbsolutePath());

		final int width, height;
		if (inImg.getWidth() == inImg.getHeight()) {
			width = size;
			height = size;
		}
		else if (inImg.getWidth() > inImg.getHeight()) {
			width = size;
			height = (int) (inImg.getHeight() * size / (double) inImg.getWidth());
		}
		else {
			width = (int) (inImg.getWidth() * size / (double) inImg.getHeight());
			height = size;
		}

		final BufferedImage outImg = scaleImage(inImg, width, height);
		writeImageViaTmpFile(outImg, outF);
		return outF;
	}

	private static BufferedImage scaleImage (final BufferedImage inImg, final int width, final int height) {
		final BufferedImage outImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = outImg.createGraphics();
		try {
			g.setComposite(AlphaComposite.Src);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.drawImage(inImg, 0, 0, width, height, Color.BLACK, null);
		}
		finally {
			g.dispose();
		}
		return outImg;
	}

	private static void writeImageViaTmpFile (final BufferedImage outImg, final File f) throws IOException {
		final File ftmp = new File(f.getAbsolutePath() + ".tmp");
		try {
			writeImageToFile(outImg, ftmp);
			if (!ftmp.renameTo(f)) throw new IOException("Failed to rename '" + ftmp.getAbsolutePath() + "' to '" + f.getAbsolutePath() + "'.");
		}
		finally {
			if (ftmp.exists()) ftmp.delete();
		}
	}

	private static void writeImageToFile (final BufferedImage outImg, final File f) throws IOException {
		final ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
		try {
			final ImageWriteParam jpegParams = jpgWriter.getDefaultWriteParam();
			jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			jpegParams.setCompressionQuality(QUALITY);

			final FileImageOutputStream ios = new FileImageOutputStream(f);
			try {
				jpgWriter.setOutput(ios);
				jpgWriter.write(null, new IIOImage(outImg, null, null), jpegParams);
			}
			finally {
				ios.close();
			}
		}
		finally {
			jpgWriter.dispose();
		}
	}

}
