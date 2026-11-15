# Uso, a partir de Backend-Muttley: python scripts/gerar_guilhoche.py src/main/resources/certificado/guilhoche.svg
import math, sys
# Guilhochê do certificado em A4 paisagem (297 x 210 mm, 10 unidades por mm).
# Traço preto opaco sobre transparente: o PDF usa como máscara e pinta na cor do modelo.
# Cada curva é desenhada uma vez em <defs> e repetida com <use>, girada ou deslocada.
W, H = 2970, 2100
CX, CY = W / 2, H / 2

def caminho(pontos):
    x, y = pontos[0]
    return f'M{x:.0f} {y:.0f}' + ''.join(f'L{x:.0f} {y:.0f}' for x, y in pontos[1:])

def hipotrocoide(R, r, d, passos=720):
    pts = []
    for i in range(passos + 1):
        t = 2 * math.pi * i / passos
        pts.append(((R - r) * math.cos(t) + d * math.cos((R - r) / r * t),
                    (R - r) * math.sin(t) - d * math.sin((R - r) / r * t)))
    return caminho(pts)

# Onda longa, maior que a página: as cópias deslocadas fazem o entrelaçado.
def onda(amp, comprimento=520, x0=-600, x1=W + 600, passos=900):
    pts = []
    for i in range(passos + 1):
        x = x0 + (x1 - x0) * i / passos
        pts.append((x, amp * math.sin(2 * math.pi * x / comprimento)))
    return caminho(pts)

defs = [
    f'<path id="a" d="{hipotrocoide(300, 37.5, 205)}"/>',   # 8 pétalas
    f'<path id="b" d="{hipotrocoide(300, 50, 140)}"/>',     # 6 pétalas, mais miúda
    f'<path id="o" d="{onda(34)}"/>',
    f'<clipPath id="f"><rect x="160" y="0" width="{W - 320}" height="{H}"/></clipPath>',
]

usos = []
for i in range(16):  # roseta: as duas curvas giradas formam a renda
    usos.append(f'<use href="#a" transform="translate({CX:.0f} {CY:.0f}) scale(0.82) rotate({i * 360 / 16 / 8:.2f})"/>')
for i in range(12):
    usos.append(f'<use href="#b" transform="translate({CX:.0f} {CY:.0f}) scale(0.82) rotate({i * 360 / 12 / 6:.2f})"/>')
usos.append(f'<circle cx="{CX:.0f}" cy="{CY:.0f}" r="430"/><circle cx="{CX:.0f}" cy="{CY:.0f}" r="442"/>')

faixas = []
for y0, sinal in ((205, 1), (H - 205, -1)):
    for i in range(10):  # cada cópia deslocada na horizontal e um pouco na vertical
        faixas.append(f'<use href="#o" transform="translate({i * 26} {y0 + sinal * i * 4}) scale(1 {sinal})"/>')
        faixas.append(f'<use href="#o" transform="translate({260 + i * 26} {y0 + sinal * i * 4}) scale(1 {-sinal})"/>')

svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}">'
       f'<defs>{"".join(defs)}</defs>'
       f'<g fill="none" stroke="#000" stroke-width="2" stroke-linejoin="round">'
       + ''.join(usos) + f'<g clip-path="url(#f)">{"".join(faixas)}</g></g></svg>')
open(sys.argv[1], 'w').write(svg)
print(len(svg) // 1024, 'KB')
