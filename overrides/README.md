# Overrides do Winlator Enhanced

Os arquivos em `overrides/app/src/main` são a fonte das modificações do fork, **não** cópias editadas diretamente dentro do submódulo Winlator upstream.

- `java/com/winlator/optimization/`: gerenciamento Wine/Proton, FEXCore, perfis, frametimes e nova biblioteca.
- `java/com/winlator/core/`: detecção GPU e extensões da plataforma.
- `res/layout/`, `res/menu/`, `res/drawable/`: layouts, menu lateral, botões e cores da interface nova.

Para aplicar execute `python3 scripts/apply_app_overrides.py` a partir da raiz. Esse patcher é intencionalmente estrito: falha se o upstream mudou, em vez de aplicar um patch em um ponto incorreto. Veja [Arquitetura](../docs/ARCHITECTURE.md).
