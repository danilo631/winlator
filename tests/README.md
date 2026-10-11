# Testes e validação

- **Java puro**: perfis de CPU, cache, tracker de atualização, pré-verificação ELF/PE do FEXCore, filtros da biblioteca e presets Proton ESYNC/NTsync.
- **Python**: estrutura dos arquivos compactados, proteção contra caminhos maliciosos, criação do pacote Proton/FEX e integridade dos módulos.

Execute testes Python:

```bash
python3 -m unittest discover -s tests -p 'test_*.py' -v
```

O GitHub Actions contém o comando Java 17 de compilação/regressão em `.github/workflows/enhanced-android.yml`. A CI também aplica overlays duas vezes e constrói o APK.

**Nota:** aprovação desses testes não significa compatibilidade comprovada em hardware Android, GPUs Mali, DXVK/VKD3D ou execução de jogos reais.
