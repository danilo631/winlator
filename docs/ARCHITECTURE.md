# Arquitetura do Winlator Enhanced

O objetivo é evoluir o Winlator 11.2 sem divergir da base upstream e sem alegar suporte a runtimes que ainda não foram validados.

## Árvore de código e responsabilidades

- `app/`, `gladio/` e `vortek/`: **submódulos Git**, apontando para versões específicas. Não edite o conteúdo upstream diretamente em commits da árvore principal.
- `overrides/app/src/main/java/com/winlator/optimization/`: Java da interface, perfis, verificações de runtime e interação com o Wine.
- `overrides/app/src/main/res/`: telas e elementos visuais Enhanced, incluindo o menu `Wine & Proton`.
- `scripts/apply_app_overrides.py`: valida âncoras e aplica patches ao submódulo. Requer idempotência: a segunda aplicação não pode alterar nada nem falhar. Layouts existentes usam hashes Git-blob upstream para recusar alterações inesperadas.
- `scripts/assemble_fex_proton.py`, `scripts/package_proton_wine.py`, `scripts/package_fexcore_dlls.py`: scripts de preparação e validação de pacotes. Não substituem um instalador de dependências ABI completas.
- `tests/`: testes de regras puras Java e scripts Python. Os testes não executam games Windows Android.
- `.github/workflows/`: testes, build APK Android e compilação experimental dos binários FEXCore/Proton.

## Fluxo de build

```text
GitHub checkout --recurse-submodules
        ↓
scripts/apply_app_overrides.py --check
        ↓
apply overlays (2x, idempotent)
        ↓
regressões Java/Python
        ↓
Gradle :app:assembleDebug
        ↓
Actions artifact Winlator-Enhanced-11-debug
```

Na UI, `MainActivity` abre `RuntimeCenterFragment` pelo menu lateral; o gerenciador mostra Wine instalado via `WineInstaller.getInstalledWineInfos`, oferece downloads oficiais e abre `SettingsFragment` com argumentos de navegação para **Importar Wine** ou **Gerenciar Wine**. A seção `LLWineInstallation` agora é visível sem depender da flag interna `DEBUG_MODE`.

A seleção de Wine é feita por contêiner e o backend CPU é armazenado nas variáveis de ambiente, por exemplo `WINLATOR_CPU_BACKEND=fexcore`. O Box64 permanece como padrão e o FEXCore precisa passar por verificações de ABI do Wine ARM64 e DLLs PE. O Wine ARM64EC não representa Vulkan/GPU Mali; são camadas distintas.

## Branches e PRs em camadas

- PR #2: `feature/winlator-enhanced-profiles-11`, sobre `main`;
- PR #3: `feature/winlator-frame-pacing-cache-11`, sobre a fase anterior;
- PR #4: `feature/fex-proton-runtime-integration-11`, sobre a fase 2;
- PR #5: `feature/launcher-proton-experience-11`, sobre a fase 3;
- Esta fase: `feature/runtime-center-navigation-11`, sobre a fase 4.

Os recursos só são incorporados a `main` se os PRs forem deliberadamente revisados e mesclados. **Nunca force merge automaticamente** quando existir risco de compatibilidade/instalação.

## Regras de contribuição

Conserve o caminho Box64 funcional, não armazene credenciais/tokens no repositório, não copie GameHub fechado, mantenha imports e IDs de views compatíveis, escreva testes, documente variantes de ABI e evite alegações de FPS sem benchmark.
