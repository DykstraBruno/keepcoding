import { Component, ViewChild, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ConsentService } from '../../services/consent.service';
import { CookiePreferencesComponent } from '../consent/cookie-preferences.component';

/**
 * Página /privacidade — política LGPD do KeepCoding.
 *
 * Texto descreve o que é coletado, finalidade, base legal, retenção,
 * compartilhamento e direitos do titular. Inclui controles para revogar
 * o consentimento e reabrir o modal de preferências.
 */
@Component({
  selector: 'app-privacy-policy',
  imports: [RouterLink, CookiePreferencesComponent],
  template: `
    <div class="page">
      <header class="page__head">
        <a routerLink="/" class="back">← voltar</a>
        <h1>Política de Privacidade</h1>
        <p class="updated">Última atualização: 26/06/2026</p>
      </header>

      <section>
        <h2>1. Controlador</h2>
        <p>
          KeepCoding é um projeto educacional. O controlador dos dados pessoais
          tratados nesta aplicação é o operador do projeto. Para exercer
          direitos previstos na LGPD (Lei 13.709/2018), entre em contato pelo
          e-mail informado no rodapé ou diretamente no
          <a
            href="https://github.com/DykstraBruno/keepcoding"
            target="_blank"
            rel="noopener">repositório GitHub</a>.
        </p>
      </section>

      <section>
        <h2>2. Dados coletados</h2>
        <p>Tratamos apenas o estritamente necessário para o funcionamento:</p>
        <ul>
          <li>
            <b>Identificação (autenticação Supabase):</b> e-mail e nome
            fornecidos pelo provedor social escolhido (Google, GitHub etc.) no
            momento do login. A sessão é mantida em <code>localStorage</code>
            no seu navegador.
          </li>
          <li>
            <b>Chave de IA do usuário (BYOK):</b> se você optar por usar sua
            própria chave de API (OpenAI ou Google), ela é armazenada apenas no
            <code>localStorage</code> do seu navegador e enviada por header em
            cada chamada para o backend, que a repassa para o provedor sem
            persistir.
          </li>
          <li>
            <b>Conteúdo gerado:</b> submissões de código, entrevistas e
            diagramas que você cria ficam vinculados ao seu usuário no banco
            do projeto para alimentar XP, ranking e histórico.
          </li>
          <li>
            <b>Preferências de consentimento:</b> sua escolha de cookies é
            gravada em <code>localStorage</code> (chave
            <code>keepcoding.lgpd-consent.v1</code>).
          </li>
        </ul>
        <p>
          <b>Não usamos</b> cookies de marketing, cookies de terceiros, pixels
          publicitários ou ferramentas de analytics como Google Analytics.
        </p>
      </section>

      <section>
        <h2>3. Finalidade e base legal</h2>
        <ul>
          <li>
            <b>Autenticação e operação do serviço</b> — base legal:
            <i>execução de contrato</i> (art. 7º, V) e <i>legítimo interesse</i>
            para manter a aplicação segura.
          </li>
          <li>
            <b>Chave BYOK</b> — base legal: <i>consentimento</i> (art. 7º, I) —
            você decide colá-la. Pode removê-la a qualquer momento pelo modal
            "Conectar IA".
          </li>
          <li>
            <b>Cookies de Análise/Marketing</b> — quando existirem, base legal:
            <i>consentimento</i> coletado pelo banner.
          </li>
        </ul>
      </section>

      <section>
        <h2>4. Compartilhamento</h2>
        <ul>
          <li>
            <b>Supabase</b> — operador de autenticação (armazena
            <code>auth.users</code> e e-mail). Sujeito à política do Supabase.
          </li>
          <li>
            <b>Provedor de IA escolhido</b> (OpenAI, Google) — recebe o
            conteúdo dos seus prompts e a sua chave BYOK quando ativa.
          </li>
          <li>
            <b>Judge0</b> — sandbox que executa o código enviado nas
            submissões.
          </li>
        </ul>
        <p>Nenhum dado é vendido ou cedido para fins publicitários.</p>
      </section>

      <section>
        <h2>5. Armazenamento e retenção</h2>
        <ul>
          <li>
            Identidade e XP: enquanto sua conta estiver ativa. Solicitação de
            exclusão remove o usuário e o histórico vinculado.
          </li>
          <li>
            Sessão e chave BYOK: ficam apenas no seu navegador até você sair
            ou apagar manualmente. Logout encerra a sessão Supabase.
          </li>
          <li>
            Consentimento de cookies: até você revogar ou limpar o
            <code>localStorage</code>.
          </li>
        </ul>
      </section>

      <section>
        <h2>6. Seus direitos (LGPD art. 18)</h2>
        <p>Você pode, a qualquer tempo, solicitar:</p>
        <ul>
          <li>Confirmação da existência de tratamento;</li>
          <li>Acesso aos dados;</li>
          <li>Correção de dados incompletos, inexatos ou desatualizados;</li>
          <li>Anonimização, bloqueio ou eliminação de dados desnecessários;</li>
          <li>Portabilidade dos dados;</li>
          <li>
            Eliminação dos dados tratados com consentimento (incluindo
            exclusão da conta);
          </li>
          <li>Informação sobre compartilhamentos;</li>
          <li>Revogação do consentimento.</li>
        </ul>
        <p>
          Para exercer, abra uma <i>issue</i> no repositório do projeto
          identificando o pedido.
        </p>
      </section>

      <section>
        <h2>7. Cookies — gerenciar</h2>
        <p>
          Você pode reabrir o painel de preferências, revogar o consentimento
          atual ou aceitar tudo:
        </p>
        <div class="actions">
          <button type="button" class="btn-secondary" (click)="prefs.show()">
            Abrir preferências
          </button>
          <button type="button" class="btn-secondary" (click)="consent.reset()">
            Revogar consentimento
          </button>
        </div>
      </section>

      <section>
        <h2>8. Alterações</h2>
        <p>
          Esta política pode ser atualizada. Mudanças relevantes serão
          comunicadas pelo banner de cookies, que reaparecerá pedindo novo
          consentimento.
        </p>
      </section>
    </div>
    <app-cookie-preferences #prefs />
  `,
  styles: [
    `
      .page {
        max-width: 760px;
        margin: 0 auto;
        padding: 1.75rem 1.25rem 6rem;
        color: var(--text);
        line-height: 1.6;
      }
      .page__head {
        margin-bottom: 1.5rem;
      }
      .back {
        font-size: 0.85rem;
        color: var(--text-dim);
        text-decoration: none;
      }
      .back:hover {
        color: var(--accent);
      }
      h1 {
        margin: 0.5rem 0 0.25rem;
        font-size: 1.7rem;
      }
      h2 {
        margin: 1.8rem 0 0.6rem;
        font-size: 1.05rem;
        color: var(--accent);
      }
      p,
      ul {
        margin: 0.4rem 0;
        font-size: 0.92rem;
      }
      ul {
        padding-left: 1.4rem;
      }
      code {
        font-family: 'Cascadia Code', Consolas, monospace;
        background: var(--panel-2);
        padding: 0.1rem 0.35rem;
        border-radius: 4px;
        font-size: 0.85rem;
      }
      .updated {
        margin: 0.25rem 0 0;
        font-size: 0.8rem;
        color: var(--text-dim);
      }
      a {
        color: var(--accent);
      }
      .actions {
        display: flex;
        gap: 0.5rem;
        flex-wrap: wrap;
        margin-top: 0.5rem;
      }
      .btn-secondary {
        padding: 0.5rem 0.95rem;
        font-size: 0.85rem;
        color: var(--text);
        background: transparent;
        border: 1px solid var(--border);
        border-radius: 7px;
        cursor: pointer;
      }
      .btn-secondary:hover {
        border-color: var(--accent);
      }
    `,
  ],
})
export class PrivacyPolicyComponent {
  readonly consent = inject(ConsentService);

  @ViewChild('prefs') prefs!: CookiePreferencesComponent;
}
