package com.jompastech.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Application Health Check Controller.
 *
 * <p>This controller exposes lightweight endpoints used by infrastructure
 * components (load balancers, PaaS providers, container orchestrators)
 * to verify whether the application is running and responsive.</p>
 *
 * <p><b>Important:</b> These endpoints must remain publicly accessible and
 * must not require authentication, as they are consumed by automated
 * systems that do not provide credentials.</p>
 *
 * <p>Health checks should be fast, deterministic, and side-effect free.
 * Avoid calling heavy dependencies or business services in this layer.</p>
 */
@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "Application health check endpoints")
public class HealthController {

    /**
     * Basic liveness probe.
     *
     * <p>Indicates that the application process is up and able to
     * respond to HTTP requests.</p>
     *
     * <p>This endpoint does not validate external dependencies such as
     * databases or third-party services. Its sole purpose is to
     * confirm that the JVM and web server are running.</p>
     *
     * @return HTTP 200 OK if the application is alive
     */
    @GetMapping
    @Operation(
            summary = "Application liveness check",
            description = "Returns HTTP 200 when the application is running and responsive."
    )
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("UP");
    }
}
