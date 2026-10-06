package com.example.sistema.gerenciamento.api.Controller;

import com.example.sistema.gerenciamento.api.Repository.FotoPerfilRepository;
import com.example.sistema.gerenciamento.api.Security.UsuarioAutenticado;
import com.example.sistema.gerenciamento.api.Service.FotoPerfilService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequiredArgsConstructor @RequestMapping("/auth/me/foto")
public class FotoPerfilController {
    private final FotoPerfilService service;
    private final FotoPerfilRepository repository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> enviar(@AuthenticationPrincipal UsuarioAutenticado user, @RequestParam MultipartFile arquivo) {
        return Map.of("fotoUrl", service.salvar(user.getId(), arquivo));
    }

    @GetMapping
    public ResponseEntity<byte[]> imagem(@AuthenticationPrincipal UsuarioAutenticado user) {
        return repository.findById(user.getId()).map(foto -> ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(MediaType.IMAGE_PNG).body(foto.getImagem())).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@AuthenticationPrincipal UsuarioAutenticado user) { service.remover(user.getId()); }
}
