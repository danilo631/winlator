# Winlator Enhanced 11.2 — implementação experimental

Uma extensão reproduzível para [Winlator](https://github.com/brunodev85/winlator), inspirada em fluxos simples de GameHub e na filosofia de perfis por aplicativo do FEX/Proton. **Não incorpora código proprietário do GameHub.**

## O que está implementado

O fork mantém o submódulo `app` na revisão oficial 11.2. O script `scripts/apply_app_overrides.py` insere os fontes adicionais no checkout durante o build, sem alterar o repositório upstream. A validação falha se a estrutura do Winlator mudar.

- **Perfis rápidos dentro do app:** um botão na aba **Variáveis de ambiente** de contêineres e atalhos de jogos oferece Balanceado, Desempenho experimental, Compatibilidade, Temperatura conservadora, Mali, Diagnóstico DXVK e Ocultar HUD. O usuário vê a descrição de cada perfil antes de salvar.
- **Controle por jogo:** o diálogo de atalhos já salva variáveis e a escolha de Box64 individualmente. O novo botão reutiliza esse mecanismo existente. Perfis não instalados não são prometidos.
- **Mali e GPUs não Adreno:** novos contêineres começam sem `TU_DEBUG` e `ZINK_DEBUG` nos overrides genéricos. O Winlator 11.2 já integra **Vortek** para Vulkan e **Gladio/VirGL** para OpenGL, conforme disponibilidade real no aparelho.
- **GPU/EGL:** o reconhecimento OpenGL ES passa a usar um pbuffer de 1×1 e espera limitada a dois segundos; desaloca EGLSurface, EGLContext e EGLDisplay.
- **Diagnósticos:** ativa e remove `DXVK_HUD=fps,frametimes` por jogo, sem misturar resultados de DXVK com outros caminhos de renderização.
- **Automação:** testes Java para regras dos perfis, patcher idempotente e workflow para compilar APK debug no GitHub Actions.

### Descrição dos perfis

| Perfil | Box64 | Variáveis aplicadas |
| --- | --- | --- |
| Balanceado | INTERMEDIATE | shader cache habilitado; limite 256 MB; Esync ligado |
| Desempenho | PERFORMANCE | cache 512 MB; Esync e `mesa_glthread` ligados |
| Compatibilidade | STABILITY | cache 256 MB; Esync e `mesa_glthread` desligados |
| Menor aquecimento | CONSERVATIVE | cache 128 MB; `mesa_glthread` desligado |
| Mali seguro | CONSERVATIVE | remove overrides exclusivos Turnip/Zink; cache 256 MB |
| Diagnóstico DXVK | não altera | exibe FPS e tempo de quadro no DXVK |
| Ocultar HUD | não altera | remove apenas `DXVK_HUD` |

Esses perfis são **pontos de partida para testes**, não garantia de ganho de FPS ou redução de consumo. A maior parte das mudanças modifica somente variáveis que o usuário escolheu ajustar. Os demais overrides são preservados.

## Build com GitHub Actions

1. Abra a branch `feature/winlator-enhanced-profiles-11` e o PR correspondente.
2. Acesse **Actions → Winlator Enhanced / Android**.
3. Use **Run workflow**, selecionando a branch de melhorias. O workflow executa testes e tenta compilar `app:assembleDebug`.
4. Se o build for concluído, baixe o artefato `Winlator-Enhanced-11-debug`. Instale apenas depois de salvar seus dados.

Build local (Ubuntu com Android SDK/NDK/CMake e JDK 17):

```sh
git clone --recurse-submodules https://github.com/danilo631/winlator.git
cd winlator
git checkout feature/winlator-enhanced-profiles-11
git submodule update --init --recursive
python3 scripts/apply_app_overrides.py --check
python3 scripts/apply_app_overrides.py
(cd app && ./gradlew :app:assembleDebug)
```

**Atenção ao Android Package ID:** o debug pode usar `com.winlator` e conflitar com a instalação original. Não instale sobre o app que contém seus jogos sem verificar a assinatura e criar backup. Para coexistência será necessário alterar `applicationId` de forma consistente e avaliar os caminhos fixos de armazenamento do aplicativo.

## FEX: estudo de integração (não instalado)

[FEX](https://github.com/FEX-Emu/FEX) tem JIT x86/x64, memória e cache de código e encaminhamento de bibliotecas do host. Porém, seu aplicativo completo é projetado para **Linux sobre Linux**, não Android. O **FEXCore** pode, em teoria, ser compilado para AArch64 Android, mas instalar um executável FEX ou um rootfs x86 não o torna automaticamente compatível com o Android e com a arquitetura de lançamento do Winlator. A integração correta precisaria de:

- Componentes FEXCore compatíveis com Android, com política de licença e checksum de binários;
- Adaptação do launchpad, syscalls, rootfs, host libraries, caminhos e WoW64;
- Feature flag para **Box64** continuar padrão e rollback caso FEX falhe;
- Benchmarks determinísticos (mesmo jogo, mesmo driver, termal e frame times).

Nenhum binário FEX ou rootfs é distribuído neste patch.

## Proton: estudo de integração (não instalado)

[Proton](https://github.com/ValveSoftware/Proton) não é somente `wine64`: integra Wine, DXVK, VKD3D-Proton e scripts específicos para Steam/Linux. O Winlator já oferece partes desse conjunto. O objetivo prático é ampliar opções via componentes empacotados e versionados; executar Proton diretamente requer adaptar sua lógica de prefixo, entrypoint, dependências do Steam Runtime e ambiente de tradução. Observe requisitos Vulkan do [VKD3D-Proton](https://github.com/HansKristian-Work/vkd3d-proton), que variam conforme GPU/driver. **Não há suporte a Proton nativo/Steam neste PR**.

## Limitações e validação

- Fonte oficial **11.2**; o build APK e testes em aparelhos Android são necessários antes de marcar o PR pronto.
- Não há aumento de FPS medido nem testes em GPU Mali neste estágio.
- `Vortek + Gladio` é uma opção já existente e não um driver novo desenvolvido aqui.
- No Mali, tente Vortek com aplicativos Vulkan compatíveis; teste VirGL ou Gladio e WineD3D nos jogos que apresentarem problemas. Vulkan/DirectX 12 podem falhar por recursos de hardware.
- Recursos de Steam, streaming, autenticação ou distribuição de jogos **não foram copiados do GameHub**.
- Perfis agressivos do Box64 podem gerar crashes; faça backup e volte ao perfil de estabilidade.

## Créditos e fontes

- [Winlator + código do app](https://github.com/brunodev85/winlator)
- [GameHub Lite — estudo de funcionalidades (não incorporado)](https://github.com/gamehublite/gamehub-oss)
- [FEX](https://github.com/FEX-Emu/FEX) e [FAQ oficial](https://wiki.fex-emu.com/index.php/FAQ)
- [Proton](https://github.com/ValveSoftware/Proton)
- [DXVK](https://github.com/doitsujin/dxvk)
- [VKD3D-Proton](https://github.com/HansKristian-Work/vkd3d-proton)
- [Mesa3D](https://www.mesa3d.org/)

Respeite as licenças de cada dependência e redistribua apenas os componentes compatíveis com seus termos.
