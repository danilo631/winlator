# Scripts e comandos

Os scripts permanecem em `scripts/` porque os workflows GitHub Actions e os testes dependem desses caminhos. Não reorganize fisicamente scripts sem atualizar todas as referências e executar CI.

| Script | Função |
|---|---|
| `apply_app_overrides.py` | Aplica verificações/overlays da UI e runtime ao Winlator 11.2 fixado |
| `package_proton_wine.py` | Gera um Wine `.tar.xz` de uma árvore Wine pré-compilada |
| `package_fexcore_dlls.py` | Valida/empacota DLLs PE FEXCore compiladas |
| `assemble_fex_proton.py` | Combina Proton Wine Android ARM64EC e módulos FEXCore com verificações de arquivos |

## Exemplo — aplicar overlays

```bash
git submodule update --init --recursive
python3 scripts/apply_app_overrides.py --check
python3 scripts/apply_app_overrides.py
python3 scripts/apply_app_overrides.py  # idempotência obrigatória
```

## Exemplo — empacotar Proton/FEXCore

```bash
python3 scripts/assemble_fex_proton.py \
  --wine proton-wine-11.0-2-arm64ec.wcp.xz \
  --fex FEXCore-2609-ARM64EC-modules.tar.xz \
  --output proton-fex-winlator.tar.xz
```

Para fazer tudo no GitHub, abra o workflow **Bundle ARM64EC Proton Wine + FEXCore**. A montagem valida formato do ELF/PE/TAR, mas **não garante a execução do Wine no Android**. Confira [Guia Wine](../docs/WINE_PROTON_GUIDE.md).
