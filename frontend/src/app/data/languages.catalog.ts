/**
 * Catálogo central das linguagens suportadas pelo editor.
 *
 * Fonte única de verdade pra:
 *  - dropdown de seleção do editor;
 *  - link "📖 Docs" da linguagem ativa;
 *  - código inicial sugerido (starter);
 *  - mapeamento {@code id} (uso interno do editor, lowercase ao estilo Monaco)
 *    -> {@code api} (valor enviado ao backend, casando com o enum
 *    {@code com.keepcoding.domain.enums.Language}).
 *
 * Pra adicionar uma nova linguagem: acrescente uma entrada aqui E o valor
 * correspondente no enum {@code Language} do backend.
 */
export interface LanguageMeta {
  /** ID interno (lowercase) usado pelo editor. */
  readonly id: string;
  /** Valor enviado ao backend (espelha o enum Language do Spring). */
  readonly api: string;
  /** Rótulo exibido ao usuário. */
  readonly label: string;
  /** URL da documentação oficial. */
  readonly docsUrl: string;
  /** Código inicial sugerido no editor ao trocar de linguagem. */
  readonly starter: string;
}

export const LANGUAGE_CATALOG: readonly LanguageMeta[] = [
  {
    id: 'cpp',
    api: 'CPP',
    label: 'C++',
    docsUrl: 'https://en.cppreference.com/w/',
    starter: `#include <iostream>
using namespace std;

int main() {
    // resolva o problema em C++
    return 0;
}
`,
  },
  {
    id: 'java',
    api: 'JAVA',
    label: 'Java',
    docsUrl: 'https://docs.oracle.com/en/java/javase/21/docs/api/',
    starter: `public class Solution {
    // resolva o problema em Java
}
`,
  },
  {
    id: 'python3',
    api: 'PYTHON3',
    label: 'Python 3',
    docsUrl: 'https://docs.python.org/3/',
    starter: `def solution():
    # resolva o problema em Python 3
    pass
`,
  },
  {
    id: 'python',
    api: 'PYTHON',
    label: 'Python',
    docsUrl: 'https://docs.python.org/3/',
    starter: `def solution():
    # resolva o problema em Python
    pass
`,
  },
  {
    id: 'javascript',
    api: 'JAVASCRIPT',
    label: 'JavaScript',
    docsUrl: 'https://developer.mozilla.org/en-US/docs/Web/JavaScript',
    starter: `// resolva o problema em JavaScript
function solution() {
}
`,
  },
  {
    id: 'typescript',
    api: 'TYPESCRIPT',
    label: 'TypeScript',
    docsUrl: 'https://www.typescriptlang.org/docs/',
    starter: `// resolva o problema em TypeScript
function solution(): void {
}
`,
  },
  {
    id: 'csharp',
    api: 'CSHARP',
    label: 'C#',
    docsUrl: 'https://learn.microsoft.com/en-us/dotnet/csharp/',
    starter: `public class Solution {
    // resolva o problema em C#
}
`,
  },
  {
    id: 'c',
    api: 'C',
    label: 'C',
    docsUrl: 'https://en.cppreference.com/w/c',
    starter: `#include <stdio.h>

int main(void) {
    // resolva o problema em C
    return 0;
}
`,
  },
  {
    id: 'go',
    api: 'GO',
    label: 'Go',
    docsUrl: 'https://pkg.go.dev/std',
    starter: `package main

func main() {
    // resolva o problema em Go
}
`,
  },
  {
    id: 'kotlin',
    api: 'KOTLIN',
    label: 'Kotlin',
    docsUrl: 'https://kotlinlang.org/docs/home.html',
    starter: `fun main() {
    // resolva o problema em Kotlin
}
`,
  },
  {
    id: 'swift',
    api: 'SWIFT',
    label: 'Swift',
    docsUrl: 'https://www.swift.org/documentation/',
    starter: `// resolva o problema em Swift
func solve() {
}
`,
  },
  {
    id: 'rust',
    api: 'RUST',
    label: 'Rust',
    docsUrl: 'https://doc.rust-lang.org/std/',
    starter: `fn main() {
    // resolva o problema em Rust
}
`,
  },
  {
    id: 'ruby',
    api: 'RUBY',
    label: 'Ruby',
    docsUrl: 'https://docs.ruby-lang.org/en/',
    starter: `# resolva o problema em Ruby
def solve
end
`,
  },
  {
    id: 'php',
    api: 'PHP',
    label: 'PHP',
    docsUrl: 'https://www.php.net/manual/en/',
    starter: `<?php
// resolva o problema em PHP
function solve() {
}
`,
  },
  {
    id: 'dart',
    api: 'DART',
    label: 'Dart',
    docsUrl: 'https://dart.dev/guides',
    starter: `void main() {
  // resolva o problema em Dart
}
`,
  },
  {
    id: 'scala',
    api: 'SCALA',
    label: 'Scala',
    docsUrl: 'https://docs.scala-lang.org/',
    starter: `object Solution {
  def main(args: Array[String]): Unit = {
    // resolva o problema em Scala
  }
}
`,
  },
  {
    id: 'elixir',
    api: 'ELIXIR',
    label: 'Elixir',
    docsUrl: 'https://hexdocs.pm/elixir/',
    starter: `defmodule Solution do
  # resolva o problema em Elixir
  def solve, do: nil
end
`,
  },
  {
    id: 'erlang',
    api: 'ERLANG',
    label: 'Erlang',
    docsUrl: 'https://www.erlang.org/docs',
    starter: `-module(solution).
-export([solve/0]).

solve() ->
    %% resolva o problema em Erlang
    ok.
`,
  },
  {
    id: 'racket',
    api: 'RACKET',
    label: 'Racket',
    docsUrl: 'https://docs.racket-lang.org/',
    starter: `#lang racket
;; resolva o problema em Racket
`,
  },
] as const;

/** Tipo computado a partir do catálogo: lista válida de IDs do editor. */
export type EditorLanguage = (typeof LANGUAGE_CATALOG)[number]['id'];

/** Tipo computado a partir do catálogo: lista válida de IDs enviados ao backend. */
export type ApiLanguage = (typeof LANGUAGE_CATALOG)[number]['api'];

/** Lookup helper; lança se id não existir no catálogo. */
export function metaFor(id: EditorLanguage): LanguageMeta {
  const found = LANGUAGE_CATALOG.find((l) => l.id === id);
  if (!found) {
    throw new Error(`Linguagem desconhecida: ${id}`);
  }
  return found;
}

/** Padrão usado quando nenhum estado anterior define a linguagem. */
export const DEFAULT_EDITOR_LANGUAGE: EditorLanguage = 'java';
