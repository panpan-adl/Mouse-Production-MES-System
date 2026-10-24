const CODE128_STANDARD = {
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

const CODE128_PATTERNS = [];
for (let i = 0; i < 107; i++) {
    CODE128_PATTERNS.push(CODE128_STANDARD[i]);
}

const START_B = 104;
const STOP = 106;

function charToCode128B(char) {
    const code = char.charCodeAt(0);
    if (code >= 32 && code <= 126) {
        return code - 32;
    }
    return -1;
}

function calculateChecksum(codes) {
    let checksum = codes[0];
    for (let i = 1; i < codes.length; i++) {
        checksum += codes[i] * i;
    }
    return checksum % 103;
}

function encode(text) {
    const codes = [START_B];
    for (let i = 0; i < text.length; i++) {
        codes.push(charToCode128B(text[i]));
    }
    const checksum = calculateChecksum(codes);
    codes.push(checksum);
    codes.push(STOP);
    return codes;
}

function drawBarcode(canvasCtx, text, options) {
    const opts = options || {};
    const unitWidth = opts.width || 6;
    const barHeight = opts.height || 150;
    const startX = opts.x || 60;
    const startY = opts.y || 40;
    
    const canvasWidth = opts.canvasWidth || 600;
    const canvasHeight = opts.canvasHeight || 240;
    
    canvasCtx.setFillStyle('#ffffff');
    canvasCtx.fillRect(0, 0, canvasWidth, canvasHeight);
    canvasCtx.setFillStyle('#000000');
    
    const codes = encode(text);
    
    let currentX = startX;
    codes.forEach(code => {
        const pattern = CODE128_PATTERNS[code];
        if (!pattern) return;
        for (let i = 0; i < pattern.length; i++) {
            const width = parseInt(pattern[i]) * unitWidth;
            if (i % 2 === 0) {
                canvasCtx.fillRect(currentX, startY, width, barHeight);
            }
            currentX += width;
        }
    });
    
    canvasCtx.setFillStyle('#333333');
    canvasCtx.setFontSize(28);
    canvasCtx.setTextAlign('center');
    canvasCtx.fillText(text, (startX + currentX) / 2, startY + barHeight + 36);
}

function generateOrderNo() {
    const now = new Date();
    const year = now.getFullYear().toString();
    const month = (now.getMonth() + 1).toString().padStart(2, '0');
    const day = now.getDate().toString().padStart(2, '0');
    const random = Math.floor(Math.random() * 1000).toString().padStart(3, '0');
    return year + month + day + random;
}

module.exports = {
    drawBarcode,
    generateOrderNo,
    encode
};
