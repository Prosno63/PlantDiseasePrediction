package bd.fasol.common.util;

import bd.fasol.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import javax.imageio.ImageIO;
import java.util.Optional;
import java.util.UUID;

@Component
public class ImageStorage {
    private final Path imageDirectory;

    public ImageStorage(@Value("${app.image-storage-path}") String imageDirectory) {
        this.imageDirectory = Paths.get(imageDirectory);
    }

    public String store(MultipartFile image) {
        try {
            Files.createDirectories(imageDirectory);
            Path target = imageDirectory.resolve(UUID.randomUUID() + "_" + Paths.get(Optional.ofNullable(image.getOriginalFilename()).orElse("image")).getFileName());
            Files.copy(image.getInputStream(), target);
            return target.getFileName().toString();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not save image");
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

    public void delete(String filename) {
        try {
            Files.deleteIfExists(imageDirectory.resolve(Paths.get(filename).getFileName()));
        } catch (IOException e) {
            // Cleanup is best effort; the original request failure is more useful to the caller.
        }
    }
}
