package com.keepcoding.domain.enums;

/**
 * Linguagens suportadas pelo editor e pelo sandbox.
 *
 * <p>Os identificadores são case-insensitive na deserialização do Jackson
 * porque {@code spring.jackson.serialization.write-enums-using-to-string=true}
 * não está habilitado — então o cliente DEVE enviar o nome exatamente como
 * declarado abaixo.</p>
 *
 * <p>As metadados (rótulo de exibição, URL de documentação e código inicial)
 * ficam no catálogo do frontend ({@code data/languages.catalog.ts}) — o
 * backend só precisa do identificador para validação e roteamento.</p>
 */
public enum Language {
    JAVA,
    TYPESCRIPT,
    CPP,
    PYTHON,
    PYTHON3,
    JAVASCRIPT,
    CSHARP,
    C,
    GO,
    KOTLIN,
    SWIFT,
    RUST,
    RUBY,
    PHP,
    DART,
    SCALA,
    ELIXIR,
    ERLANG,
    RACKET
}
