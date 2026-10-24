from PIL import Image, ImageDraw, ImageFont

CODE128_STANDARD = {
    0: '212222', 1: '222122', 2: '222221', 3: '121223', 4: '121322',
    5: '131222', 6: '122213', 7: '122312', 8: '132212', 9: '221213',
    10: '221312', 11: '231212', 12: '112232', 13: '122132', 14: '122231',
    15: '113222', 16: '123122', 17: '123221', 18: '223211', 19: '221132',
    20: '221231', 21: '213212', 22: '223112', 23: '312131', 24: '311222',
    25: '321122', 26: '321221', 27: '312212', 28: '322112', 29: '322211',
    30: '212123', 31: '212321', 32: '232121', 33: '111323', 34: '131123',
    35: '131321', 36: '112313', 37: '132113', 38: '132311', 39: '211313',
    40: '231113', 41: '231311', 42: '112133', 43: '112331', 44: '132131',
    45: '113123', 46: '113321', 47: '133121', 48: '313121', 49: '211331',
    50: '231131', 51: '213113', 52: '213311', 53: '213131', 54: '311123',
    55: '311321', 56: '331121', 57: '312113', 58: '312311', 59: '332111',
    60: '314111', 61: '221411', 62: '431111', 63: '111224', 64: '111422',
    65: '121124', 66: '121421', 67: '141122', 68: '141221', 69: '112214',
    70: '112412', 71: '122114', 72: '122411', 73: '142112', 74: '142211',
    75: '241211', 76: '221114', 77: '413111', 78: '241112', 79: '134111',
    80: '111242', 81: '121142', 82: '121241', 83: '114212', 84: '124112',
    85: '124211', 86: '411212', 87: '421112', 88: '421211', 89: '212141',
    90: '214121', 91: '412121', 92: '111424', 93: '141124', 94: '141421',
    95: '112414', 96: '142114', 97: '142411', 98: '411412', 99: '411214',
    100: '421114', 101: '412114', 102: '214141', 103: '132132',
    104: '211214', 105: '211232', 106: '2331112'
}

CODE128 = [CODE128_STANDARD[i] for i in range(107)]

START_B = 104
STOP = 106

print(f"Total patterns: {len(CODE128)}")
print(f"START_B ({START_B}): {CODE128[START_B]}")
print(f"STOP ({STOP}): {CODE128[STOP]}")

if CODE128[START_B] != '211214':
    print(f"ERROR: START_B should be '211214', got '{CODE128[START_B]}'")
    exit(1)

if CODE128[STOP] != '2331112':
    print(f"ERROR: STOP should be '2331112', got '{CODE128[STOP]}'")
    exit(1)

print("CODE128 table is valid!")

def encode(text):
    codes = [START_B]
    for c in text:
        codes.append(ord(c) - 32)
    checksum = codes[0]
    for i in range(1, len(codes)):
        checksum += codes[i] * i
    codes.append(checksum % 103)
    codes.append(STOP)
    return codes

def draw_barcode(text, filename):
    unit_width = 6
    bar_height = 150
    quiet_zone = 60
    
    total_width = quiet_zone * 2
    codes = encode(text)
    for code in codes:
        pattern = CODE128[code]
        for digit in pattern:
            total_width += int(digit) * unit_width
    
    height = bar_height + 60
    img = Image.new('RGB', (total_width, height), 'white')
    draw = ImageDraw.Draw(img)
    
    x = quiet_zone
    for code in codes:
        pattern = CODE128[code]
        for i in range(len(pattern)):
            w = int(pattern[i]) * unit_width
            if i % 2 == 0:
                draw.rectangle([x, 10, x + w, 10 + bar_height], fill='black')
            x += w
    
    try:
        font = ImageFont.truetype('arial.ttf', 32)
    except:
        font = ImageFont.load_default()
    
    label = text
    lw = draw.textlength(label, font=font)
    draw.text(((total_width - lw) // 2, bar_height + 20), label, fill='black', font=font)
    
    img.save(filename, 'PNG')
    print(f'Generated: {filename} (width={total_width}, height={height})')

nums = ['20260715001', '20260715002', '20260715003', '20260715004', '20260715005']
for n in nums:
    draw_barcode(n, f'barcode_{n}.png')

print('\nAll done!')
