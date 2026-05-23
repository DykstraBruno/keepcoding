export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';

/** Resumo de um problema para listagem no dashboard. */
export interface ProblemSummary {
  id: number;
  title: string;
  difficulty: Difficulty;
}

/** Caso de teste de um problema. */
export interface TestCase {
  id: number;
  input: string;
  expectedOutput: string;
  isSample: boolean;
}

/** Desafio de algoritmo. */
export interface Problem {
  id: number;
  title: string;
  description: string;
  difficulty: Difficulty;
  timeLimit: number;
  memoryLimit: number;
  testCases: TestCase[];
}
