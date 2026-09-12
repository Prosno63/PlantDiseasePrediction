package bd.fasol.common.util;

import bd.fasol.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

@Component
public class ImageStorage {
    public byte[] store(MultipartFile image) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
                gzip.write(image.getBytes());
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read image");
        }
    }

    public void validate(MultipartFile image) {
        if (image == null || image.isEmpty())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Image is required");
        if (image.getSize() > 5 * 1024 * 1024 || image.getContentType() == null || !image.getContentType().startsWith("image/"))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Upload a valid image under 5 MB");
        try {
            if (ImageIO.read(image.getInputStream()) == null)
                throw new ApiException(HttpStatus.BAD_REQUEST, "Upload a readable image");
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Upload a readable image");
        }
    }

}
