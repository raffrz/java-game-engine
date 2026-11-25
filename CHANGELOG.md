# Changelog

## [Não lançado]

### Adicionado
- `RenderPassDescriptor.depthCompare(DepthCompare)` e `depthWrite(boolean)`: a comparação do teste de profundidade do
  passe (`LESS`, o padrão, `LESS_OR_EQUAL`, `GREATER`, `GREATER_OR_EQUAL`, `ALWAYS`) e se ele grava a profundidade
  (padrão: grava). Com `GREATER` e sem escrita, um passe desenha só o que os anteriores escondem (a silhueta de um
  personagem atrás de uma parede; desligue também o `clearDepthBuffer`). Aplicados no `OpenGLBackend` com `glDepthFunc` e
  `glDepthMask`; o clear de profundidade segue só o `clearDepthBuffer`, independente da escrita.
- Animação esquelética 3D: módulo `animation` (`Skeleton`, `Pose`, `Track`, `AnimationClip`, `Poses`,
  `Animator` com crossfade; IK `TwoBoneIk`, `FabrikIk`, `LookAtIk`), skinning na GPU (`MeshData`/`Mesh` com
  juntas e pesos, `RenderCommand.jointPalette`), `SkinnedModel`, `SkinnedMaterials` e loader glTF 2.0
  (`Models.loadGltf` → `ModelAsset`; JSON lido pelo Gson 2.15.0, nova dependência do `toolkit3d`). Demo
  `animation.AnimationDemo`.
- `Transform`: posição, rotação e escala com a matriz de modelo T·R·S, comum a `TexturedModel` e
  `SkinnedModel` (`transform()`).
- `Assets.texture(key, bytes)` (imagem em memória), `Assets.solidColor(r, g, b, a)` (textura 1×1 cacheada
  pela cor) e `Assets.normalize(path)`.
- `Input.getScrollY()`: rolagem vertical da roda do mouse no frame, implementada no `GlfwInput`.
- `IsometricCamera.rotate(degrees)` e `yaw()`: a câmera orbita em volta do eixo Y do alvo;
  `pan`, `viewMatrix` e `directionToEye` acompanham o yaw.
- `LitMaterial.viewDirection(...)`: atualiza só a direção da câmera do brilho especular, para
  câmeras que se movem.
- `LitMaterial.tint(color, strength)`: mistura uma cor sobre a superfície (destaque de hover ou
  seleção), com os uniforms `tintColor` e `tintStrength` no shader padrão.
- `LitMaterial.emission(map, strength)`: brilho próprio, a cor do mapa de emissão (mesma UV da textura
  de cor) somada depois da luz, sem receber luz, sombra nem tint; uniforms `emissionMap`,
  `useEmission` e `emissionStrength` no shader padrão. Desligado por padrão.

### Demos
- `IsometricChess`: contorno da peça sob o cursor, zoom pela roda e rotação arrastando com o botão
  do meio; peças em `examples.chess.model.pieces` (uma subclasse por tipo) com os movimentos válidos de
  cada uma, e o caminho até o destino sob o cursor destacado com tint.
- `IsometricChess`: clicar num destino válido move a peça; a peça capturada é levada para fora do
  tabuleiro, do lado de quem capturou, com animação (sobe, atravessa, desce). Loop em três
  passos: input (`ChessControls`), lógica (`ChessLogic`, que também anima as peças no
  `ChessState`) e render (`ChessRenderer`, a partir da `ChessScene`, que só guarda os modelos).
- `IsometricChess`: turnos (brancas começam, só a peça da vez é selecionável); HUD mostra de quem
  é a vez e os controles, em fonte menor. A peça desliza até a casa nova. Shadow map e câmera
  configurados na `ChessSceneFactory`.

### Corrigido
- `GlfwWindow.destroy()` libera os callbacks registrados na janela antes de destruí-la.
