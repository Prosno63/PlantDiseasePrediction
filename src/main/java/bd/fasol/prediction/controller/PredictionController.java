package bd.fasol.prediction.controller;

import bd.fasol.model.User;
import bd.fasol.common.exception.ApiException;
import bd.fasol.prediction.dto.request.TextPredictionRequest;
import bd.fasol.prediction.dto.response.PredictionResponse;
import bd.fasol.prediction.service.PredictionService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

@RestController
@RequestMapping("/predict")
public class PredictionController {
    private final PredictionService service;

    public PredictionController(PredictionService service) {
        this.service = service;
    }

    @PostMapping("/{crop}/text")
    public PredictionResponse text(@PathVariable String crop,
            @Valid @RequestBody TextPredictionRequest request, Authentication authentication) {
        return service.predictText((User) authentication.getPrincipal(), crop, request);
    }

    @PostMapping("/text")
    public PredictionResponse detectText(@Valid @RequestBody TextPredictionRequest request,
            Authentication authentication) {
        String firstWord = request.text().trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        String crop = firstWord.startsWith("ধান") || firstWord.startsWith("rice")
                ? "Rice"
                : firstWord.startsWith("বেগুন") || firstWord.startsWith("eggplant")
                        ? "Eggplant"
                        : null;
        if (crop == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Text must start with ধান or বেগুন");
        }
        return service.predictText((User) authentication.getPrincipal(), crop, request);
    }

    @PostMapping(value = "/{crop}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PredictionResponse image(@PathVariable String crop,
            @RequestParam MultipartFile image, Authentication authentication) {
        return service.predictImage((User) authentication.getPrincipal(), crop, image);
    }
}
