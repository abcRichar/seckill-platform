"""生成秒杀项目本地演示图片（SVG，离线可用）。"""
import os

BASE = r"E:\seckill-platform\uploads\seed"
os.makedirs(BASE, exist_ok=True)

PRODUCTS = [
    ("phone", "星曜 X1 Pro", "📱", "#2563eb", "#7c3aed"),
    ("laptop", "极光 Air 14", "💻", "#0f766e", "#0ea5e9"),
    ("headphones", "声浪 Pro", "🎧", "#b91c1c", "#f97316"),
    ("watch", "时光 S3", "⌚", "#4338ca", "#db2777"),
    ("coffee", "醇香咖啡礼盒", "☕", "#92400e", "#d97706"),
    ("speaker", "声场 X 音箱", "🔊", "#1e3a8a", "#0891b2"),
    ("keyboard", "静音机械键盘", "⌨️", "#374151", "#6b7280"),
    ("backpack", "城市通勤双肩包", "🎒", "#065f46", "#10b981"),
]

AVATARS = [
    ("admin", "管", "#1d4ed8", "#3b82f6"),
    ("alice", "橙", "#c2410c", "#fb923c"),
    ("bob", "北", "#0f766e", "#2dd4bf"),
    ("carol", "卡", "#7e22ce", "#c084fc"),
    ("david", "杰", "#b91c1c", "#f87171"),
    ("emma", "木", "#166534", "#4ade80"),
]


def product_svg(emoji, name, c1, c2):
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="600" height="600" viewBox="0 0 600 600">
  <defs>
    <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="{c1}"/>
      <stop offset="100%" stop-color="{c2}"/>
    </linearGradient>
    <radialGradient id="r" cx="30%" cy="20%" r="70%">
      <stop offset="0%" stop-color="#ffffff" stop-opacity="0.32"/>
      <stop offset="100%" stop-color="#ffffff" stop-opacity="0"/>
    </radialGradient>
  </defs>
  <rect width="600" height="600" fill="url(#g)"/>
  <rect width="600" height="600" fill="url(#r)"/>
  <circle cx="500" cy="110" r="150" fill="#ffffff" opacity="0.08"/>
  <circle cx="80" cy="540" r="190" fill="#000000" opacity="0.10"/>
  <text x="300" y="310" font-size="190" text-anchor="middle" dominant-baseline="middle"
        font-family="Segoe UI Emoji, Apple Color Emoji, Microsoft YaHei">{emoji}</text>
  <text x="300" y="455" font-size="34" font-weight="700" text-anchor="middle"
        fill="#ffffff" font-family="Microsoft YaHei, Segoe UI">{name}</text>
  <text x="300" y="510" font-size="19" letter-spacing="7" text-anchor="middle"
        fill="#ffffff" opacity="0.72" font-family="Segoe UI">SECKILL SELECT</text>
</svg>
'''


def avatar_svg(char, c1, c2):
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200" viewBox="0 0 200 200">
  <defs>
    <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="{c1}"/>
      <stop offset="100%" stop-color="{c2}"/>
    </linearGradient>
  </defs>
  <circle cx="100" cy="100" r="100" fill="url(#g)"/>
  <circle cx="100" cy="100" r="82" fill="none" stroke="#ffffff" stroke-opacity="0.25" stroke-width="2"/>
  <text x="100" y="112" font-size="86" font-weight="700" text-anchor="middle"
        fill="#ffffff" font-family="Microsoft YaHei, Segoe UI">{char}</text>
</svg>
'''


BANNER = '''<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="700" viewBox="0 0 1200 700">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#0b0e14"/>
      <stop offset="55%" stop-color="#6b1713"/>
      <stop offset="100%" stop-color="#d94a1f"/>
    </linearGradient>
    <radialGradient id="glow" cx="50%" cy="50%" r="50%">
      <stop offset="0%" stop-color="#fbbf24" stop-opacity="0.6"/>
      <stop offset="100%" stop-color="#fbbf24" stop-opacity="0"/>
    </radialGradient>
  </defs>
  <rect width="1200" height="700" fill="url(#bg)"/>
  <circle cx="930" cy="330" r="300" fill="url(#glow)"/>
  <circle cx="930" cy="330" r="190" fill="none" stroke="#fde68a" stroke-opacity="0.3" stroke-width="2"/>
  <circle cx="930" cy="330" r="250" fill="none" stroke="#fde68a" stroke-opacity="0.14" stroke-width="2"/>
  <text x="80" y="250" font-size="68" font-weight="800" fill="#ffffff"
        font-family="Microsoft YaHei, Segoe UI">限时秒杀 · 好物直降</text>
  <text x="84" y="325" font-size="27" fill="#fde68a" letter-spacing="2"
        font-family="Microsoft YaHei, Segoe UI">每日 10:00 准时开抢 · 限量库存 · 先到先得</text>
  <rect x="84" y="380" width="228" height="58" rx="29" fill="#fbbf24"/>
  <text x="198" y="418" font-size="24" font-weight="700" text-anchor="middle"
        fill="#7c2d12" font-family="Microsoft YaHei, Segoe UI">立即抢购 →</text>
  <text x="84" y="520" font-size="15" fill="#ffffff" opacity="0.5" letter-spacing="4"
        font-family="Segoe UI">SECKILL MARKET · 2026</text>
  <text x="930" y="400" font-size="170" text-anchor="middle" font-family="Segoe UI Emoji">⚡</text>
  <text x="930" y="585" font-size="18" letter-spacing="10" text-anchor="middle"
        fill="#ffffff" opacity="0.55" font-family="Segoe UI">FLASH SALE</text>
</svg>
'''

for slug, name, emoji, c1, c2 in PRODUCTS:
    with open(os.path.join(BASE, f"{slug}.svg"), "w", encoding="utf-8") as f:
        f.write(product_svg(emoji, name, c1, c2))

for slug, char, c1, c2 in AVATARS:
    with open(os.path.join(BASE, f"avatar-{slug}.svg"), "w", encoding="utf-8") as f:
        f.write(avatar_svg(char, c1, c2))

with open(os.path.join(BASE, "banner.svg"), "w", encoding="utf-8") as f:
    f.write(BANNER)

print("generated:", len(os.listdir(BASE)), "svg files in", BASE)
