# Modelos do IsometricChess: normais/winding invertidos

## O problema

Os 12 `.obj` de peças de xadrez (`resources/models/{pawn,rook,knight,bishop,queen,king}_{white,black}.obj`)
renderizam errado com as configurações padrão do motor:

- Com face culling ligado (`cullFace(true)`, o padrão), a peça aparece "de dentro para fora" — a
  superfície externa correta é descartada e só a interna sobra visível.
- Mesmo com culling desligado (então a geometria fica certa, porque o depth test escolhe a
  superfície mais próxima independente de winding), a iluminação sai invertida — a maior parte da
  peça fica escura (só luz ambiente) e só pequenos trechos pegam luz direta.

Os modelos `earth.obj`/`clouds.obj` (usados no `ModelViewer`) **não** têm nenhum desses problemas:
renderizam certo com culling ligado e iluminação normal, sem nenhum ajuste especial.

## Diagnóstico

Medi as normais de `pawn_white.obj` contra o centroide da malha (`vértice - centroide` vs a normal
daquele vértice): **~75% delas apontam "para dentro"** (produto escalar negativo), quando o
esperado para uma malha exportada corretamente é a maioria apontando para fora. Esse teste não é
perfeito para formas côncavas (um peão tem um "pescoço" fino onde até normais corretas podem dar
negativo contra o centroide global), mas o desvio de 3:1 é grande demais pra ser só ruído da
heurística.

Além disso, o problema de **culling** (winding invertido) e o de **iluminação** (normais
invertidas) apareceram **juntos** e de forma consistente nesses 12 arquivos. Isso é exatamente a
assinatura de um "Flip Normals" aplicado na malha em algum editor 3D: essa operação inverte a
ordem dos vértices de cada face (o que inverte o winding) **e** recalcula/exporta a normal
invertida junto — as duas coisas saem erradas ao mesmo tempo pela mesma causa.

## Minha opinião: é o export dos modelos de xadrez, não o motor

O motor assume a convenção padrão do OpenGL (que também é a mais comum em ferramentas 3D,
Blender incluso, quando as normais estão "certas"): **face frontal = sentido anti-horário (CCW)
visto de fora, normal apontando para fora, e os dois concordam entre si**. Isso é exatamente o que
`earth.obj`/`clouds.obj` seguem, sem nenhum tratamento especial no código. As peças de xadrez é que
destoam desse padrão — e destoam das duas formas ao mesmo tempo, o que aponta para um "Flip
Normals" acidental (ou um exportador/fonte diferente) especificamente nesses arquivos, não para um
bug geral de convenção no motor.

Ou seja: pelo que os dados mostram, não acho que foi o ModelViewer que exportou "errado" e o xadrez
"certo" — é o contrário. Se o motor estivesse com a convenção trocada, o `earth`/`clouds` que
teriam dado o mesmo tipo de problema, e não deram.

## O que existe hoje (gambiarra no motor, funcional mas não ideal)

Para as peças renderizarem certo sem re-exportar nada, o `IsometricChess` hoje:

- desliga face culling só na pass 3D dele (`cullFace(false)` em
  `rengine-demos/.../chess/ChessRenderer.java`) — renderiza os dois lados de cada triângulo e
  deixa o depth test escolher a superfície visível certa;
- chama `models.load(obj, textura, true)` — o último parâmetro `flipNormals` inverte as normais
  lidas do arquivo só para esses modelos (`rengine-toolkit3d/.../toolkit3d/Models.java`).

Isso funciona, mas tem dois custos:

1. **Performance**: sem culling, a GPU rasteriza o dobro de triângulos por peça (frente e verso).
   Pouco importa com 16 peças numa tela só, mas não é a forma "certa" de resolver.
2. **Dívida técnica**: todo modelo novo que vier dessa mesma fonte (ou com o mesmo problema de
   export) vai precisar do mesmo `flipNormals = true` manual, e quem for mexer no código precisa
   lembrar que essa flag existe e por quê.

## Como consertar na origem (recomendado)

Se você tiver os arquivos fonte (`.blend` ou equivalente) dos modelos de xadrez:

1. Abra o modelo no Blender.
2. Entre em Edit Mode (`Tab`), selecione tudo (`A`).
3. `Mesh > Normals > Recalculate Outside` (atalho `Shift+N`). Isso resolve o caso comum (normais
   viradas por engano, winding consistente internamente).
   - Se depois disso ainda renderizar "de dentro pra fora" no motor (ou seja, se o problema for o
     winding da malha em si, não só as normais), use `Mesh > Normals > Flip` como passo adicional.
4. Reexporte como `.obj` **com as mesmas opções** usadas para `earth.obj`/`clouds.obj`
   (principalmente os eixos "Forward"/"Up" — se inverter esses em relação ao que já funciona, pode
   trocar o problema de winding por outro). Se não souber quais foram, abra `earth.obj` e
   `pawn_white.obj` num editor de texto e compare o cabeçalho/primeiras linhas de comentário (o
   Blender costuma anotar a versão/config do exportador ali) para ver se saíram de exportações
   diferentes.
5. Depois de reexportar, reverta as duas gambiarras no código:
   - `ChessRenderer.java`: volte `cullFace(false)` para `cullFace(true)` na `scene3DDescriptor`.
   - `ChessScene.java` (`createPieceView`): troque `models.load(obj, textura, true)` pela versão de
     2 argumentos (sem `flipNormals`).
6. Rode `IsometricChess` de novo e confirme visualmente que continua certo com culling ligado.

## Se não for possível reexportar

Pode manter como está — o `flipNormals` + `cullFace(false)` continuam funcionando indefinidamente,
só carregam o custo de performance/dívida técnica acima. Nesse caso, considere documentar no
próprio `Models` (já tem um comentário no `ChessRenderer.java` explicando) para quem mexer no
código no futuro não precisar redescobrir isso.
