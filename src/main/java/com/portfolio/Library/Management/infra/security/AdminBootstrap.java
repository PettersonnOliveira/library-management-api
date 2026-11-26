package com.portfolio.Library.Management.infra.security;

import com.portfolio.Library.Management.Entities.User;
import com.portfolio.Library.Management.Repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Cria (ou promove) o usuario ADMIN inicial a partir das propriedades
 * app.admin.email / app.admin.password, evitando o problema de "ovo e galinha":
 * o cadastro publico sempre cria USER e /users exige ADMIN.
 */
@Component
public class AdminBootstrap {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminName;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          @Value("${app.admin.name:Administrador}") String adminName,
                          @Value("${app.admin.email:}") String adminEmail,
                          @Value("${app.admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminName = adminName;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void createAdminIfMissing() {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn("app.admin.email/app.admin.password nao configurados: nenhum ADMIN inicial sera criado.");
            return;
        }

        Optional<User> existing = userRepository.findByEmail(adminEmail);
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.getRole() != Role.ADMIN) {
                user.setRole(Role.ADMIN);
                userRepository.save(user);
                log.info("Usuario {} promovido a ADMIN.", adminEmail);
            }
            return;
        }

        User admin = new User(adminName, adminEmail, passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.info("Usuario ADMIN inicial criado: {}", adminEmail);
    }
}
