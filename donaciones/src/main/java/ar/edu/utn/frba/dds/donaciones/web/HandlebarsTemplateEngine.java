package ar.edu.utn.frba.dds.donaciones.web;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import io.javalin.http.Context;
import io.javalin.rendering.FileRenderer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Motor de plantillas Handlebars integrado con Javalin para el Cliente Liviano MVC (Entrega 4).
 * Carga vistas desde /templates/*.hbs.
 */
public class HandlebarsTemplateEngine implements FileRenderer {

  private final Handlebars handlebars;

  public HandlebarsTemplateEngine() {
    ClassPathTemplateLoader loader = new ClassPathTemplateLoader("/templates", ".hbs");
    this.handlebars = new Handlebars(loader);

    // Helper para comparar igualdad en las plantillas: {{#if (eq estado "ENTREGADA")}}
    this.handlebars.registerHelper("eq", (Helper<Object>) (context, options) -> {
      Object param = options.param(0, null);
      return Objects.equals(String.valueOf(context), String.valueOf(param));
    });
  }

  @NotNull
  @Override
  public String render(@NotNull String filePath, @NotNull Map<String, ?> model, @NotNull Context context) {
    String templateName = filePath.endsWith(".hbs")
        ? filePath.substring(0, filePath.length() - 4)
        : filePath;
    if (templateName.startsWith("/")) {
      templateName = templateName.substring(1);
    }
    try {
      Template template = handlebars.compile(templateName);
      return template.apply(model);
    } catch (IOException e) {
      throw new RuntimeException("Error renderizando plantilla Handlebars: " + filePath, e);
    }
  }
}
