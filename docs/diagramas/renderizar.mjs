import { spawnSync } from 'node:child_process';
import { existsSync, mkdtempSync, writeFileSync, unlinkSync, rmdirSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const pasta = dirname(fileURLToPath(import.meta.url));
const cli = join(pasta, 'node_modules/@mermaid-js/mermaid-cli/src/cli.js');
const catalogo = ['Muttley-Use-Case', 'Muttley-Class-Diagram', 'erd', 'visao-arquitetural',
  'documento/visao-arquitetural-simplificada'];
const solicitados = process.argv.slice(2);
const diagramas = solicitados.length ? solicitados : catalogo;
if (diagramas.some(nome => !catalogo.includes(nome))) {
  throw new Error('Diagrama desconhecido. Use um nome do catálogo em renderizar.mjs.');
}
if (!existsSync(cli)) throw new Error('Instale as dependências nesta pasta com npm ci.');

// Permite aproveitar um Chrome/Chromium local sem fixar caminhos de uma máquina.
const temporaria = mkdtempSync(join(tmpdir(), 'muttley-diagramas-'));
const config = join(temporaria, 'puppeteer.json');
try {
  const opcoes = [];
  if (process.env.PUPPETEER_EXECUTABLE_PATH) {
    writeFileSync(config, JSON.stringify({ executablePath: process.env.PUPPETEER_EXECUTABLE_PATH }));
    opcoes.push('-p', config);
  }
  for (const nome of diagramas) {
    const resultado = spawnSync(process.execPath, [cli,
      '-i', join(pasta, `${nome}.mmd`), '-o', join(pasta, `${nome}.png`),
      '-c', join(pasta, 'mermaid-config.json'), '-b', 'white',
      '-w', '2400', '-H', '1800', '-s', '2', ...opcoes
    ], { stdio: 'inherit', windowsHide: true });
    if (resultado.error) throw resultado.error;
    if (resultado.status !== 0) throw new Error(`Falha ao renderizar ${nome}.`);
  }
} finally {
  if (existsSync(config)) unlinkSync(config);
  rmdirSync(temporaria);
}
