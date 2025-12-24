package com.jompastech.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.jompastech.backend.config.CloudinaryConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(CloudinaryConfig cloudinaryConfig) {
        // Configura o cliente Cloudinary usando as propriedades carregadas
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudinaryConfig.getCloudName(),
                "api_key", cloudinaryConfig.getApiKey(),
                "api_secret", cloudinaryConfig.getApiSecret(),
                "secure", cloudinaryConfig.isSecure()
        ));
    }

    /**
     * Faz o upload de uma única imagem para o Cloudinary.
     * @param file O arquivo de imagem a ser enviado.
     * @return A URL pública da imagem no Cloudinary.
     * @throws IOException Se ocorrer um erro durante o upload.
     */
    public String uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("O arquivo da imagem não pode ser nulo ou vazio.");
        }

        // Realiza o upload. As opções podem ser ajustadas conforme necessidade.
        Map<String, Object> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "timoneiro/boats", // Organiza as imagens em uma pasta
                        "public_id", "boat_" + System.currentTimeMillis() + "_" + file.getOriginalFilename(),
                        "overwrite", false
                )
        );

        // A URL segura (HTTPS) da imagem é retornada no resultado.
        return uploadResult.get("secure_url").toString();
    }

    /**
     * Faz o upload de múltiplas imagens para o Cloudinary.
     * @param files Lista de arquivos de imagem.
     * @return Lista de URLs públicas das imagens enviadas.
     * @throws IOException Se ocorrer um erro durante o upload de qualquer imagem.
     */
    public List<String> uploadImages(List<MultipartFile> files) throws IOException {
        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String url = uploadImage(file);
                imageUrls.add(url);
            }
        }
        return imageUrls;
    }

    /**
     * Exclui uma imagem do Cloudinary com base no seu public_id.
     * @param publicId O identificador público da imagem no Cloudinary.
     * @throws IOException Se ocorrer um erro durante a exclusão.
     */
    public void deleteImage(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}