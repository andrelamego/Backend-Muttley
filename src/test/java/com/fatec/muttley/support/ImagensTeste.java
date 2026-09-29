package com.fatec.muttley.support;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public final class ImagensTeste {
    private ImagensTeste() {}
    public static byte[] criar(String formato) {
        try {
            var bytes = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB), formato, bytes);
            return bytes.toByteArray();
        } catch (IOException e) { throw new IllegalStateException(e); }
    }
}
