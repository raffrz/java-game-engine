# Changelog

## [Não lançado]

### Adicionado
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
