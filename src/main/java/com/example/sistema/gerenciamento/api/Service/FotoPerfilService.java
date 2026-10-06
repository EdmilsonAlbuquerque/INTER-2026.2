package com.example.sistema.gerenciamento.api.Service;

import com.example.sistema.gerenciamento.api.Entity.FotoPerfil;
import com.example.sistema.gerenciamento.api.Exception.BusinessException;
import com.example.sistema.gerenciamento.api.Repository.*;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service @RequiredArgsConstructor
public class FotoPerfilService {
    private final FotoPerfilRepository fotos;
    private final UsuarioRepository usuarios;

    @Transactional
    public String salvar(Long id, MultipartFile arquivo) {
        if (arquivo.isEmpty() || arquivo.getSize() > 5 * 1024 * 1024)
            throw new BusinessException("Escolha uma imagem JPG ou PNG de até 5 MB.");
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(arquivo.getBytes()))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new BusinessException("O arquivo não é uma imagem JPG ou PNG válida.");
            var reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("jpeg"))
                    throw new BusinessException("Use uma imagem JPG ou PNG.");
                reader.setInput(input);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > 16000000)
                    throw new BusinessException("A imagem deve ter no máximo 16 milhões de pixels.");
                var original = reader.read(0);
                double ratio = Math.min(1, 512.0 / Math.max(width, height));
                var resized = new BufferedImage(Math.max(1, (int)(width * ratio)), Math.max(1, (int)(height * ratio)), BufferedImage.TYPE_INT_ARGB);
                var graphics = resized.createGraphics();
                try {
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    graphics.drawImage(original, 0, 0, resized.getWidth(), resized.getHeight(), null);
                } finally { graphics.dispose(); }
                var output = new ByteArrayOutputStream();
                ImageIO.write(resized, "png", output);
                var foto = new FotoPerfil(); foto.setUsuarioId(id); foto.setImagem(output.toByteArray()); fotos.save(foto);
                String url = "/auth/me/foto?v=" + UUID.randomUUID();
                usuarios.findById(id).orElseThrow().setFotoUrl(url);
                return url;
            } finally { reader.dispose(); }
        } catch (IOException exception) { throw new BusinessException("Não foi possível ler a imagem. Escolha outro arquivo."); }
    }

    @Transactional
    public void remover(Long id) {
        fotos.findById(id).ifPresent(fotos::delete);
        usuarios.findById(id).orElseThrow().setFotoUrl(null);
    }
}
